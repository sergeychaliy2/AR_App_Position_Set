package com.arpositionset.app.data.remote

import android.content.Context
import android.net.Uri
import com.arpositionset.app.core.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Copies a user-picked image (via SAF) into app private storage at
 * `filesDir/markers/<uuid>.<ext>`. Returns the absolute path so the DB can
 * store a stable `file://` reference independent of the original content URI,
 * which may be revoked by the system at any time.
 */
@Singleton
class MarkerImageImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    suspend fun import(uri: Uri): String = withContext(dispatcher) {
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        val ext = guessExtension(uri)
        val target = File(dir, "${UUID.randomUUID()}$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { out -> input.copyTo(out) }
        } ?: error("Не удалось открыть выбранное изображение")
        target.absolutePath
    }

    private fun guessExtension(uri: Uri): String {
        val mime = context.contentResolver.getType(uri).orEmpty()
        return when {
            mime.contains("png", ignoreCase = true) -> ".png"
            mime.contains("jpeg", ignoreCase = true) || mime.contains("jpg", ignoreCase = true) -> ".jpg"
            mime.contains("webp", ignoreCase = true) -> ".webp"
            else -> ".img"
        }
    }

    private companion object {
        const val DIR = "markers"
    }
}
