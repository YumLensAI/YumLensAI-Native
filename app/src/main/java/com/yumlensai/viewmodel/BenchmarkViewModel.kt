package com.yumlensai.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yumlensai.data.api.ApiClient
import com.yumlensai.data.api.model.BenchmarkRecord
import com.yumlensai.data.api.model.StoredBenchmark
import com.yumlensai.BuildConfig
import com.yumlensai.data.dataset.TestDatasetManager
import com.yumlensai.data.ml.PredictModel
import com.yumlensai.data.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.UUID

enum class BenchmarkStatus {
    LOADING, READY, INDISPONIBLE, EXECUTING, SAVING, COMPLETED, FAILED
}

data class BenchmarkState(
    val status: BenchmarkStatus = BenchmarkStatus.LOADING,
    val step: Int = 0,
    val total: Int = 35000
)

class BenchmarkViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val EXECUTION_TESTS_NUMBER = BuildConfig.EXECUTION_TESTS_NUMBER
        private val MINIMAL_BATTERY_LEVEL = BuildConfig.MINIMAL_BATTERY_LEVEL
        private const val TOTAL_TEST_IMAGES = 112
    }

    private val _localBenchmark = MutableStateFlow(BenchmarkState())
    val localBenchmark: StateFlow<BenchmarkState> = _localBenchmark.asStateFlow()

    private val _backendBenchmark = MutableStateFlow(BenchmarkState())
    val backendBenchmark: StateFlow<BenchmarkState> = _backendBenchmark.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _deviceModel = MutableStateFlow(Build.MODEL)
    val deviceModel: StateFlow<String> = _deviceModel.asStateFlow()

    private val _osVersion = MutableStateFlow("Android ${Build.VERSION.RELEASE}")
    val osVersion: StateFlow<String> = _osVersion.asStateFlow()

    private val _ramMemory = MutableStateFlow("")
    val ramMemory: StateFlow<String> = _ramMemory.asStateFlow()

    val backendUrl: String = BuildConfig.BACKEND_URL

    private val predictModel = PredictModel(application)
    private val testDataset = TestDatasetManager(application)

    private var wakeLock: PowerManager.WakeLock? = null

    init {
        loadRamInfo()
        prepareDataset()
        initModels()
    }

    private fun prepareDataset() {
        viewModelScope.launch(Dispatchers.IO) {
            testDataset.prepareAll()
        }
    }

    private fun loadRamInfo() {
        val activityManager = getApplication<Application>()
            .getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val gb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        _ramMemory.value = "%.2f GB".format(gb)
    }

    private fun initModels() {
        viewModelScope.launch(Dispatchers.IO) {
            predictModel.load()
            _localBenchmark.value = _localBenchmark.value.copy(
                status = if (predictModel.state == PredictModel.State.Loaded)
                    BenchmarkStatus.READY else BenchmarkStatus.INDISPONIBLE
            )
        }
        viewModelScope.launch {
            try {
                ApiClient.service.checkHealth()
                _backendBenchmark.value = _backendBenchmark.value.copy(
                    status = BenchmarkStatus.READY
                )
            } catch (e: Exception) {
                _backendBenchmark.value = _backendBenchmark.value.copy(
                    status = BenchmarkStatus.INDISPONIBLE
                )
            }
        }
    }

    fun executeLocalBenchmark() {
        viewModelScope.launch(Dispatchers.IO) {
            acquireWakeLock()
            _localBenchmark.value = _localBenchmark.value.copy(
                status = BenchmarkStatus.EXECUTING,
                step = 0,
                total = EXECUTION_TESTS_NUMBER
            )
            val records = mutableListOf<BenchmarkRecord>()

            for (i in 0 until EXECUTION_TESTS_NUMBER) {
                val battery = getBatteryLevel()
                if (battery != null && battery <= MINIMAL_BATTERY_LEVEL) break

                val imageIndex = (1..TOTAL_TEST_IMAGES).random()
                val imageName = "img%03d.jpg".format(imageIndex)
                val imageUri = getTestImageUri(imageIndex)

                val startTime = System.currentTimeMillis()
                val initialBattery = battery?.toString() ?: "unknown"
                val detected = predictModel.runInference(imageUri)
                val endTime = System.currentTimeMillis()
                val finalBattery = getBatteryLevel()?.toString() ?: "unknown"

                records.add(
                    BenchmarkRecord(
                        fileName = imageName,
                        detectedObjects = detected,
                        startTimestamp = startTime,
                        endTimestamp = endTime,
                        initialBattery = initialBattery,
                        finalBattery = finalBattery,
                        elapsedTime = endTime - startTime
                    )
                )
                _localBenchmark.value = _localBenchmark.value.copy(step = i + 1)
            }

            _localBenchmark.value = _localBenchmark.value.copy(status = BenchmarkStatus.SAVING)

            StorageManager.saveBenchmark(
                getApplication(),
                StoredBenchmark(
                    id = UUID.randomUUID().toString(),
                    environment = "local",
                    records = records
                )
            )

            releaseWakeLock()
            _localBenchmark.value = _localBenchmark.value.copy(status = BenchmarkStatus.COMPLETED)
            triggerSync()
        }
    }

    fun executeBackendBenchmark() {
        viewModelScope.launch(Dispatchers.IO) {
            acquireWakeLock()
            _backendBenchmark.value = _backendBenchmark.value.copy(
                status = BenchmarkStatus.EXECUTING,
                step = 0,
                total = EXECUTION_TESTS_NUMBER
            )
            val records = mutableListOf<BenchmarkRecord>()

            try {
                for (i in 0 until EXECUTION_TESTS_NUMBER) {
                    val battery = getBatteryLevel()
                    if (battery != null && battery <= MINIMAL_BATTERY_LEVEL) break

                    val imageIndex = (1..TOTAL_TEST_IMAGES).random()
                    val imageName = "img%03d.jpg".format(imageIndex)
                    val imageUri = getTestImageUri(imageIndex)

                    val startTime = System.currentTimeMillis()
                    val initialBattery = battery?.toString() ?: "unknown"

                    val context = getApplication<Application>()
                    val file = withContext(Dispatchers.IO) {
                        val inputStream = context.contentResolver.openInputStream(imageUri)!!
                        val tempFile = File.createTempFile("img", ".jpg", context.cacheDir)
                        tempFile.outputStream().use { inputStream.copyTo(it) }
                        tempFile
                    }
                    val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("image", file.name, requestBody)
                    val detected = ApiClient.service.predict(part)

                    val endTime = System.currentTimeMillis()
                    val finalBattery = getBatteryLevel()?.toString() ?: "unknown"

                    records.add(
                        BenchmarkRecord(
                            fileName = imageName,
                            detectedObjects = detected,
                            startTimestamp = startTime,
                            endTimestamp = endTime,
                            initialBattery = initialBattery,
                            finalBattery = finalBattery,
                            elapsedTime = endTime - startTime
                        )
                    )
                    _backendBenchmark.value = _backendBenchmark.value.copy(step = i + 1)
                }

                _backendBenchmark.value = _backendBenchmark.value.copy(status = BenchmarkStatus.SAVING)

                StorageManager.saveBenchmark(
                    getApplication(),
                    StoredBenchmark(
                        id = UUID.randomUUID().toString(),
                        environment = "backend",
                        records = records
                    )
                )

                _backendBenchmark.value = _backendBenchmark.value.copy(status = BenchmarkStatus.COMPLETED)
                triggerSync()
            } catch (e: Exception) {
                _backendBenchmark.value = _backendBenchmark.value.copy(status = BenchmarkStatus.FAILED)
            } finally {
                releaseWakeLock()
            }
        }
    }

    fun triggerSync() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            val context = getApplication<Application>()
            val unsynced = StorageManager.getUnsyncedBenchmarks(context)
            for (benchmark in unsynced) {
                try {
                    when (benchmark.environment) {
                        "local" -> ApiClient.service.saveBenchmarkLocal(benchmark.records)
                        "backend" -> ApiClient.service.saveBenchmarkBackend(benchmark.records)
                    }
                    StorageManager.markBenchmarkSynced(context, benchmark.id)
                } catch (e: Exception) {
                    // Continue on failure
                }
            }
            _isSyncing.value = false
        }
    }

    private fun getBatteryLevel(): Float? {
        val batteryIntent = getApplication<Application>().registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return null
        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level >= 0 && scale > 0) level.toFloat() / scale else null
    }

    private fun getTestImageUri(imageIndex: Int): android.net.Uri = testDataset.getImageUri(imageIndex)

    private fun acquireWakeLock() {
        val pm = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "YumLensAI:BenchmarkWakeLock")
        wakeLock?.acquire(60 * 60 * 1000L)
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onCleared() {
        predictModel.close()
        releaseWakeLock()
        super.onCleared()
    }
}
