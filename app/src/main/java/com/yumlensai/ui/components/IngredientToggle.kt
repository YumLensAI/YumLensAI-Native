package com.yumlensai.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yumlensai.constants.IngredientDetail
import com.yumlensai.ui.theme.GrayText

@Composable
fun IngredientToggle(
    detail: IngredientDetail,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fillScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.92f,
        animationSpec = tween(durationMillis = 180),
        label = "ingredient-toggle-scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onSelect)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(fillScale)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) detail.color else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(detail.imageRes),
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = detail.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color.Black else GrayText
                )
                CaloriesChip(calories = detail.calories, isActive = isSelected)
            }
            Text(
                text = detail.category,
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                color = if (isSelected) Color.Black else GrayText
            )
        }
    }
}

@Composable
private fun CaloriesChip(calories: Int, isActive: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Color(0xFFFFE9E8) else Color(0xFFEEEEEE))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.LocalFireDepartment,
            contentDescription = null,
            tint = if (isActive) Color(0xFFFF4500) else GrayText,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "$calories calorias",
            fontSize = 12.sp,
            color = if (isActive) Color(0xFFFF4500) else GrayText
        )
    }
}
