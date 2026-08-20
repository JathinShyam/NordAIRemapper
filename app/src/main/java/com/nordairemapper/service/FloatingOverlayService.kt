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
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import com.nordairemapper.domain.model.OverlayAnimation
import com.nordairemapper.domain.model.OverlayVisualStyle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Floating Plus Key menu. Hosts Compose in a WindowManager overlay, so this
 * service must manually provide Lifecycle / SavedState / ViewModelStore owners
 * and drive lifecycle to RESUMED (LifecycleService alone is not enough for a
 * stable Compose overlay on OxygenOS).
 */
@AndroidEntryPoint
class FloatingOverlayService :
    android.app.Service(),
    androidx.lifecycle.LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    @Inject lateinit var remapConfigRepository: RemapConfigRepository
    @Inject lateinit var actionDispatcher: ActionDispatcher

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
        // Attach/restore BEFORE moving lifecycle out of INITIALIZED.
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        try {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } catch (t: Throwable) {
            Log.e(TAG, "startForeground failed", t)
            toast("Overlay failed to start")
            stopSelf()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW not granted")
            toast("Allow Display over other apps")
            stopSelf()
            return START_NOT_STICKY
        }

        val keyguard = getSystemService(KeyguardManager::class.java)
        if (keyguard?.isKeyguardLocked == true) {
            Log.w(TAG, "Keyguard locked; overlay suppressed")
            toast("Unlock phone to show overlay")
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch {
            runCatching {
                val config = remapConfigRepository.observeOverlayConfig().first()
                if (!config.enabled) {
                    Log.w(TAG, "Overlay disabled in settings")
                    toast("Turn on Enable overlay in Overlay settings")
                    stopSelf()
                    return@launch
                }
                val slots = config.slots
                    .filter { it !is RemapAction.None }
                    .take(OverlayConfig.MAX_SLOTS)
                if (slots.isEmpty()) {
                    Log.w(TAG, "No overlay slots configured")
                    toast("Fill Overlay slots first")
                    stopSelf()
                    return@launch
                }
                Log.d(TAG, "Showing overlay with ${slots.size} slots")
                showOverlay(config.copy(slots = slots))
            }.onFailure { t ->
                Log.e(TAG, "Failed to show overlay", t)
                toast("Overlay crashed — check logcat")
                dismissAndStop()
            }
        }
        return START_NOT_STICKY
    }

    private fun showOverlay(config: OverlayConfig) {
        dismissOverlay()
        val wm = getSystemService(WindowManager::class.java) ?: error("No WindowManager")
        windowManager = wm

        val root = FrameLayout(this).apply {
            // Owners must live on the window root so Compose can walk the tree.
            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeViewModelStoreOwner(this@FloatingOverlayService)
            setViewTreeSavedStateRegistryOwner(this@FloatingOverlayService)
        }
        overlayRoot = root

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeViewModelStoreOwner(this@FloatingOverlayService)
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
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        wm.addView(root, params)
        Log.d(TAG, "Overlay window attached")
    }

    private fun dismissAndStop() {
        dismissOverlay()
        stopSelf()
    }

    private fun dismissOverlay() {
        overlayRoot?.let { view ->
            runCatching { windowManager?.removeViewImmediate(view) }
                .onFailure { Log.w(TAG, "removeView failed", it) }
        }
        overlayRoot = null
    }

    private fun toast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
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
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        store.clear()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "FloatingOverlay"
        private const val CHANNEL_ID = "overlay"
        private const val NOTIFICATION_ID = 2

        fun show(context: Context) {
            if (!Settings.canDrawOverlays(context)) {
                Log.w(TAG, "canDrawOverlays=false; skipping start")
                Toast.makeText(
                    context.applicationContext,
                    "Allow Display over other apps for overlay",
                    Toast.LENGTH_SHORT,
                ).show()
                return
            }
            runCatching {
                context.applicationContext.startForegroundService(
                    Intent(context.applicationContext, FloatingOverlayService::class.java),
                )
            }.onFailure { t ->
                Log.e(TAG, "startForegroundService failed", t)
                Toast.makeText(
                    context.applicationContext,
                    "Could not start overlay service",
                    Toast.LENGTH_SHORT,
                ).show()
            }
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
    val accent = Color(config.accentColorArgb)
    val onePlus = config.visualStyle == OverlayVisualStyle.ONEPLUS
    val surfaceColor = if (onePlus) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    } else {
        Color(0xFFF2F2F2).copy(alpha = 0.96f)
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
            color = surfaceColor,
            modifier = Modifier
                .padding(20.dp)
                .alpha(config.opacity.coerceIn(0.3f, 1f))
                .then(
                    if (config.glowEffects) {
                        Modifier.shadow(
                            elevation = 14.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = accent.copy(alpha = 0.4f),
                            spotColor = accent.copy(alpha = 0.5f),
                        )
                    } else {
                        Modifier
                    },
                )
                .then(
                    if (config.glowEffects && onePlus) {
                        Modifier.border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    } else {
                        Modifier
                    },
                )
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
                        slots.forEachIndexed { index, action ->
                            OverlayActionButton(
                                action = action,
                                iconDp = iconDp,
                                index = index,
                                accent = accent,
                                animation = config.animation,
                                position = config.position,
                                onClick = { onAction(action) },
                            )
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
                            slots.take(3).forEachIndexed { index, action ->
                                OverlayActionButton(
                                    action = action,
                                    iconDp = iconDp,
                                    index = index,
                                    accent = accent,
                                    animation = config.animation,
                                    position = config.position,
                                    onClick = { onAction(action) },
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            slots.drop(3).forEachIndexed { index, action ->
                                OverlayActionButton(
                                    action = action,
                                    iconDp = iconDp,
                                    index = index + 3,
                                    accent = accent,
                                    animation = config.animation,
                                    position = config.position,
                                    onClick = { onAction(action) },
                                )
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
    index: Int,
    accent: Color,
    animation: OverlayAnimation,
    position: OverlayPosition,
    onClick: () -> Unit,
) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(animation) {
        delay(index * 50L)
        appear.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.65f, stiffness = 380f),
        )
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer {
                val t = appear.value
                alpha = t
                when (animation) {
                    OverlayAnimation.FADE -> Unit
                    OverlayAnimation.SCALE -> {
                        scaleX = 0.86f + t * 0.14f
                        scaleY = 0.86f + t * 0.14f
                        translationY = (1f - t) * 18f
                    }
                    OverlayAnimation.SLIDE -> {
                        val fromX = when (position) {
                            OverlayPosition.LEFT_EDGE -> -24f
                            OverlayPosition.RIGHT_EDGE -> 24f
                            OverlayPosition.BOTTOM_CENTER -> 0f
                        }
                        translationX = (1f - t) * fromX
                        translationY = if (position == OverlayPosition.BOTTOM_CENTER) {
                            (1f - t) * 24f
                        } else {
                            0f
                        }
                    }
                }
            }
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(iconDp + 16.dp)
                .background(accent.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon(),
                contentDescription = action.displayName(),
                tint = accent,
                modifier = Modifier.size(iconDp * 0.55f),
            )
        }
        Text(
            text = action.displayName(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}
