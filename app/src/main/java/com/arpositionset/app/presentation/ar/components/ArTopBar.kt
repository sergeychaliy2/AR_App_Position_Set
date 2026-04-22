package com.arpositionset.app.presentation.ar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Grid4x4
import androidx.compose.material.icons.rounded.GridOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import com.arpositionset.app.presentation.theme.ArColors

@Composable
fun ArTopBar(
    planesVisible: Boolean,
    onTogglePlanes: () -> Unit,
    onClear: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .displayCutoutPadding()
            .background(Brush.verticalGradient(listOf(ArColors.BackgroundDark.copy(alpha = 0.85f), androidx.compose.ui.graphics.Color.Transparent)))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandBadge()
        Spacer(Modifier.weight(1f))
        GlassIconButton(
            icon = Icons.Rounded.Tune,
            onClick = onSettings,
        )
        Spacer(Modifier.width(8.dp))
        GlassIconButton(
            icon = if (planesVisible) Icons.Rounded.Grid4x4 else Icons.Rounded.GridOff,
            active = planesVisible,
            onClick = onTogglePlanes,
        )
        Spacer(Modifier.width(8.dp))
        GlassIconButton(
            icon = Icons.Rounded.DeleteSweep,
            onClick = onClear,
        )
    }
}

@Composable
private fun BrandBadge() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = androidx.compose.ui.graphics.Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(ArColors.GradientAccent)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Grid4x4,
                contentDescription = null,
                tint = ArColors.OnPrimary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "AR Position",
                style = MaterialTheme.typography.labelLarge,
                color = ArColors.OnPrimary,
            )
        }
    }
}

@Composable
private fun GlassIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    val bg = if (active) ArColors.PrimaryViolet.copy(alpha = 0.25f) else ArColors.SurfaceGlass
    Surface(
        color = bg,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = if (active) 4.dp else 2.dp,
        modifier = Modifier
            .size(44.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ArColors.OnSurface,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
