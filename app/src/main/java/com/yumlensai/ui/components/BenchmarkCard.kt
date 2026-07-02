package com.yumlensai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.yumlensai.viewmodel.BenchmarkState
import com.yumlensai.viewmodel.BenchmarkStatus

data class BenchmarkCardColors(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val subtitleColor: Color = Color.Black,
)

@Composable
fun BenchmarkCard(
    title: String,
    state: BenchmarkState,
    colors: BenchmarkCardColors,
    onExecute: () -> Unit
) {
    val statusText = getStatusText(state.status, title.contains("Local"))
    val isButtonEnabled = state.status == BenchmarkStatus.READY ||
            state.status == BenchmarkStatus.COMPLETED ||
            state.status == BenchmarkStatus.FAILED

    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("lotties/processing.json")
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = state.status == BenchmarkStatus.EXECUTING,
        speed = 1.5f
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(0.75f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    color = colors.subtitleColor
                )
                Spacer(Modifier.height(8.dp))
                if (state.status == BenchmarkStatus.EXECUTING || state.status == BenchmarkStatus.SAVING) {
                    Text(
                        text = "${state.step} / ${state.total}",
                        fontSize = 13.sp,
                        color = colors.primary
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Button(
                    onClick = onExecute,
                    enabled = isButtonEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        disabledContainerColor = colors.secondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Executar",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(0.25f)
                    .size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(72.dp)
                )
            }
        }
    }
}

private fun getStatusText(status: BenchmarkStatus, isLocal: Boolean): String = when (status) {
    BenchmarkStatus.LOADING -> if (isLocal) "Iniciando Modelo ..." else "Verificando Servidor"
    BenchmarkStatus.READY -> "Pronto para Executar"
    BenchmarkStatus.INDISPONIBLE -> if (isLocal) "Falha ao Iniciar Modelo" else "Falha na Comunicação com Servidor"
    BenchmarkStatus.EXECUTING -> "Executando"
    BenchmarkStatus.SAVING -> "Salvando Benchmark"
    BenchmarkStatus.COMPLETED -> "Concluído"
    BenchmarkStatus.FAILED -> "Ocorreu uma Falha"
}
