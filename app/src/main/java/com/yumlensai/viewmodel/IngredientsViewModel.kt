package com.yumlensai.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yumlensai.data.api.ApiClient
import com.yumlensai.data.api.model.Bookmark
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

class IngredientsViewModel(application: Application) : AndroidViewModel(application) {

    private val _ingredients = MutableStateFlow<List<String>>(emptyList())
    val ingredients: StateFlow<List<String>> = _ingredients.asStateFlow()

    private val _toggles = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val toggles: StateFlow<Map<String, Boolean>> = _toggles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _bookmarkId = MutableStateFlow<String?>(null)
    val bookmarkId: StateFlow<String?> = _bookmarkId.asStateFlow()

    private val _isOfflineTipVisible = MutableStateFlow(false)
    val isOfflineTipVisible: StateFlow<Boolean> = _isOfflineTipVisible.asStateFlow()

    private val predictModel = PredictModel(application)

    init {
        // Pre-warm the model so it's likely ready when inference is requested
        viewModelScope.launch(Dispatchers.IO) { predictModel.load() }
    }

    fun loadFromBookmark(bookmark: Bookmark) {
        _bookmarkId.value = bookmark.id
        setIngredients(bookmark.ingredients)
    }

    fun runLocalInference(imageUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val detected = withContext(Dispatchers.IO) {
                    predictModel.load() // idempotent — ensures model is ready before inference
                    predictModel.runInference(imageUri)
                }
                setIngredients(detected)
            } catch (e: Exception) {
                setIngredients(emptyList())
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun runServerInference(imageUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val detected = withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val file = uriToFile(imageUri, context)
                    val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("image", file.name, requestBody)
                    ApiClient.service.predict(part)
                }
                setIngredients(detected)
            } catch (e: Exception) {
                runLocalInference(imageUri)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleIngredient(key: String) {
        val current = _toggles.value.toMutableMap()
        current[key] = !(current[key] ?: true)
        _toggles.value = current
    }

    fun getSelectedIngredients(): List<String> =
        _toggles.value.filter { it.value }.keys.toList()

    fun saveBookmark(imageUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val bookmark = StorageManager.saveBookmark(
                context,
                imageUri,
                _ingredients.value
            )
            _bookmarkId.value = bookmark.id
        }
    }

    fun removeBookmark() {
        val id = _bookmarkId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            StorageManager.removeBookmark(getApplication(), id)
            _bookmarkId.value = null
        }
    }

    fun showOfflineTip() {
        _isOfflineTipVisible.value = true
    }

    fun hideOfflineTip() {
        _isOfflineTipVisible.value = false
    }

    private fun setIngredients(list: List<String>) {
        _ingredients.value = list
        _toggles.value = list.associateWith { true }
    }

    private fun uriToFile(uri: Uri, context: android.content.Context): File {
        val inputStream = context.contentResolver.openInputStream(uri)!!
        val tempFile = File.createTempFile("img", ".jpg", context.cacheDir)
        tempFile.outputStream().use { inputStream.copyTo(it) }
        return tempFile
    }

    override fun onCleared() {
        predictModel.close()
        super.onCleared()
    }
}
