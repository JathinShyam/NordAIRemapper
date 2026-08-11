package com.nordairemapper.service

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.nordairemapper.R
import com.nordairemapper.domain.model.OverlayConfig
import com.nordairemapper.domain.model.OverlayIconSize
import com.nordairemapper.domain.model.OverlayLayoutStyle
import com.nordairemapper.domain.model.OverlayPosition
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.domain.repository.RemapConfigRepository
import com.nordairemapper.presentation.MainActivity
import com.nordairemapper.presentation.common.displayName
import com.nordairemapper.presentation.common.icon
import com.nordairemapper.ui.theme.NordAIRemapperTheme
import com.nordairemapper.ui.theme.NordBlue
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FloatingOverlayService : LifecycleService(), SavedStateRegistryOwner {

    @Inject lateinit var remapConfigRepository: RemapConfigRepository
    @Inject lateinit var actionDispatcher: ActionDispatcher

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayRoot: FrameLayout? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )

        val keyguard = getSystemService(KeyguardManager::class.java)
        if (keyguard?.isKeyguardLocked == true) {
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch {
            val config = remapConfigRepository.observeOverlayConfig().first()
            if (!config.enabled && config.slots.isEmpty()) {
                // Still allow ShowOverlay to display configured slots even if master toggle off,
                // as long as slots exist; if empty, bail.
            }
            val slots = config.slots.filter { it !is RemapAction.None }.take(OverlayConfig.MAX_SLOTS)
            if (slots.isEmpty()) {
                stopSelf()
                return@launch
            }
            showOverlay(config.copy(slots = slots))
        }
        return START_NOT_STICKY
    }

    private fun showOverlay(config: OverlayConfig) {
        dismissOverlay()
        val wm = getSystemService(WindowManager::class.java)
        windowManager = wm

        val root = FrameLayout(this)
        overlayRoot = root

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeSavedStateRegistryOwner(this@FloatingOverlayService)
            setContent {
                NordAIRemapperTheme {
                    OverlayWindowContent(
                        config = config,
                        onAction = { action ->
                            serviceScope.launch {
                                actionDispatcher.execute(action)
                                dismissAndStop()
                            }
                        },
                        onDismiss = { dismissAndStop() },
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
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = when (config.position) {
                OverlayPosition.LEFT_EDGE -> Gravity.START or Gravity.CENTER_VERTICAL
                OverlayPosition.RIGHT_EDGE -> Gravity.END or Gravity.CENTER_VERTICAL
                OverlayPosition.BOTTOM_CENTER -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
        }

        wm.addView(root, params)
    }

    private fun dismissAndStop() {
        dismissOverlay()
        stopSelf()
    }

    private fun dismissOverlay() {
        overlayRoot?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        overlayRoot = null
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Overlay", NotificationManager.IMPORTANCE_MIN)
        )
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Overlay menu")
            .setContentText("Floating Plus Key actions")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        dismissOverlay()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "overlay"
        private const val NOTIFICATION_ID = 2

        fun show(context: Context) {
            if (!android.provider.Settings.canDrawOverlays(context)) return
            context.startForegroundService(Intent(context, FloatingOverlayService::class.java))
        }
    }
}

@androidx.compose.runtime.Composable
private fun OverlayWindowContent(
    config: OverlayConfig,
    onAction: (RemapAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val iconDp = when (config.iconSize) {
        OverlayIconSize.SMALL -> 28.dp
        OverlayIconSize.MEDIUM -> 36.dp
        OverlayIconSize.LARGE -> 48.dp
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
        contentAlignment = when (config.position) {
            OverlayPosition.LEFT_EDGE -> Alignment.CenterStart
            OverlayPosition.RIGHT_EDGE -> Alignment.CenterEnd
            OverlayPosition.BOTTOM_CENTER -> Alignment.BottomCenter
        },
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            modifier = Modifier
                .padding(20.dp)
                .alpha(config.opacity.coerceIn(0.3f, 1f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
        ) {
            val slots = config.slots
            when (config.layoutStyle) {
                OverlayLayoutStyle.PILL_BAR -> {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        slots.forEach { action ->
                            OverlayActionButton(action, iconDp) { onAction(action) }
                        }
                    }
                }
                OverlayLayoutStyle.RADIAL -> {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            slots.take(3).forEach { action ->
                                OverlayActionButton(action, iconDp) { onAction(action) }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            slots.drop(3).forEach { action ->
                                OverlayActionButton(action, iconDp) { onAction(action) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun OverlayActionButton(
    action: RemapAction,
    iconDp: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(iconDp + 16.dp)
                .background(NordBlue.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon(),
                contentDescription = action.displayName(),
                tint = NordBlue,
                modifier = Modifier.size(iconDp * 0.55f),
            )
        }
        Text(
            text = action.displayName(),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}
