package com.f7developer.slimeverse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f7developer.slimeverse.game.Element
import com.f7developer.slimeverse.game.Rarity
import com.f7developer.slimeverse.ui.theme.LocalExtraColors

fun formatNum(n: Long): String = when {
    n >= 1_000_000_000 -> String.format("%.2fB", n / 1e9)
    n >= 1_000_000 -> String.format("%.2fM", n / 1e6)
    n >= 10_000 -> String.format("%.1fK", n / 1e3)
    else -> n.toString()
}

fun formatNum(n: Int): String = formatNum(n.toLong())

@Composable
fun GameCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    corner: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    var m = modifier
        .shadow(3.dp, shape, spotColor = Color.Black.copy(alpha = 0.10f))
        .clip(shape)
        .background(color)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(modifier = m.padding(16.dp), content = content)
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft)
        }
        action?.invoke()
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .shadow(if (enabled) 4.dp else 0.dp, shape, spotColor = color.copy(alpha = 0.4f))
            .clip(shape)
            .background(
                if (enabled) Brush.horizontalGradient(listOf(color, lightenColor(color, 0.12f)))
                else Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f)))
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text(text, color = if (enabled) Color.White else Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(tint.copy(alpha = 0.12f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (icon != null) Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Text(text, color = tint, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
        }
    }
}

fun lightenColor(col: Color, f: Float): Color = Color(
    red = (col.red + (1 - col.red) * f).coerceIn(0f, 1f),
    green = (col.green + (1 - col.green) * f).coerceIn(0f, 1f),
    blue = (col.blue + (1 - col.blue) * f).coerceIn(0f, 1f),
    alpha = col.alpha,
)

@Composable
fun ResourcePill(icon: ImageVector, value: String, tint: Color, label: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, LocalExtraColors.current.line, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(16.dp))
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ElementBadge(element: Element, small: Boolean = false) {
    val col = Color(element.color)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(col.copy(alpha = 0.16f))
            .padding(horizontal = if (small) 6.dp else 8.dp, vertical = if (small) 2.dp else 4.dp),
    ) {
        Text(element.displayName, color = col, fontWeight = FontWeight.Bold, fontSize = if (small) 10.5.sp else 12.sp)
    }
}

@Composable
fun RarityBadge(rarity: Rarity, small: Boolean = false) {
    val col = Color(rarity.color)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(col.copy(alpha = 0.16f))
            .padding(horizontal = if (small) 6.dp else 8.dp, vertical = if (small) 2.dp else 4.dp),
    ) {
        Text(rarity.displayName, color = col, fontWeight = FontWeight.Bold, fontSize = if (small) 10.5.sp else 12.sp)
    }
}

@Composable
fun StatChip(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 11.sp, color = LocalExtraColors.current.inkSoft, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun LinearProgress(progress: Float, tint: Color, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(LocalExtraColors.current.line),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(height / 2))
                .background(Brush.horizontalGradient(listOf(tint, lightenColor(tint, 0.2f)))),
        )
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = LocalExtraColors.current.inkFaint, modifier = Modifier.size(48.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft, textAlign = TextAlign.Center)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = LocalExtraColors.current.inkSoft) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = LocalExtraColors.current.inkSoft) }
        },
    )
}

@Composable
fun TraitChip(trait: com.f7developer.slimeverse.game.Trait) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(trait.displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}