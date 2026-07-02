package com.yumlensai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yumlensai.ui.components.BenchmarkCard
import com.yumlensai.ui.components.BenchmarkCardColors
import com.yumlensai.ui.theme.BackendBackground
import com.yumlensai.ui.theme.BackendPrimary
import com.yumlensai.ui.theme.BackendSecondary
import com.yumlensai.ui.theme.DeviceInfoBg
import com.yumlensai.ui.theme.GrayText
import com.yumlensai.ui.theme.HeaderPurple
import com.yumlensai.ui.theme.LocalBackground
import com.yumlensai.ui.theme.LocalPrimary
import com.yumlensai.ui.theme.LocalSecondary
import com.yumlensai.viewmodel.BenchmarkViewModel

@Composable
fun BenchmarkScreen(
    onNavigateToHome: () -> Unit,
    viewModel: BenchmarkViewModel = viewModel()
) {
    val localBenchmark by viewModel.localBenchmark.collectAsState()
    val backendBenchmark by viewModel.backendBenchmark.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val deviceModel by viewModel.deviceModel.collectAsState()
    val osVersion by viewModel.osVersion.collectAsState()
    val ramMemory by viewModel.ramMemory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "YumLensAI Benchmark",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = HeaderPurple
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = HeaderPurple
                    )
                }
                IconButton(onClick = { viewModel.triggerSync() }) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sincronizar",
                        tint = if (isSyncing) HeaderPurple else GrayText
                    )
                }
            }
        }

        // Navigate to home
        TextButton(
            onClick = onNavigateToHome,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(text = "Ir para o App →", color = HeaderPurple)
        }

        Spacer(Modifier.height(8.dp))

        // Device info card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DeviceInfoBg)
                .padding(16.dp)
        ) {
            Text(
                text = "Informações do Dispositivo",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            DeviceInfoRow(label = "Modelo", value = deviceModel)
            DeviceInfoRow(label = "Sistema", value = osVersion)
            DeviceInfoRow(label = "Memória RAM", value = ramMemory)
        }

        Spacer(Modifier.height(20.dp))

        // Local benchmark card
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Processamento Local",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            BenchmarkCard(
                title = "Processamento local",
                state = localBenchmark,
                colors = BenchmarkCardColors(
                    primary = LocalPrimary,
                    secondary = LocalSecondary,
                    background = LocalBackground
                ),
                onExecute = { viewModel.executeLocalBenchmark() }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Backend benchmark card
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Processamento Backend",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            BenchmarkCard(
                title = "Processamento Backend",
                state = backendBenchmark,
                colors = BenchmarkCardColors(
                    primary = BackendPrimary,
                    secondary = BackendSecondary,
                    background = BackendBackground
                ),
                onExecute = { viewModel.executeBackendBenchmark() }
            )
        }
    }
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = GrayText
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
