package com.nordairemapper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.nordairemapper.R
import com.nordairemapper.domain.model.ActionFeedback
import com.nordairemapper.domain.model.OverlayConfig
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.domain.repository.RemapConfigRepository
import com.nordairemapper.presentation.MainActivity
import com.nordairemapper.presentation.common.caption
import com.nordairemapper.presentation.common.icon
import com.nordairemapper.ui.components.VisualActionPopupLayer
import com.nordairemapper.ui.theme.NordAIRemapperTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/**
 * Brief action popup shown when a remap fires and Visual Overlay is enabled.
 * Distinct from [FloatingOverlayService] (interactive multi-slot menu).
 */
@AndroidEntryPoint
class ActionFeedbackOverlayService :
    android.app.Service(),
    androidx.lifecycle.LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    @Inject lateinit var remapConfigRepository: RemapConfigRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    private var windowManager: WindowManager? = null
    private var overlayRoot: FrameLayout? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        // FGS contract first: every startForegroundService() must be followed
        // by startForeground(), even on the null-action early-exit path.
        try {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "startForeground failed", t)
            stopSelf()
            return START_NOT_STICKY
        }

        val action = pendingAction.getAndSet(null)
        if (action == null || action.action is RemapAction.None) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW not granted; skipping action popup")
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch {
            runCatching {
                val config = remapConfigRepository.observeOverlayConfig().first()
                val appIcon = resolveAppIcon(action.action)
                showPopup(action, config, appIcon)
                delay(config.holdDurationMs.coerceIn(300L, 2000L))
            }.onFailure { t ->
                Log.e(TAG, "Action feedback failed", t)
            }
            dismissAndStop()
        }
        return START_NOT_STICKY
    }

    private suspend fun resolveAppIcon(action: RemapAction): ImageBitmap? {
        val packageName = (action as? RemapAction.LaunchApp)?.packageName ?: return null
        if (packageName.isBlank()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap()
            }.onFailure { t ->
                Log.w(TAG, "App icon load failed for $packageName", t)
            }.getOrNull()
        }
    }

    private fun showPopup(
        feedback: ActionFeedback,
        config: OverlayConfig,
        appIcon: ImageBitmap?,
    ) {
        dismissOverlay()
        val wm = getSystemService(WindowManager::class.java) ?: return
        windowManager = wm

        val root = FrameLayout(this).apply {
            setViewTreeLifecycleOwner(this@ActionFeedbackOverlayService)
            setViewTreeViewModelStoreOwner(this@ActionFeedbackOverlayService)
            setViewTreeSavedStateRegistryOwner(this@ActionFeedbackOverlayService)
        }
        overlayRoot = root

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ActionFeedbackOverlayService)
            setViewTreeViewModelStoreOwner(this@ActionFeedbackOverlayService)
            setViewTreeSavedStateRegistryOwner(this@ActionFeedbackOverlayService)
            setContent {
                NordAIRemapperTheme {
                    VisualActionPopupLayer(
                        icon = feedback.icon(),
                        caption = feedback.caption(),
                        appIcon = appIcon,
                        accent = androidx.compose.ui.graphics.Color(config.accentColorArgb),
                        visualStyle = config.visualStyle,
                        glowEffects = config.glowEffects,
                        animation = config.animation,
                    )
                }
            }
        }
        root.addView(
            composeView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        wm.addView(root, params)
    }

    private fun dismissAndStop() {
        dismissOverlay()
        stopSelf()
    }

    private fun dismissOverlay() {
        overlayRoot?.let { view ->
            runCatching { windowManager?.removeViewImmediate(view) }
        }
        overlayRoot = null
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Action feedback", NotificationManager.IMPORTANCE_MIN),
        )
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Action feedback")
            .setContentText("Showing remap popup")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        dismissOverlay()
        serviceScope.cancel()
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        store.clear()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ActionFeedback"
        private const val CHANNEL_ID = "action_feedback"
        private const val NOTIFICATION_ID = 3

        private val pendingAction = AtomicReference<ActionFeedback?>(null)

        fun show(context: Context, feedback: ActionFeedback) {
            if (feedback.action is RemapAction.None || feedback.action is RemapAction.ShowOverlay) return
            if (!Settings.canDrawOverlays(context)) {
                Log.d(TAG, "Skipping feedback — no overlay permission")
                return
            }
            pendingAction.set(feedback)
            runCatching {
                context.applicationContext.startForegroundService(
                    Intent(context.applicationContext, ActionFeedbackOverlayService::class.java),
                )
            }.onFailure { t ->
                Log.e(TAG, "startForegroundService failed", t)
                pendingAction.set(null)
            }
        }
    }
}
