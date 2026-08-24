package com.nordairemapper.service

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import com.nordairemapper.service.shizuku.IGrantService
import com.nordairemapper.service.shizuku.ShizukuGrantService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Optional Unlock path for users already running Shizuku: executes the same
 * idempotent shell grants as the USB/Wireless paths through a Shizuku user
 * service (adb-level privileges), so no PC and no in-app pairing is needed.
 *
 * The permission-result listener is a SINGLETON registered once: both the
 * Unlock screen and Lab embed this flow, and Shizuku dispatches results to
 * every registered listener — two per-VM listeners meant double grant runs.
 */
object ShizukuGrant {

    const val SHIZUKU_PACKAGE = "moe.shizuku.manager"
    const val PERMISSION_REQUEST_CODE = 9001

    /** Bump when IGrantService / ShizukuGrantService changes shape. */
    private const val SERVICE_VERSION = 1
    private const val BIND_TIMEOUT_MS = 10_000L

    @Volatile private var listenerRegistered = false

    /** Single in-flight permission callback; set right before requesting. */
    @Volatile private var pendingPermissionResult: ((Boolean) -> Unit)? = null

    private val requestListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode != PERMISSION_REQUEST_CODE) return@OnRequestPermissionResultListener
            val callback = pendingPermissionResult
            pendingPermissionResult = null
            callback?.invoke(grantResult == PackageManager.PERMISSION_GRANTED)
        }

    private fun ensureListener() {
        if (!listenerRegistered) {
            synchronized(this) {
                if (!listenerRegistered) {
                    Shizuku.addRequestPermissionResultListener(requestListener)
                    listenerRegistered = true
                }
            }
        }
    }

    /**
     * Requests Shizuku permission and invokes [onResult] exactly once with the
     * outcome. Safe to call from either embedded Unlock UI instance.
     */
    fun requestPermission(onResult: (Boolean) -> Unit) {
        ensureListener()
        pendingPermissionResult = onResult
        runCatching { Shizuku.requestPermission(PERMISSION_REQUEST_CODE) }
            .onFailure {
                pendingPermissionResult = null
                onResult(false)
            }
    }

    fun isInstalled(context: Context): Boolean = runCatching {
        context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        true
    }.getOrDefault(false)

    /** True when the Shizuku service binder is alive and speaks API ≥ 11. */
    fun isServiceRunning(): Boolean = runCatching {
        Shizuku.pingBinder() && Shizuku.getVersion() >= 11
    }.getOrDefault(false)

    fun hasPermission(): Boolean = runCatching {
        Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    /**
     * Binds our user service inside Shizuku's server and runs each Unlock
     * command there, checking exit codes. Fails with a presentable message.
     */
    suspend fun runGrants(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            check(hasPermission()) { "Shizuku permission not granted" }
            val service = withTimeoutOrNull(BIND_TIMEOUT_MS) { bindUserService(context) }
                ?: throw IllegalStateException("Shizuku did not respond in time")
            try {
                for (command in ElevatedPermissions.UNLOCK_SHELL_COMMANDS) {
                    val exit = service.runCommand(command)
                    check(exit == 0) { "`$command` exited with $exit" }
                }
                Unit
            } finally {
                runCatching { service.exit() }
            }
        }
    }

    private suspend fun bindUserService(context: Context): IGrantService =
        suspendCancellableCoroutine { cont ->
            val args = Shizuku.UserServiceArgs(
                ComponentName(context, ShizukuGrantService::class.java),
            ).version(SERVICE_VERSION)
            lateinit var connection: ServiceConnection
            connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: android.os.IBinder?) {
                    if (binder == null || !binder.pingBinder()) {
                        cont.resumeWithException(IllegalStateException("Shizuku service returned no binder"))
                        return
                    }
                    cont.resume(IGrantService.Stub.asInterface(binder))
                }

                override fun onServiceDisconnected(name: ComponentName?) = Unit
                override fun onBindingDied(name: ComponentName?) {
                    if (cont.isActive) {
                        cont.resumeWithException(IllegalStateException("Shizuku binding died"))
                    }
                }
            }
            try {
                Shizuku.bindUserService(args, connection)
                cont.invokeOnCancellation {
                    runCatching { Shizuku.unbindUserService(args, connection, false) }
                }
            } catch (t: Throwable) {
                if (cont.isActive) cont.resumeWithException(t)
            }
        }
}
