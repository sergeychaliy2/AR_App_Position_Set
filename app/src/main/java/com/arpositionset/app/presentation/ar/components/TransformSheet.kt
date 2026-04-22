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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Rotate90DegreesCw
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.unit.dp
import com.arpositionset.app.domain.model.PlacedObject
import com.arpositionset.app.domain.model.TransformState
import com.arpositionset.app.presentation.ar.Axis
import com.arpositionset.app.presentation.theme.ArColors

@Composable
fun TransformSheet(
    placed: PlacedObject?,
    onScaleChange: (String, Float) -> Unit,
    onRotationChange: (String, Float) -> Unit,
    onRemove: (String) -> Unit,
    onMove: (String) -> Unit,
    onNudge: (String, Axis, Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = placed != null,
            enter = slideInVertically(tween(280)) { it } + fadeIn(tween(280)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(220)),
        ) {
            val target = placed ?: return@AnimatedVisibility
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = ArColors.SurfaceElevated.copy(alpha = 0.94f),
                tonalElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 110.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Handle()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = target.sourceObject.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = ArColors.OnSurface,
                            )
                            Text(
                                text = target.sourceObject.category.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = ArColors.SecondaryCyan,
                            )
                        }
                        RoundIconButton(
                            icon = Icons.Rounded.OpenWith,
                            tint = ArColors.SecondaryCyan,
                            onClick = { onMove(target.placementId) },
                        )
                        Spacer(Modifier.width(8.dp))
                        RoundIconButton(
                            icon = Icons.Rounded.DeleteOutline,
                            tint = ArColors.Error,
                            onClick = { onRemove(target.placementId) },
                        )
                        Spacer(Modifier.width(8.dp))
                        RoundIconButton(
                            icon = Icons.Rounded.Close,
                            tint = ArColors.OnSurface,
                            onClick = onDismiss,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    TransformRow(
                        icon = Icons.Rounded.ZoomOutMap,
                        label = "Масштаб",
                        value = target.transform.scale,
                        valueRange = TransformState.MIN_SCALE..TransformState.MAX_SCALE,
                        format = { "x%.2f".format(it) },
                        onChange = { onScaleChange(target.placementId, it) },
                    )
                    Spacer(Modifier.height(8.dp))
                    TransformRow(
                        icon = Icons.Rounded.Rotate90DegreesCw,
                        label = "Поворот",
                        value = target.transform.rotationYDegrees,
                        valueRange = 0f..360f,
                        format = { "%.0f°".format(it) },
                        onChange = { onRotationChange(target.placementId, it) },
                    )
                    Spacer(Modifier.height(12.dp))
                    NudgeGrid(
                        onNudge = { axis, signed -> onNudge(target.placementId, axis, signed) },
                    )
                }
            }
        }
    }
}

/** 6-кнопочная сетка для точного сдвига на ±5 см по осям X/Y/Z в мировых
 *  координатах. Для крупных перемещений — кнопка «Переместить» (tap-to-place). */
@Composable
private fun NudgeGrid(onNudge: (Axis, Float) -> Unit) {
    val step = 0.05f  // 5 см за одно нажатие
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NudgeAxis(label = "X", color = ArColors.BrandRed,
            onMinus = { onNudge(Axis.X, -step) }, onPlus = { onNudge(Axis.X, step) })
        NudgeAxis(label = "Y", color = ArColors.LightGreen,
            onMinus = { onNudge(Axis.Y, -step) }, onPlus = { onNudge(Axis.Y, step) })
        NudgeAxis(label = "Z", color = ArColors.SkyBlue,
            onMinus = { onNudge(Axis.Z, -step) }, onPlus = { onNudge(Axis.Z, step) })
    }
}

@Composable
private fun RowScope.NudgeAxis(label: String, color: Color, onMinus: () -> Unit, onPlus: () -> Unit) {
    Surface(
        color = ArColors.SurfaceDark,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.weight(1f),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                color = color.copy(alpha = 0.18f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .size(36.dp)
                    .pointerInput(Unit) { detectTapGestures(onTap = { onMinus() }) },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("−", color = color, style = MaterialTheme.typography.titleLarge)
                }
            }
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelLarge,
            )
            Surface(
                color = color.copy(alpha = 0.18f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .size(36.dp)
                    .pointerInput(Unit) { detectTapGestures(onTap = { onPlus() }) },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+", color = color, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

@Composable
private fun Handle() {
    Box(
        modifier = Modifier
            .padding(bottom = 12.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ArColors.OnSurfaceMuted.copy(alpha = 0.5f)),
        )
    }
}

@Composable
private fun TransformRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    // The slider owns its drag state locally. Re-keying on `label` resets
    // it only when the selected placement changes. Re-syncing on every external
    // `value` update (as the old `if (local != value) local = value` did) caused
    // a feedback loop: external StateFlow emission → local update → onChange →
    // StateFlow emission, visible as slider "jitter" during drag.
    var local by remember(label) { mutableStateOf(value) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = ArColors.PrimaryViolet.copy(alpha = 0.18f),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ArColors.PrimaryViolet,
                modifier = Modifier.padding(8.dp).size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = ArColors.OnSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = format(local),
                    style = MaterialTheme.typography.labelMedium,
                    color = ArColors.SecondaryCyan,
                )
            }
            Slider(
                value = local,
                onValueChange = { local = it; onChange(it) },
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = ArColors.PrimaryViolet,
                    activeTrackColor = ArColors.PrimaryViolet,
                    inactiveTrackColor = Color.White.copy(alpha = 0.14f),
                ),
            )
        }
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Surface(
        color = ArColors.SurfaceDark,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .size(40.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
