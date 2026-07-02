package com.yumlensai.constants

import androidx.compose.ui.graphics.Color
import com.yumlensai.R

data class IngredientDetail(
    val name: String,
    val calories: Int,
    val category: String,
    val color: Color,
    val imageRes: Int
)

val ingredientDetails: Map<String, IngredientDetail> = mapOf(
    "apple" to IngredientDetail(
        name = "Maçã",
        calories = 52,
        category = "Fruta",
        color = Color(0xFFFF6347),
        imageRes = R.drawable.ingredient_apple
    ),
    "banana" to IngredientDetail(
        name = "Banana",
        calories = 89,
        category = "Fruta",
        color = Color(0xFFFFEB3B),
        imageRes = R.drawable.ingredient_banana
    ),
    "kiwi" to IngredientDetail(
        name = "Kiwi",
        calories = 61,
        category = "Fruta",
        color = Color(0xFF8BC34A),
        imageRes = R.drawable.ingredient_kiwi
    ),
    "lemon" to IngredientDetail(
        name = "Limão",
        calories = 29,
        category = "Fruta",
        color = Color(0xFFFFEB3B),
        imageRes = R.drawable.ingredient_lemon
    ),
    "orange" to IngredientDetail(
        name = "Laranja",
        calories = 47,
        category = "Fruta",
        color = Color(0xFFFFA726),
        imageRes = R.drawable.ingredient_orange
    ),
    "strawberry" to IngredientDetail(
        name = "Morango",
        calories = 33,
        category = "Fruta",
        color = Color(0xFFFF1744),
        imageRes = R.drawable.ingredient_strawberry
    )
)
