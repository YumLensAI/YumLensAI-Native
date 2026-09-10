package com.yumlensai.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.yumlensai.data.api.model.RecipeObject
import com.yumlensai.ui.screens.BenchmarkScreen
import com.yumlensai.ui.screens.HomeScreen
import com.yumlensai.ui.screens.IngredientsScreen
import com.yumlensai.ui.screens.RecipeScreen
import com.yumlensai.ui.screens.SuggestionsScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val BENCHMARK = "benchmark"
    const val HOME = "home"
    const val INGREDIENTS = "ingredients/{imageUri}/{isServerUnavailable}/{ingredientsJson}/{bookmarkId}"
    const val SUGGESTIONS = "suggestions/{ingredientsJson}"
    const val RECIPE = "recipe/{recipeJson}"

    fun ingredients(
        imageUri: String,
        isServerUnavailable: Boolean = false,
        ingredientsJson: String = "[]",
        bookmarkId: String = ""
    ): String {
        val encodedUri = URLEncoder.encode(imageUri, "UTF-8")
        val encodedIngredients = URLEncoder.encode(ingredientsJson, "UTF-8")
        return "ingredients/$encodedUri/$isServerUnavailable/$encodedIngredients/$bookmarkId"
    }

    fun suggestions(ingredients: List<String>): String {
        val json = URLEncoder.encode(Gson().toJson(ingredients), "UTF-8")
        return "suggestions/$json"
    }

    fun recipe(item: RecipeObject): String {
        val json = URLEncoder.encode(Gson().toJson(item), "UTF-8")
        return "recipe/$json"
    }
}

@Composable
fun YumLensAINavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.BENCHMARK) {
            BenchmarkScreen(
                onNavigateToHome = { navController.popBackStack() }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToIngredients = { uri, isOffline ->
                    navController.navigate(Routes.ingredients(uri, isOffline))
                },
                onNavigateToBookmark = { uri, ingredients, bookmarkId ->
                    val json = Gson().toJson(ingredients)
                    navController.navigate(Routes.ingredients(uri, false, json, bookmarkId))
                },
                onNavigateToBenchmark = { navController.navigate(Routes.BENCHMARK) }
            )
        }

        composable(
            route = Routes.INGREDIENTS,
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType },
                navArgument("isServerUnavailable") { type = NavType.BoolType },
                navArgument("ingredientsJson") { type = NavType.StringType },
                navArgument("bookmarkId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val imageUri = URLDecoder.decode(
                backStackEntry.arguments?.getString("imageUri") ?: "", "UTF-8"
            )
            val isServerUnavailable = backStackEntry.arguments?.getBoolean("isServerUnavailable") ?: false
            val ingredientsJson = URLDecoder.decode(
                backStackEntry.arguments?.getString("ingredientsJson") ?: "[]", "UTF-8"
            )
            val bookmarkId = backStackEntry.arguments?.getString("bookmarkId") ?: ""

            IngredientsScreen(
                imageUri = imageUri,
                isServerUnavailable = isServerUnavailable,
                preloadedIngredientsJson = ingredientsJson,
                initialBookmarkId = bookmarkId.ifEmpty { null },
                onBack = { navController.popBackStack() },
                onNavigateToSuggestions = { ingredients ->
                    navController.navigate(Routes.suggestions(ingredients))
                }
            )
        }

        composable(
            route = Routes.SUGGESTIONS,
            arguments = listOf(navArgument("ingredientsJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val json = URLDecoder.decode(
                backStackEntry.arguments?.getString("ingredientsJson") ?: "[]", "UTF-8"
            )
            val ingredients = try {
                Gson().fromJson(json, Array<String>::class.java).toList()
            } catch (e: Exception) {
                emptyList()
            }

            SuggestionsScreen(
                ingredients = ingredients,
                onBack = { navController.popBackStack() },
                onNavigateToRecipe = { recipe ->
                    navController.navigate(Routes.recipe(recipe))
                }
            )
        }

        composable(
            route = Routes.RECIPE,
            arguments = listOf(navArgument("recipeJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val json = URLDecoder.decode(
                backStackEntry.arguments?.getString("recipeJson") ?: "{}", "UTF-8"
            )
            val recipe = try {
                Gson().fromJson(json, RecipeObject::class.java)
            } catch (e: Exception) {
                RecipeObject()
            }

            RecipeScreen(
                recipe = recipe,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
