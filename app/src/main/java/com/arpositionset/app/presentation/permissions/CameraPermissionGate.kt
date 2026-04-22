package com.arpositionset.app.presentation.permissions

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arpositionset.app.R
import com.arpositionset.app.presentation.theme.ArColors
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Blocks AR content until the user grants camera permission.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionGate(content: @Composable () -> Unit) {
    val state = rememberPermissionState(Manifest.permission.CAMERA)
    if (state.status.isGranted) {
        content()
    } else {
        PermissionRationale(
            onRequest = { state.launchPermissionRequest() },
        )
    }
}

@Composable
private fun PermissionRationale(onRequest: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ArColors.BackgroundDark, ArColors.SurfaceDark))),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = ArColors.SurfaceElevated,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = null,
                    tint = ArColors.PrimaryViolet,
                    modifier = Modifier.size(56.dp),
                )
                Text(
                    text = stringResource(R.string.permission_camera_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = ArColors.OnSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.permission_camera_rationale),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ArColors.OnSurfaceMuted,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ArColors.PrimaryViolet,
                        contentColor = ArColors.OnPrimary,
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(stringResource(R.string.permission_camera_grant))
                }
            }
        }
    }
}
