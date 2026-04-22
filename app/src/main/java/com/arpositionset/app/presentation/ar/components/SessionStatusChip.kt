package com.arpositionset.app.presentation.ar.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arpositionset.app.domain.model.ArSessionState
import com.arpositionset.app.domain.model.TrackedMarker
import com.arpositionset.app.presentation.theme.ArColors

@Composable
fun SessionStatusChip(
    state: ArSessionState,
    downloadProgress: Float?,
    trackedMarker: TrackedMarker?,
    modifier: Modifier = Modifier,
) {
    val (label, color) = when (state) {
        ArSessionState.Initializing -> "Инициализация ARCore…" to ArColors.SecondaryCyan
        ArSessionState.RequiresInstall -> "Установите ARCore" to ArColors.Warning
        ArSessionState.Searching -> "Двигайте телефоном, чтобы найти поверхности" to ArColors.SecondaryCyan
        ArSessionState.Ready -> "Коснитесь поверхности — поставим объект" to ArColors.Success
        ArSessionState.TrackingLost -> "Отслеживание потеряно" to ArColors.Warning
        is ArSessionState.Unsupported -> "Устройство не поддерживает AR" to ArColors.Error
        is ArSessionState.Failed -> "Ошибка сессии" to ArColors.Error
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(
            color = ArColors.SurfaceGlass,
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 4.dp,
            modifier = Modifier
                .padding(top = 72.dp)
                .clip(RoundedCornerShape(18.dp)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                PulsingDot(color = color)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = ArColors.OnSurface,
                )
            }
        }

        AnimatedVisibility(
            visible = trackedMarker != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
        ) {
            val isTracking = trackedMarker?.isTracking == true
            Surface(
                color = if (isTracking) ArColors.Success.copy(alpha = 0.18f) else ArColors.SurfaceGlass,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(top = 120.dp)
                    .clip(RoundedCornerShape(16.dp)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ImageSearch,
                        contentDescription = null,
                        tint = if (isTracking) ArColors.Success else ArColors.OnSurfaceMuted,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isTracking) "Маркер активен: ${trackedMarker?.name}"
                        else "Маркер потерян: ${trackedMarker?.name}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isTracking) ArColors.Success else ArColors.OnSurfaceMuted,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = downloadProgress != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
        ) {
            Surface(
                color = ArColors.SurfaceElevated.copy(alpha = 0.92f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .padding(top = 168.dp)
                    .clip(RoundedCornerShape(18.dp)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Загрузка модели",
                        style = MaterialTheme.typography.labelMedium,
                        color = ArColors.OnSurface,
                    )
                    Spacer(Modifier.width(12.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress ?: 0f },
                        color = ArColors.SecondaryCyan,
                        trackColor = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier.width(140.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(
        modifier = Modifier
            .size(10.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color),
    )
}
