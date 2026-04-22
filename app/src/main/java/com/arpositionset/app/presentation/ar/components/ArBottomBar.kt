package com.arpositionset.app.presentation.ar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arpositionset.app.domain.model.ArObject
import com.arpositionset.app.presentation.theme.ArColors

@Composable
fun ArBottomBar(
    selectedObject: ArObject?,
    onOpenGallery: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, ArColors.BackgroundDark.copy(alpha = 0.85f))))
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ArColors.SurfaceElevated.copy(alpha = 0.92f),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onOpenGallery),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = ArColors.PrimaryViolet.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Collections,
                        contentDescription = null,
                        tint = ArColors.PrimaryViolet,
                        modifier = Modifier
                            .size(44.dp)
                            .padding(10.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedObject == null) "Выберите объект" else "Текущий выбор",
                        style = MaterialTheme.typography.labelSmall,
                        color = ArColors.OnSurfaceMuted,
                    )
                    Spacer(Modifier.size(2.dp))
                    Text(
                        text = selectedObject?.name ?: "Откройте галерею и выберите модель",
                        style = MaterialTheme.typography.titleMedium,
                        color = ArColors.OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (selectedObject != null) {
                        Text(
                            text = selectedObject.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = ArColors.SecondaryCyan,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (selectedObject != null) {
                        Icon(
                            imageVector = Icons.Rounded.Inventory,
                            contentDescription = null,
                            tint = ArColors.OnSurfaceMuted,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = ArColors.OnSurfaceMuted,
                    )
                }
            }
        }
    }
}
