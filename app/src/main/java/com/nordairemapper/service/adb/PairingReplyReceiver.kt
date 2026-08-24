package com.nordairemapper.service.adb

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nordairemapper.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives the 6-digit pairing code typed into the heads-up notification and
 * completes the Built-In Wireless pairing + grant flow — the user never has
 * to leave the system pairing dialog.
 */
@AndroidEntryPoint
class PairingReplyReceiver : BroadcastReceiver() {

    @Inject lateinit var grantViaWirelessAdb: ReadLogsGrantViaWirelessAdb

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "onReceive: reply received")
        val code = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(PairingNotifier.KEY_CODE)
            ?.toString()
            ?.filter { it.isDigit() }
            .orEmpty()
        Log.i(TAG, "code digits=${code.length}")

        if (code.length != 6) {
            PairingNotifier.postPrompt(
                context,
                PairingSession.pairingPort,
                errorLine = "That wasn't 6 digits — enter the full code.",
            )
            return
        }

        if (!PairingSession.isActive) {
            PairingNotifier.postResult(
                context,
                ok = false,
                "Pairing session expired — open Keyforge and start again.",
            )
            return
        }

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                PairingNotifier.postProgress(appContext, "Pairing with the code you entered…")
                Log.i(TAG, "Notification reply: attempting pair+grant")
                val result = grantViaWirelessAdb.pairAndGrant(
                    pairingCode = code,
                    host = PairingSession.host,
                    pairingPort = PairingSession.pairingPort,
                    connectPort = PairingSession.connectPort,
                )
                when (result) {
                    is ReadLogsGrantViaWirelessAdb.GrantResult.AlreadyGranted,
                    is ReadLogsGrantViaWirelessAdb.GrantResult.Success,
                    -> {
                        // Bring the user back into the app — allowed here
                        // because Keyforge usually holds SYSTEM_ALERT_WINDOW;
                        // if the system blocks it, the result banner taps through.
                        runCatching {
                            appContext.startActivity(
                                Intent(appContext, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                        PairingNotifier.postResult(
                            appContext,
                            ok = true,
                            "You're all set — assign presses on Home.",
                        )
                    }
                    is ReadLogsGrantViaWirelessAdb.GrantResult.Failed ->
                        PairingNotifier.postResult(appContext, ok = false, result.message)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Reply pairing crashed", t)
                PairingNotifier.postResult(appContext, ok = false, t.message ?: t.javaClass.simpleName)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "PairingReply"
    }
}
