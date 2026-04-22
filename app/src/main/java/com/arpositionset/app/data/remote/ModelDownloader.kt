package com.arpositionset.app.data.remote

import android.content.Context
import com.arpositionset.app.core.IoDispatcher
import com.arpositionset.app.core.Outcome
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.buffer
import okio.sink
import timber.log.Timber

/**
 * Downloads remote model files to the app cache directory. Emits progress as a
 * percent [0..1] via [Outcome.Progress] and terminates with [Outcome.Success]
 * holding the local file path, or [Outcome.Failure].
 */
@Singleton
class ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    fun download(url: String, targetName: String): Flow<Outcome<String>> = callbackFlow {
        val targetDir = File(context.filesDir, CLOUD_DIR).apply { mkdirs() }
        val target = File(targetDir, targetName)
        if (target.exists() && target.length() > 0) {
            trySend(Outcome.Success(target.absolutePath))
            close()
            return@callbackFlow
        }

        val request = Request.Builder().url(url).get().build()
        val call = httpClient.newCall(request)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Timber.e(e, "Model download failed")
                trySend(Outcome.Failure(e, "Не удалось скачать модель"))
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { r ->
                    if (!r.isSuccessful) {
                        val err = IOException("HTTP ${r.code}")
                        trySend(Outcome.Failure(err, "Ошибка сервера: ${r.code}"))
                        close(err)
                        return
                    }
                    val body = r.body
                    if (body == null) {
                        val err = IOException("Empty body")
                        trySend(Outcome.Failure(err, "Пустой ответ сервера"))
                        close(err)
                        return
                    }
                    val total = body.contentLength().coerceAtLeast(1L)
                    try {
                        target.sink().buffer().use { sink ->
                            val source = body.source()
                            var read = 0L
                            val buffer = okio.Buffer()
                            val chunk = 64 * 1024L
                            while (true) {
                                val bytes = source.read(buffer, chunk)
                                if (bytes == -1L) break
                                sink.write(buffer, bytes)
                                read += bytes
                                trySend(Outcome.Progress((read.toFloat() / total.toFloat()).coerceIn(0f, 1f)))
                            }
                            sink.flush()
                        }
                        trySend(Outcome.Success(target.absolutePath))
                    } catch (t: Throwable) {
                        target.delete()
                        trySend(Outcome.Failure(t, "Ошибка записи файла"))
                        close(t)
                        return
                    }
                    close()
                }
            }
        })

        awaitClose { call.cancel() }
    }.flowOn(dispatcher)

    suspend fun importLocal(sourceBytes: ByteArray, targetName: String): String =
        withContext(dispatcher) {
            val dir = File(context.filesDir, IMPORT_DIR).apply { mkdirs() }
            val target = File(dir, targetName)
            target.outputStream().use { it.write(sourceBytes) }
            target.absolutePath
        }

    companion object {
        private const val CLOUD_DIR = "cloud_models"
        private const val IMPORT_DIR = "imported_models"
    }
}
