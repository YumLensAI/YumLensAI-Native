package com.yumlensai.data.api.model

data class RecipeObject(
    val title: String = "",
    val category: String = "",
    val description: String = "",
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val image: String = "",
    val time: String = ""
)
