package com.arpositionset.app.data.remote

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.arpositionset.app.core.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Copies a user-picked file from a Storage Access Framework URI into app
 * private storage so the AR layer can load it from a stable path.
 */
@Singleton
class SafImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    data class ImportedFile(val path: String, val displayName: String, val sizeBytes: Long)

    suspend fun import(uri: Uri): ImportedFile = withContext(dispatcher) {
        val resolver: ContentResolver = context.contentResolver
        val meta = resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) else "model.glb"
                val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
                name to size
            } else null
        } ?: ("model.glb" to 0L)

        val dir = File(context.filesDir, IMPORT_DIR).apply { mkdirs() }
        val targetName = uniqueName(dir, meta.first)
        val target = File(dir, targetName)
        resolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { out -> input.copyTo(out) }
        } ?: error("Не удалось открыть выбранный файл")

        ImportedFile(path = target.absolutePath, displayName = meta.first, sizeBytes = meta.second)
    }

    private fun uniqueName(dir: File, desired: String): String {
        if (!File(dir, desired).exists()) return desired
        val dotIdx = desired.lastIndexOf('.')
        val base = if (dotIdx > 0) desired.substring(0, dotIdx) else desired
        val ext = if (dotIdx > 0) desired.substring(dotIdx) else ""
        var i = 1
        while (File(dir, "${base}_$i$ext").exists()) i++
        return "${base}_$i$ext"
    }

    companion object {
        private const val IMPORT_DIR = "imported_models"
    }
}
