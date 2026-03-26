package com.verdant.core.ai.mediapipe

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.verdant.core.datastore.UserPreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Download + lifecycle state for the on-device LLM.
 */
sealed interface ModelDownloadState {
    data object NotDownloaded : ModelDownloadState
    data class Downloading(val progress: Float) : ModelDownloadState
    data object Downloaded : ModelDownloadState
    data object Loading : ModelDownloadState
    data object Ready : ModelDownloadState
    data class Error(val message: String) : ModelDownloadState
    data object Unsupported : ModelDownloadState
}

private const val MODEL_DIR = "models"
private const val MODEL_FILENAME = "gemma-2b-it-q4.bin"
private const val MODEL_URL =
    "https://storage.googleapis.com/verdant-ai-models/gemma-2b-it-q4.bin"
private const val MIN_RAM_BYTES = 6L * 1024 * 1024 * 1024 // 6 GB
private const val IDLE_TIMEOUT_MS = 60_000L

/**
 * Manages the on-device Gemma 2B model: download, load/unload lifecycle,
 * device capability checks, and memory-pressure handling.
 *
 * Thread-safe: all inference must go through [runInference] which serializes
 * access via a [Mutex].
 */
@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val prefs: UserPreferencesDataStore,
) {
    private val okHttpClient = OkHttpClient()
    private val _state = MutableStateFlow<ModelDownloadState>(ModelDownloadState.NotDownloaded)
    val state: StateFlow<ModelDownloadState> = _state.asStateFlow()

    private var llmInference: LlmInference? = null
    private val inferenceMutex = Mutex()
    private var lastInferenceTime = 0L

    private val modelFile: File
        get() = File(appContext.filesDir, "$MODEL_DIR/$MODEL_FILENAME")

    init {
        if (!isDeviceSupported()) {
            _state.value = ModelDownloadState.Unsupported
        } else if (modelFile.exists()) {
            _state.value = ModelDownloadState.Downloaded
        }

        // Register memory-pressure callback
        appContext.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
                    unloadModel()
                }
            }

            override fun onConfigurationChanged(newConfig: Configuration) {}

            @Deprecated("Deprecated in Java")
            override fun onLowMemory() {
                unloadModel()
            }
        })
    }

    fun isDeviceSupported(): Boolean {
        val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        return memInfo.totalMem >= MIN_RAM_BYTES
    }

    /**
     * Downloads the model file with progress reporting.
     * No-op if already downloaded or device is unsupported.
     */
    suspend fun downloadModel() {
        if (_state.value is ModelDownloadState.Unsupported) return
        if (modelFile.exists()) {
            _state.value = ModelDownloadState.Downloaded
            return
        }

        withContext(Dispatchers.IO) {
            try {
                _state.value = ModelDownloadState.Downloading(0f)

                val request = Request.Builder().url(MODEL_URL).build()
                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    _state.value = ModelDownloadState.Error("Download failed: HTTP ${response.code}")
                    return@withContext
                }

                val body = response.body ?: run {
                    _state.value = ModelDownloadState.Error("Empty response body")
                    return@withContext
                }

                val totalBytes = body.contentLength()
                val dir = File(appContext.filesDir, MODEL_DIR)
                if (!dir.exists()) dir.mkdirs()

                val tempFile = File(dir, "$MODEL_FILENAME.tmp")
                var bytesWritten = 0L

                body.byteStream().use { input ->
                    tempFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesWritten += read
                            if (totalBytes > 0) {
                                _state.value = ModelDownloadState.Downloading(
                                    bytesWritten.toFloat() / totalBytes,
                                )
                            }
                        }
                    }
                }

                tempFile.renameTo(modelFile)
                prefs.setOnDeviceModelDownloaded(true)
                _state.value = ModelDownloadState.Downloaded
            } catch (e: Exception) {
                _state.value = ModelDownloadState.Error(e.message ?: "Download failed")
            }
        }
    }

    /**
     * Runs inference with the given prompt. Loads the model if needed.
     * Serialized via mutex — only one inference at a time.
     */
    suspend fun runInference(prompt: String): String = inferenceMutex.withLock {
        val inference = getOrLoadInference()
        lastInferenceTime = System.currentTimeMillis()
        withContext(Dispatchers.Default) {
            inference.generateResponse(prompt)
        }
    }

    /**
     * Returns the loaded [LlmInference], loading the model if necessary.
     * Must be called within [inferenceMutex].
     */
    private suspend fun getOrLoadInference(): LlmInference {
        llmInference?.let { return it }

        if (!modelFile.exists()) {
            throw IllegalStateException("Model not downloaded")
        }

        _state.value = ModelDownloadState.Loading

        return withContext(Dispatchers.IO) {
            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(256)
                    .setMaxTopK(40)
                    .build()

                LlmInference.createFromOptions(appContext, options).also {
                    llmInference = it
                    _state.value = ModelDownloadState.Ready
                }
            } catch (e: Exception) {
                _state.value = ModelDownloadState.Error("Failed to load model: ${e.message}")
                throw e
            }
        }
    }

    /** Unloads the model from memory. Safe to call from any thread. */
    fun unloadModel() {
        llmInference?.close()
        llmInference = null
        if (_state.value is ModelDownloadState.Ready || _state.value is ModelDownloadState.Loading) {
            _state.value = if (modelFile.exists()) ModelDownloadState.Downloaded else ModelDownloadState.NotDownloaded
        }
    }

    /** Deletes the downloaded model file and resets state. */
    suspend fun deleteModel() {
        unloadModel()
        withContext(Dispatchers.IO) {
            if (modelFile.exists()) modelFile.delete()
        }
        prefs.setOnDeviceModelDownloaded(false)
        _state.value = if (isDeviceSupported()) ModelDownloadState.NotDownloaded else ModelDownloadState.Unsupported
    }
}
