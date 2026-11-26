package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MsnWink
import com.example.ui.theme.parseHexColor
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WinkOverlay(
    wink: MsnWink?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = wink != null,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(240))
    ) {
        if (wink != null) {
            val scale = remember(wink.id) { Animatable(0.3f) }
            val burst = remember(wink.id) { Animatable(0f) }
            val accent = remember(wink.accentHex) { parseHexColor(wink.accentHex, Color(0xFF0F62C9)) }

            LaunchedEffect(wink.id) {
                scale.animateTo(
                    targetValue = 1.25f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                burst.animateTo(1f, animationSpec = tween(1100, easing = FastOutSlowInEasing))
                delay(1100L)
                onDismiss()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxR = minOf(size.width, size.height) * 0.42f
                    val progress = burst.value
                    for (i in 0 until 16) {
                        val angle = (i * (360f / 16f)) * (Math.PI / 180.0)
                        val radius = maxR * (0.25f + 0.75f * progress)
                        val pt = Offset(
                            center.x + (cos(angle) * radius).toFloat(),
                            center.y + (sin(angle) * radius).toFloat()
                        )
                        drawCircle(
                            color = accent.copy(alpha = (1f - progress * 0.75f).coerceIn(0f, 1f)),
                            radius = (18f - 10f * progress).coerceAtLeast(4f),
                            center = pt
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = wink.glyph,
                        fontSize = 96.sp,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                            rotationZ = (burst.value - 0.5f) * 18f
                        }
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = accent
                    ) {
                        Text(
                            text = wink.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
