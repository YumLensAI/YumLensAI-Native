package com.yumlensai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yumlensai.data.api.ApiClient
import com.yumlensai.data.api.model.RecipeObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SuggestionsViewModel : ViewModel() {

    private val _recipes = MutableStateFlow<List<RecipeObject>>(emptyList())
    val recipes: StateFlow<List<RecipeObject>> = _recipes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun fetchRecipes(ingredients: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _recipes.value = ApiClient.service.searchRecipes(ingredients)
            } catch (e: Exception) {
                _recipes.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
