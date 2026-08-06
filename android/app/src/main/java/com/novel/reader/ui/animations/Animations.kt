package com.novel.reader.ui.animations

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.dp

/**
 * 丝滑入场动画 - 淡入 + 轻微上移
 */
@Composable
fun Modifier.slideInFromBottom(
    visible: Boolean,
    delayMs: Int = 0,
): Modifier {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = delayMs, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 24.dp,
        animationSpec = tween(durationMillis = 500, delayMillis = delayMs, easing = FastOutSlowInEasing),
        label = "offset"
    )
    return this.alpha(alpha).offset(y = offsetY)
}

/**
 * 延迟入场 - 用于列表项的依次出现
 */
@Composable
fun rememberDelayedVisibility(index: Int, baseDelay: Int = 60): Boolean {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(baseDelay * index.toLong())
        visible = true
    }
    return visible
}

/**
 * 渐变淡入
 */
@Composable
fun Modifier.fadeInTransition(visible: Boolean, durationMs: Int = 400): Modifier {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        label = "fade"
    )
    return this.alpha(alpha)
}

/**
 * 缩放淡入
 */
@Composable
fun Modifier.scaleFadeIn(visible: Boolean, durationMs: Int = 400): Modifier {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        label = "scaleAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        label = "scale"
    )
    return this.alpha(alpha).graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * 弹性缩放
 */
@Composable
fun Modifier.bounceClick(pressed: Boolean): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "bounce"
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}
