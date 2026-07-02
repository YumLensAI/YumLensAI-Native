package com.yumlensai.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.draw.blur
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.yumlensai.constants.ingredientDetails
import com.yumlensai.data.api.model.Bookmark
import com.yumlensai.ui.components.IngredientToggle
import com.yumlensai.ui.theme.BookmarkActive
import com.yumlensai.ui.theme.Disabled
import com.yumlensai.ui.theme.GrayText
import com.yumlensai.viewmodel.IngredientsViewModel
import com.google.gson.Gson

@Composable
fun IngredientsScreen(
    imageUri: String,
    isServerUnavailable: Boolean,
    preloadedIngredientsJson: String = "[]",
    initialBookmarkId: String? = null,
    onBack: () -> Unit,
    onNavigateToSuggestions: (List<String>) -> Unit,
    viewModel: IngredientsViewModel = viewModel()
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val ingredients by viewModel.ingredients.collectAsState()
    val toggles by viewModel.toggles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val bookmarkId by viewModel.bookmarkId.collectAsState()
    val isOfflineTipVisible by viewModel.isOfflineTipVisible.collectAsState()

    val uri = Uri.parse(imageUri)

    LaunchedEffect(Unit) {
        val preloaded = try {
            Gson().fromJson(preloadedIngredientsJson, Array<String>::class.java)?.toList()
        } catch (e: Exception) {
            null
        }

        if (!preloaded.isNullOrEmpty()) {
            viewModel.loadFromBookmark(
                Bookmark(
                    id = initialBookmarkId ?: "",
                    image = imageUri,
                    ingredients = preloaded
                )
            )
        } else if (isServerUnavailable) {
            viewModel.runLocalInference(uri)
        } else {
            viewModel.runServerInference(uri)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Blurred background
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(20.dp),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000))
        )

        // Content card
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Image preview (33% of height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight * 0.33f)
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Back button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x4D000000))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White
                    )
                }

                // Bookmark button
                IconButton(
                    onClick = {
                        if (bookmarkId != null) viewModel.removeBookmark()
                        else viewModel.saveBookmark(uri)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x4D000000))
                ) {
                    Icon(
                        imageVector = if (bookmarkId != null)
                            Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                        contentDescription = "Bookmark",
                        tint = if (bookmarkId != null) BookmarkActive else Color.White
                    )
                }
            }

            // White card
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.White)
            ) {
                if (isLoading) {
                    LoadingIngredients()
                } else {
                    // Title
                    Text(
                        text = "Ingredientes Detectados",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                    )

                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    if (ingredients.isEmpty()) {
                        EmptyIngredients()
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(ingredients) { key ->
                                val detail = ingredientDetails[key]
                                if (detail != null) {
                                    IngredientToggle(
                                        detail = detail,
                                        isSelected = toggles[key] ?: true,
                                        onSelect = { viewModel.toggleIngredient(key) }
                                    )
                                    HorizontalDivider(color = Color(0xFFEEEEEE))
                                }
                            }
                        }

                        // Calorie disclaimer
                        Text(
                            text = "Informações de calorias referentes a 100g",
                            fontSize = 12.sp,
                            color = GrayText,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    // Offline tip
                    if (isOfflineTipVisible) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF3CD))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Modo offline: a busca de receitas requer conexão com o servidor.",
                                fontSize = 13.sp,
                                color = Color(0xFF856404)
                            )
                        }
                    }

                    // Ver Receitas button
                    val selected = viewModel.getSelectedIngredients()
                    val buttonEnabled = selected.isNotEmpty()

                    Button(
                        onClick = {
                            if (isServerUnavailable) {
                                viewModel.showOfflineTip()
                            } else {
                                onNavigateToSuggestions(selected)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding(),
                        enabled = buttonEnabled,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            disabledContainerColor = Disabled
                        )
                    ) {
                        Text(
                            text = "Ver Receitas",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingIngredients() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("lotties/fruits.json")
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(200.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Identificando os ingredientes ...",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Em um passe de mágica.",
            fontSize = 13.sp,
            color = GrayText,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyIngredients() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(
            text = "Nenhum ingrediente detectado",
            fontSize = 16.sp,
            color = GrayText,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tente com uma imagem diferente.",
            fontSize = 13.sp,
            color = GrayText,
            textAlign = TextAlign.Center
        )
    }
}
