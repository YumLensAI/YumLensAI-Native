package com.yumlensai.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.yumlensai.R
import com.yumlensai.data.api.model.Bookmark
import com.yumlensai.ui.theme.CameraBlue
import com.yumlensai.ui.theme.GrayText
import com.yumlensai.ui.theme.HeaderPurple
import com.yumlensai.ui.theme.ImageOrangeRed
import com.yumlensai.ui.theme.InformativeYellow
import com.yumlensai.ui.theme.KiwiGreen
import com.yumlensai.ui.theme.LightGray
import com.yumlensai.ui.theme.StrawberryRed
import com.yumlensai.ui.theme.WarmBeige
import com.yumlensai.viewmodel.HomeViewModel
import java.io.File

@Composable
fun HomeScreen(
    onNavigateToIngredients: (String, Boolean) -> Unit,
    onNavigateToBookmark: (String, List<String>, String) -> Unit,
    onNavigateToBenchmark: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val bookmarks by viewModel.bookmarks.collectAsState()
    val isServerUnavailable by viewModel.isServerUnavailable.collectAsState()

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onNavigateToIngredients(it.toString(), isServerUnavailable) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.let { onNavigateToIngredients(it.toString(), isServerUnavailable) }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createCameraOutputUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val uri = createCameraOutputUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Box(modifier = Modifier.padding(vertical = 20.dp)) {
            Text(
                text = "YumLensAI",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column {
                Text(
                    text = "Como posso lhe ajudar hoje?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InferenceOptionCard(
                        modifier = Modifier.weight(1f),
                        iconBg = CameraBlue,
                        icon = Icons.Filled.CameraAlt,
                        title = "Capturar Foto",
                        description = "Tire uma foto dos ingredientes",
                        onClick = { launchCamera() }
                    )
                    InferenceOptionCard(
                        modifier = Modifier.weight(1f),
                        iconBg = ImageOrangeRed,
                        icon = Icons.Filled.Image,
                        title = "Galeria",
                        description = "Escolha uma imagem existente",
                        onClick = { galleryLauncher.launch("image/*") }
                    )
                }
            }

            FruitOfTheDayCard()

            if (bookmarks.isNotEmpty()) {
                Column {
                    Text(
                        text = "Capturas Salvas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(bookmarks, key = { it.id }) { bookmark ->
                            SavedCaptureItem(
                                bookmark = bookmark,
                                onClick = {
                                    onNavigateToBookmark(bookmark.image, bookmark.ingredients, bookmark.id)
                                }
                            )
                        }
                    }
                }
            }

            QuickTipCard()
        }
    }
}

private fun createCameraOutputUri(context: Context): Uri {
    val file = File.createTempFile("capture_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@Composable
private fun InferenceOptionCard(
    modifier: Modifier = Modifier,
    iconBg: Color,
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = description, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = GrayText)
        }
    }
}

@Composable
private fun FruitOfTheDayCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = InformativeYellow),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.strawberry_info),
                contentDescription = "Morango",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(92.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "Fruta do dia: Morango",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = KiwiGreen
                )
                Text(
                    text = "Rico em vitamina C e antioxidantes.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Justify,
                    color = StrawberryRed
                )
            }
        }
    }
}

@Composable
private fun SavedCaptureItem(bookmark: Bookmark, onClick: () -> Unit) {
    AsyncImage(
        model = bookmark.image,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .width(130.dp)
            .height(102.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(LightGray)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun QuickTipCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WarmBeige),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lightbulb,
                contentDescription = null,
                tint = HeaderPurple,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium)) {
                        append("Dica rápida: ")
                    }
                    append("Use boa iluminação para melhores resultados de detecção.")
                },
                fontSize = 14.sp,
                color = HeaderPurple,
                textAlign = TextAlign.Justify,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
