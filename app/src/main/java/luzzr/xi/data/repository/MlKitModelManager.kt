package luzzr.xi.data.repository

import android.util.Log
import luzzr.xi.domain.model.ModelDownloadProgress
import luzzr.xi.domain.model.ModelDownloadState
import luzzr.xi.domain.model.SupportedLanguage
import luzzr.xi.domain.repository.MlKitModelGateway
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class LanguageModelStatus(
    val sourceLang: String,
    val targetLang: String,
    val state: ModelDownloadState = ModelDownloadState.IDLE,
    val progress: Float = 0f
)

@Singleton
class MlKitModelManager @Inject constructor() : MlKitModelGateway {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val downloadStates = ConcurrentHashMap<String, ModelDownloadState>()
    private val downloadProgress = ConcurrentHashMap<String, ModelDownloadProgress>()
    private val listeners = CopyOnWriteArrayList<(Map<String, ModelDownloadState>) -> Unit>()
    private val progressListeners = CopyOnWriteArrayList<(String, ModelDownloadProgress) -> Unit>()

    private var downloadJob: Job? = null
    private var currentProgressJob: Job? = null
    private var currentDownloadKey: String? = null

    fun getStatus(sourceLang: String, targetLang: String): ModelDownloadState {
        return downloadStates[key(sourceLang, targetLang)] ?: ModelDownloadState.IDLE
    }

    override suspend fun isModelDownloaded(
        sourceLang: String, 
        targetLang: String,
        sourceCode: String,
        targetCode: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelKey = key(sourceLang, targetLang)
            if (downloadStates[modelKey] == ModelDownloadState.COMPLETED) {
                return@withContext true
            }
            val modelManager = RemoteModelManager.getInstance()
            val sCode = convertCode(sourceCode)
            val tCode = convertCode(targetCode)

            val sourceModel = TranslateRemoteModel.Builder(sCode).build()
            val isSourceOk = Tasks.await(modelManager.isModelDownloaded(sourceModel))
            
            val targetModel = TranslateRemoteModel.Builder(tCode).build()
            val isTargetOk = Tasks.await(modelManager.isModelDownloaded(targetModel))

            val result = isSourceOk && isTargetOk
            if (result) {
                downloadStates[modelKey] = ModelDownloadState.COMPLETED
            }
            result
        } catch (e: Exception) {
            Log.e("MlKitModelManager", "isModelDownloaded check failed", e)
            false
        }
    }

    fun addListener(listener: (Map<String, ModelDownloadState>) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (Map<String, ModelDownloadState>) -> Unit) {
        listeners.remove(listener)
    }

    override fun addProgressListener(listener: (String, ModelDownloadProgress) -> Unit) {
        progressListeners.add(listener)
    }

    override fun removeProgressListener(listener: (String, ModelDownloadProgress) -> Unit) {
        progressListeners.remove(listener)
    }

    override suspend fun downloadModel(
        sourceLang: String,
        targetLang: String,
        sourceCode: String?,
        targetCode: String?
    ): Result<Unit> {
        if (sourceCode == null || targetCode == null) {
            return Result.failure(Exception("Unsupported language"))
        }

        val modelKey = key(sourceLang, targetLang)

        // Cancel a *different* previously in-flight download (do not cancel our own job).
        val myJob = coroutineContext[Job]
        val prevJob = downloadJob
        if (prevJob != null && prevJob != myJob) {
            prevJob.cancel()
            currentProgressJob?.cancel()
            currentProgressJob = null
            val prevKey = currentDownloadKey
            if (prevKey != null && prevKey != modelKey &&
                downloadStates[prevKey] == ModelDownloadState.DOWNLOADING
            ) {
                downloadStates[prevKey] = ModelDownloadState.IDLE
                downloadProgress[prevKey] = downloadProgress[prevKey]?.copy(
                    state = ModelDownloadState.IDLE,
                    progress = 0f
                ) ?: ModelDownloadProgress(state = ModelDownloadState.IDLE)
                notifyProgressListeners(prevKey)
            }
        }
        currentDownloadKey = modelKey
        // Track this coroutine as the in-flight job so cancelDownload() can actually
        // cancel a download started directly (e.g. from the ViewModel's scope), not just
        // the unused retryDownload() path.
        downloadJob = myJob

        downloadStates[modelKey] = ModelDownloadState.DOWNLOADING
        downloadProgress[modelKey] = ModelDownloadProgress(
            state = ModelDownloadState.DOWNLOADING,
            progress = 0f
        )
        notifyListeners()
        notifyProgressListeners(modelKey)

        return try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(convertCode(sourceCode))
                .setTargetLanguage(convertCode(targetCode))
                .build()

            val translator = Translation.getClient(options)
            val conditions = DownloadConditions.Builder().build()

            // Start asymptotic progress simulation
            currentProgressJob = scope.launch {
                simulateProgress(modelKey)
            }

            val result = suspendCancellableCoroutine { cont ->
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        downloadProgress[modelKey] = downloadProgress[modelKey]?.copy(
                            progress = 1f,
                            state = ModelDownloadState.COMPLETED
                        ) ?: ModelDownloadProgress(state = ModelDownloadState.COMPLETED, progress = 1f)
                        notifyProgressListeners(modelKey)

                        translator.close()
                        downloadStates[modelKey] = ModelDownloadState.COMPLETED
                        notifyListeners()
                        if (cont.isActive) cont.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        translator.close()
                        downloadStates[modelKey] = ModelDownloadState.FAILED
                        downloadProgress[modelKey] = downloadProgress[modelKey]?.copy(
                            state = ModelDownloadState.FAILED,
                            message = e.message ?: "Download failed"
                        ) ?: ModelDownloadProgress(
                            state = ModelDownloadState.FAILED,
                            message = e.message ?: "Download failed"
                        )
                        notifyListeners()
                        notifyProgressListeners(modelKey)
                        if (cont.isActive) cont.resume(Result.failure(e))
                    }

                cont.invokeOnCancellation {
                    translator.close()
                    if (currentDownloadKey == modelKey) {
                        downloadStates[modelKey] = ModelDownloadState.FAILED
                        downloadProgress[modelKey] = downloadProgress[modelKey]?.copy(
                            state = ModelDownloadState.FAILED,
                            message = "Download cancelled"
                        ) ?: ModelDownloadProgress(
                            state = ModelDownloadState.FAILED,
                            message = "Download cancelled"
                        )
                        notifyListeners()
                        notifyProgressListeners(modelKey)
                    }
                }
            }

            // Stop the asymptotic progress simulation. The real download already
            // reported its terminal state, so we cancel the sim job instead of
            // join()-ing it (its loop is infinite and only ends via cancellation,
            // which would otherwise deadlock this coroutine on success).
            currentProgressJob?.cancel()
            if (result.isSuccess) {
                downloadProgress[modelKey] = ModelDownloadProgress(
                    state = ModelDownloadState.COMPLETED,
                    progress = 1f
                )
                notifyProgressListeners(modelKey)
            }

            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            downloadStates[modelKey] = ModelDownloadState.FAILED
            downloadProgress[modelKey] = downloadProgress[modelKey]?.copy(
                state = ModelDownloadState.FAILED,
                message = e.message ?: "Download failed"
            ) ?: ModelDownloadProgress(
                state = ModelDownloadState.FAILED,
                message = e.message ?: "Download failed"
            )
            notifyListeners()
            notifyProgressListeners(modelKey)
            Log.e("MlKitModelManager", "Download failed", e)
            Result.failure(e)
        } finally {
            currentProgressJob?.cancel()
        }
    }

    override fun cancelDownload() {
        currentProgressJob?.cancel()
        currentProgressJob = null
        downloadJob?.cancel()
        downloadJob = null

        // Only the in-flight download key is affected, not every DOWNLOADING entry.
        val key = currentDownloadKey
        if (key != null && downloadStates[key] == ModelDownloadState.DOWNLOADING) {
            downloadStates[key] = ModelDownloadState.FAILED
            downloadProgress[key] = downloadProgress[key]?.copy(
                state = ModelDownloadState.FAILED,
                message = "Cancelled"
            ) ?: ModelDownloadProgress(
                state = ModelDownloadState.FAILED,
                message = "Cancelled"
            )
            notifyProgressListeners(key)
        }
        notifyListeners()
    }

    fun retryDownload(
        sourceLang: String,
        targetLang: String,
        sourceCode: String?,
        targetCode: String?
    ) {
        // Cancel any previous job before launching the retry so it cannot self-cancel.
        downloadJob?.cancel()
        currentProgressJob?.cancel()
        currentProgressJob = null
        downloadJob = null

        // Reset state to idle for retry
        val modelKey = key(sourceLang, targetLang)
        downloadStates[modelKey] = ModelDownloadState.IDLE
        downloadProgress[modelKey] = ModelDownloadProgress()
        notifyListeners()
        notifyProgressListeners(modelKey)

        // Start download
        downloadJob = scope.launch {
            downloadModel(sourceLang, targetLang, sourceCode, targetCode)
        }
    }

    private suspend fun simulateProgress(modelKey: String) {
        // Asymptotic curve: progress = 1 - e^(-t/tau)
        // tau=8s means ~63% at 8s, ~86% at 16s, ~95% at 24s, ~98% at 32s
        val tau = 8_000L
        val startTime = System.currentTimeMillis()
        var lastReported = 0f

        while (true) {
            delay(150)
            val elapsed = System.currentTimeMillis() - startTime
            val rawProgress = 1.0 - Math.exp(-elapsed.toDouble() / tau)
            // Only update if changed by >= 1% to avoid flooding
            val newProgress = rawProgress.toFloat().coerceIn(0f, 0.99f)
            if (newProgress - lastReported >= 0.01f) {
                lastReported = newProgress
                downloadProgress[modelKey] = downloadProgress[modelKey]?.copy(
                    progress = newProgress
                ) ?: ModelDownloadProgress(progress = newProgress)
                notifyProgressListeners(modelKey)
            }
        }
    }

    private fun notifyListeners() {
        val snapshot = downloadStates.toMap()
        listeners.forEach { it(snapshot) }
    }

    private fun notifyProgressListeners(modelKey: String) {
        val progress = downloadProgress[modelKey] ?: return
        progressListeners.forEach { it(modelKey, progress) }
    }

    fun getDownloadProgress(modelKey: String): ModelDownloadProgress {
        return downloadProgress[modelKey] ?: ModelDownloadProgress()
    }

    private fun key(source: String, target: String) = "$source->$target"

    private fun convertCode(code: String): String =
        SupportedLanguage.getByCode(code).mlKitLangCode ?: TranslateLanguage.ENGLISH

    fun downloadStatus(): Map<String, ModelDownloadState> = downloadStates.toMap()
}
