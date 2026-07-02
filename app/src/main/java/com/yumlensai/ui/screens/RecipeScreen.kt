package com.yumlensai.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.yumlensai.R
import com.yumlensai.data.api.model.RecipeObject
import com.yumlensai.ui.components.IngredientsTab
import com.yumlensai.ui.components.InstructionsTab
import kotlinx.coroutines.launch

@Composable
fun RecipeScreen(
    recipe: RecipeObject,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Light status bar icons over the dark hero image, restored on dispose
    // so Home/Ingredients keep their dark-icons-on-white appearance.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val activity = view.context as? Activity
        val previous = activity?.let {
            WindowCompat.getInsetsController(it.window, view).isAppearanceLightStatusBars
        }
        activity?.let {
            WindowCompat.getInsetsController(it.window, view).isAppearanceLightStatusBars = false
        }
        onDispose {
            if (activity != null && previous != null) {
                WindowCompat.getInsetsController(activity.window, view)
                    .isAppearanceLightStatusBars = previous
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        RecipeHero(recipe = recipe, onBack = onBack)
        RecipeTabs(recipe = recipe)
    }
}

@Composable
private fun RecipeHero(recipe: RecipeObject, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.recipe_placeholder),
            contentDescription = recipe.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x33000000), Color(0xB3000000))
                    )
                ),
            verticalArrangement = Arrangement.Bottom
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 16.dp)
            ) {
                Text(
                    text = recipe.title,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = recipe.description,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 16.dp, y = 30.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xB3000000))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun RecipeTabs(recipe: RecipeObject) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    TabRow(
        selectedTabIndex = pagerState.currentPage,
        containerColor = Color.White,
        contentColor = Color.Black,
        indicator = { positions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(positions[pagerState.currentPage]),
                color = Color.Black
            )
        }
    ) {
        val titles = listOf("Ingredientes", "Instruções")
        titles.forEachIndexed { index, title ->
            val selected = pagerState.currentPage == index
            Tab(
                selected = selected,
                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                text = {
                    Text(
                        text = title,
                        color = if (selected) Color.Black else Color.Gray
                    )
                }
            )
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> IngredientsTab(ingredients = recipe.ingredients)
            1 -> InstructionsTab(steps = recipe.steps)
        }
    }
}
