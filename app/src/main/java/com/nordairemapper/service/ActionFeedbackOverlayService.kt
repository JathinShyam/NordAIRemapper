package com.nordairemapper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
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
import com.nordairemapper.domain.model.OverlayAnimation
import com.nordairemapper.domain.model.OverlayConfig
import com.nordairemapper.domain.model.OverlayVisualStyle
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.domain.repository.RemapConfigRepository
import com.nordairemapper.presentation.MainActivity
import com.nordairemapper.presentation.common.displayName
import com.nordairemapper.presentation.common.icon
import com.nordairemapper.ui.theme.NordAIRemapperTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

        val action = pendingAction.getAndSet(null)
        if (action == null || action is RemapAction.None) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } catch (t: Throwable) {
            Log.e(TAG, "startForeground failed", t)
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
                showPopup(action, config)
                delay(config.holdDurationMs.coerceIn(300L, 2000L))
            }.onFailure { t ->
                Log.e(TAG, "Action feedback failed", t)
            }
            dismissAndStop()
        }
        return START_NOT_STICKY
    }

    private fun showPopup(action: RemapAction, config: OverlayConfig) {
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
                    ActionFeedbackContent(action = action, config = config)
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

        private val pendingAction = AtomicReference<RemapAction?>(null)

        fun show(context: Context, action: RemapAction) {
            if (action is RemapAction.None || action is RemapAction.ShowOverlay) return
            if (!Settings.canDrawOverlays(context)) {
                Log.d(TAG, "Skipping feedback — no overlay permission")
                return
            }
            pendingAction.set(action)
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

@androidx.compose.runtime.Composable
private fun ActionFeedbackContent(
    action: RemapAction,
    config: OverlayConfig,
) {
    val accent = Color(config.accentColorArgb)
    val onePlus = config.visualStyle == OverlayVisualStyle.ONEPLUS
    val surface = if (onePlus) Color(0xFF141414) else Color(0xFFF2F2F2)
    val onSurface = if (onePlus) Color(0xFFF5F5F5) else Color(0xFF1A1A1A)
    val appear = remember { Animatable(0f) }

    LaunchedEffect(config.animation) {
        appear.snapTo(0f)
        appear.animateTo(1f, animationSpec = tween(220))
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .padding(bottom = 96.dp)
                .graphicsLayer {
                    val t = appear.value
                    alpha = t
                    when (config.animation) {
                        OverlayAnimation.FADE -> Unit
                        OverlayAnimation.SCALE -> {
                            scaleX = 0.86f + 0.14f * t
                            scaleY = 0.86f + 0.14f * t
                        }
                        OverlayAnimation.SLIDE -> {
                            translationY = (1f - t) * 28f
                        }
                    }
                }
                .then(
                    if (config.glowEffects) {
                        Modifier.shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = accent.copy(alpha = 0.45f),
                            spotColor = accent.copy(alpha = 0.55f),
                        )
                    } else {
                        Modifier
                    },
                )
                .background(surface.copy(alpha = 0.94f), RoundedCornerShape(28.dp))
                .then(
                    if (config.glowEffects && onePlus) {
                        Modifier.border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(28.dp))
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (onePlus) accent.copy(alpha = 0.2f) else Color(0xFF9E9E9E).copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = action.icon(),
                    contentDescription = null,
                    tint = if (onePlus) accent else Color(0xFF616161),
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = action.displayName(),
                style = MaterialTheme.typography.titleSmall,
                color = onSurface,
                maxLines = 1,
            )
        }
    }
}
