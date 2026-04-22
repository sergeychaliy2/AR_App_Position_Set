package com.arpositionset.app.presentation.ar.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arpositionset.app.presentation.theme.ArColors

/**
 * Scanner frame overlay, shown while the AR session is Ready but no marker is
 * being tracked yet. Visual language uses the brand palette (green +
 * turquoise + red accents) and the SBP-style scanner — four bracketed
 * corners with a pulsing border and a sweeping horizontal scan line.
 *
 * The overlay is non-interactive: camera touches for plane placement pass
 * through the transparent areas.
 */
@Composable
fun MarkerScannerOverlay(
    visible: Boolean,
    orientationHint: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(200)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                ScanFrame()
                ScanSweep()
            }

            Column(
                modifier = Modifier.padding(top = 360.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    color = ArColors.SurfaceGlass,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Наведите на маркер",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = ArColors.OnSurface,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = orientationHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = ArColors.OnSurfaceMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanFrame() {
    val transition = rememberInfiniteTransition(label = "scanframe")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val inset = 6f
        val cornerLen = size.minDimension * 0.18f
        val stroke = 4f
        val baseColor = ArColors.Turquoise
        val glow = baseColor.copy(alpha = pulse)

        // outer subtle frame (full rectangle, low opacity)
        drawRoundRect(
            color = baseColor.copy(alpha = 0.12f * pulse),
            topLeft = Offset(inset, inset),
            size = Size(size.width - 2 * inset, size.height - 2 * inset),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f, 28f),
            style = Stroke(width = stroke * 0.5f),
        )

        // 4 corner brackets
        val corners = listOf(
            Offset(inset, inset) to Pair(1f, 1f),                             // top-left
            Offset(size.width - inset, inset) to Pair(-1f, 1f),               // top-right
            Offset(inset, size.height - inset) to Pair(1f, -1f),              // bottom-left
            Offset(size.width - inset, size.height - inset) to Pair(-1f, -1f), // bottom-right
        )
        for ((corner, dirs) in corners) {
            val (dx, dy) = dirs
            // horizontal leg
            drawLine(
                color = glow,
                start = corner,
                end = Offset(corner.x + cornerLen * dx, corner.y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            // vertical leg
            drawLine(
                color = glow,
                start = corner,
                end = Offset(corner.x, corner.y + cornerLen * dy),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun ScanSweep() {
    val transition = rememberInfiniteTransition(label = "scansweep")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val inset = 22f
        val top = inset + (size.height - 2 * inset) * progress
        val gradient = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                ArColors.Turquoise.copy(alpha = 0.85f),
                ArColors.BrandRed.copy(alpha = 0.65f),
                ArColors.Turquoise.copy(alpha = 0.85f),
                Color.Transparent,
            ),
            startX = inset,
            endX = size.width - inset,
        )
        drawLine(
            brush = gradient,
            start = Offset(inset, top),
            end = Offset(size.width - inset, top),
            strokeWidth = 3f,
            cap = StrokeCap.Round,
        )
    }
}
