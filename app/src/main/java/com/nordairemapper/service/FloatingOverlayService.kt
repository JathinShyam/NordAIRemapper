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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.nordairemapper.domain.model.OverlayAnimation
import com.nordairemapper.domain.model.OverlayConfig
import com.nordairemapper.domain.model.OverlayIconSize
import com.nordairemapper.domain.model.OverlayLayoutStyle
import com.nordairemapper.domain.model.OverlayPosition
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.domain.repository.RemapConfigRepository
import com.nordairemapper.presentation.MainActivity
import com.nordairemapper.presentation.common.displayName
import com.nordairemapper.presentation.common.icon
import com.nordairemapper.ui.components.NordHeading
import com.nordairemapper.ui.theme.NordAIRemapperTheme
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

// ─────────────────────────────────────────────────────────────────────────────
// Composable UI
// ─────────────────────────────────────────────────────────────────────────────

/** Icon size in dp driven by [OverlayIconSize]. */
private fun iconSizeDp(size: OverlayIconSize): Dp = when (size) {
    OverlayIconSize.SMALL -> 18.dp
    OverlayIconSize.MEDIUM -> 22.dp
    OverlayIconSize.LARGE -> 28.dp
}

/** Ring/box size = icon + comfortable padding. */
private fun ringBoxDp(size: OverlayIconSize): Dp = when (size) {
    OverlayIconSize.SMALL -> 34.dp
    OverlayIconSize.MEDIUM -> 40.dp
    OverlayIconSize.LARGE -> 50.dp
}

@Composable
private fun OverlayWindowContent(
    config: OverlayConfig,
    onAction: (RemapAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = Color(config.accentColorArgb)
    val slots = config.slots

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Scrim — full screen dark + radial accent glow, tap to dismiss ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(0.3f * 1080f, 0.2f * 2400f),
                        radius = 600f,
                    ),
                )
                .background(Color(0xFF0B0B0B).copy(alpha = 0.85f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss,
                ),
        )

        // ── Panel — layout style drives rendering; position only used by PILL_BAR ──
        when (config.layoutStyle) {

            // ── RADIAL: always bottom-center sheet, fixed 300dp wide ──────────────
            OverlayLayoutStyle.RADIAL -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 18.dp, vertical = 48.dp)
                        .width(300.dp)            // fixed width — not full screen
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {},
                        )
                        .then(
                            if (config.glowEffects) {
                                Modifier.shadow(
                                    elevation = 24.dp,
                                    shape = RoundedCornerShape(28.dp),
                                    ambientColor = accent.copy(alpha = 0.3f),
                                    spotColor = accent.copy(alpha = 0.4f),
                                )
                            } else Modifier,
                        )
                        .background(
                            Color(0xFF141414).copy(alpha = config.opacity.coerceIn(0.3f, 1f)),
                            RoundedCornerShape(28.dp),
                        )
                        .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(28.dp))
                        .padding(start = 16.dp, top = 22.dp, end = 16.dp, bottom = 18.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        NordHeading(
                            text = "Overlay",
                            style = MaterialTheme.typography.titleLarge.copy(
                                letterSpacing = (-0.02).sp,
                                textAlign = TextAlign.Center,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                        )
                        TileGrid(
                            slots = slots,
                            accent = accent,
                            iconSize = config.iconSize,
                            animation = config.animation,
                            position = OverlayPosition.BOTTOM_CENTER,
                            onAction = onAction,
                        )
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp),
                        ) {
                            Text(
                                text = "Close",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ── PILL_BAR: compact strip, position controls edge/bottom ──────────
            OverlayLayoutStyle.PILL_BAR -> {
                when (config.position) {

                    // Bottom: up to 3 per row, 2 rows max, centred
                    OverlayPosition.BOTTOM_CENTER -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 18.dp, vertical = 48.dp)
                                .wrapContentSize()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = {},
                                )
                                .then(
                                    if (config.glowEffects) Modifier.shadow(
                                        elevation = 20.dp,
                                        shape = RoundedCornerShape(24.dp),
                                        ambientColor = accent.copy(alpha = 0.3f),
                                        spotColor = accent.copy(alpha = 0.4f),
                                    ) else Modifier
                                )
                                .background(
                                    Color(0xFF141414).copy(alpha = config.opacity.coerceIn(0.3f, 1f)),
                                    RoundedCornerShape(24.dp),
                                )
                                .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(24.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                // Row 1: first 3 slots
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    slots.take(3).forEachIndexed { i, action ->
                                        PillTile(
                                            action = action, index = i, accent = accent,
                                            iconSize = config.iconSize, animation = config.animation,
                                            position = config.position, onClick = { onAction(action) },
                                        )
                                    }
                                }
                                // Row 2: slots 4-6 if present
                                if (slots.size > 3) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        slots.drop(3).forEachIndexed { i, action ->
                                            PillTile(
                                                action = action, index = i + 3, accent = accent,
                                                iconSize = config.iconSize, animation = config.animation,
                                                position = config.position, onClick = { onAction(action) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Left/Right edges: narrow vertical column hugging the edge
                    OverlayPosition.LEFT_EDGE, OverlayPosition.RIGHT_EDGE -> {
                        val edgeAlignment = if (config.position == OverlayPosition.LEFT_EDGE)
                            Alignment.CenterStart else Alignment.CenterEnd
                        Box(
                            modifier = Modifier
                                .align(edgeAlignment)
                                .padding(vertical = 0.dp)   // no vertical push — centred naturally
                                .wrapContentSize()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = {},
                                )
                                .then(
                                    if (config.glowEffects) Modifier.shadow(
                                        elevation = 20.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        ambientColor = accent.copy(alpha = 0.3f),
                                        spotColor = accent.copy(alpha = 0.4f),
                                    ) else Modifier
                                )
                                .background(
                                    Color(0xFF141414).copy(alpha = config.opacity.coerceIn(0.3f, 1f)),
                                    RoundedCornerShape(20.dp),
                                )
                                .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(20.dp))
                                .padding(10.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                slots.forEachIndexed { i, action ->
                                    PillTile(
                                        action = action, index = i, accent = accent,
                                        iconSize = config.iconSize, animation = config.animation,
                                        position = config.position, onClick = { onAction(action) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── 3×2 grid (RADIAL layout) ─────────────────────────────────────────────

@Composable
private fun TileGrid(
    slots: List<RemapAction>,
    accent: Color,
    iconSize: OverlayIconSize,
    animation: OverlayAnimation,
    position: OverlayPosition,
    onAction: (RemapAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Row 1 — slots 0..2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            slots.take(3).forEachIndexed { i, action ->
                OverlayTile(
                    action = action,
                    index = i,
                    accent = accent,
                    iconSize = iconSize,
                    animation = animation,
                    position = position,
                    onClick = { onAction(action) },
                    modifier = Modifier.weight(1f),
                )
            }
            repeat((3 - slots.take(3).size).coerceAtLeast(0)) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        // Row 2 — slots 3..5
        if (slots.size > 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                slots.drop(3).forEachIndexed { i, action ->
                    OverlayTile(
                        action = action,
                        index = i + 3,
                        accent = accent,
                        iconSize = iconSize,
                        animation = animation,
                        position = position,
                        onClick = { onAction(action) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat((3 - slots.drop(3).size).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ─── Square tile (RADIAL layout) ──────────────────────────────────────────

@Composable
private fun OverlayTile(
    action: RemapAction,
    index: Int,
    accent: Color,
    iconSize: OverlayIconSize,
    animation: OverlayAnimation,
    position: OverlayPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(index) {
        delay(index * 50L)
        appear.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.65f, stiffness = 380f),
        )
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                val t = appear.value
                alpha = t
                when (animation) {
                    OverlayAnimation.FADE -> Unit
                    OverlayAnimation.SCALE -> {
                        scaleX = 0.86f + t * 0.14f
                        scaleY = 0.86f + t * 0.14f
                        translationY = (1f - t) * 18.dp.toPx()
                    }
                    OverlayAnimation.SLIDE -> {
                        val fromX = when (position) {
                            OverlayPosition.LEFT_EDGE  -> -28f
                            OverlayPosition.RIGHT_EDGE -> 28f
                            OverlayPosition.BOTTOM_CENTER -> 0f
                        }
                        translationX = (1f - t) * fromX
                        translationY = if (position == OverlayPosition.BOTTOM_CENTER) {
                            (1f - t) * 28.dp.toPx()
                        } else 0f
                    }
                }
            }
            .background(Color(0xFF171717), RoundedCornerShape(22.dp))
            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(ringBoxDp(iconSize))
                    .background(accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                    .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = action.icon(),
                    contentDescription = action.displayName(),
                    tint = accent,
                    modifier = Modifier.size(iconSizeDp(iconSize)),
                )
            }
            Text(
                text = action.displayName(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─── Compact icon-only pill (PILL_BAR layout) ─────────────────────────────

@Composable
private fun PillTile(
    action: RemapAction,
    index: Int,
    accent: Color,
    iconSize: OverlayIconSize,
    animation: OverlayAnimation,
    position: OverlayPosition,
    onClick: () -> Unit,
) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(index) {
        delay(index * 45L)
        appear.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 420f),
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
                        scaleX = 0.7f + t * 0.3f
                        scaleY = 0.7f + t * 0.3f
                    }
                    OverlayAnimation.SLIDE -> {
                        val dx = when (position) {
                            OverlayPosition.LEFT_EDGE  -> -20f
                            OverlayPosition.RIGHT_EDGE -> 20f
                            OverlayPosition.BOTTOM_CENTER -> 0f
                        }
                        translationX = (1f - t) * dx
                        translationY = if (position == OverlayPosition.BOTTOM_CENTER) (1f - t) * 20f else 0f
                    }
                }
            }
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(ringBoxDp(iconSize))
                .background(accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon(),
                contentDescription = action.displayName(),
                tint = accent,
                modifier = Modifier.size(iconSizeDp(iconSize)),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = action.displayName(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
