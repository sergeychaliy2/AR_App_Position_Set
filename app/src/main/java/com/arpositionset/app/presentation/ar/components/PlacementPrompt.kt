package com.arpositionset.app.presentation.ar.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arpositionset.app.presentation.theme.ArColors

/**
 * Floating confirmation card that rises from the centre after the user taps a
 * surface. Two actions: "Установить" (confirm) and "Закрыть" (cancel).
 */
@Composable
fun PlacementPrompt(
    visible: Boolean,
    candidateName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(280)) { it / 3 } + fadeIn(tween(280)),
            exit = slideOutVertically(tween(220)) { it / 3 } + fadeOut(tween(220)),
        ) {
            Surface(
                color = ArColors.SurfaceElevated.copy(alpha = 0.92f),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 12.dp,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 96.dp)
                    .clip(RoundedCornerShape(28.dp)),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(ArColors.SecondaryCyan),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Точка размещения",
                            style = MaterialTheme.typography.labelLarge,
                            color = ArColors.SecondaryCyan,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = candidateName.ifBlank { "Объект" },
                        style = MaterialTheme.typography.titleLarge,
                        color = ArColors.OnSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PromptButton(
                            label = "Закрыть",
                            icon = Icons.Rounded.Close,
                            background = ArColors.SurfaceDark,
                            contentColor = ArColors.OnSurface,
                            onClick = onCancel,
                        )
                        PromptButton(
                            label = "Установить",
                            icon = Icons.Rounded.Check,
                            background = ArColors.PrimaryViolet,
                            contentColor = ArColors.OnPrimary,
                            onClick = onConfirm,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        color = background,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
            .height(52.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}
