package com.yumlensai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yumlensai.data.api.ApiClient
import com.yumlensai.data.api.model.Bookmark
import com.yumlensai.data.storage.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    private val _isServerUnavailable = MutableStateFlow(false)
    val isServerUnavailable: StateFlow<Boolean> = _isServerUnavailable.asStateFlow()

    init {
        loadBookmarks()
        checkServerHealth()
    }

    fun loadBookmarks() {
        _bookmarks.value = StorageManager.getBookmarks(getApplication())
    }

    private fun checkServerHealth() {
        viewModelScope.launch {
            try {
                ApiClient.service.checkHealth()
                _isServerUnavailable.value = false
            } catch (e: Exception) {
                _isServerUnavailable.value = true
            }
        }
    }
}
