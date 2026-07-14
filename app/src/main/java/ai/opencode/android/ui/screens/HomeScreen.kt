package ai.opencode.android.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.opencode.android.R
import ai.opencode.android.ui.theme.TuiColors
import ai.opencode.android.ui.theme.TuiFont

@Composable
fun HomeScreen(
    onNewChat: () -> Unit,
    onSessions: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(800))
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = TuiColors.Background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.logo_opencode),
                contentDescription = "opencode",
                modifier = Modifier
                    .size(120.dp)
                    .alpha(alpha.value),
                contentScale = ContentScale.Fit,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "opencode",
                color = TuiColors.TerminalGreen,
                fontSize = 28.sp,
                fontFamily = TuiFont.Mono,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(alpha.value),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "ai coding assistant",
                color = TuiColors.OnSurfaceVariant,
                fontSize = 12.sp,
                fontFamily = TuiFont.Mono,
                modifier = Modifier.alpha(alpha.value),
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Menu items
            MenuButton(
                icon = Icons.Default.Add,
                label = "new chat",
                description = "start a conversation",
                onClick = onNewChat,
                alpha = alpha.value,
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuButton(
                icon = Icons.Default.List,
                label = "sessions",
                description = "view past conversations",
                onClick = onSessions,
                alpha = alpha.value,
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuButton(
                icon = Icons.Default.Settings,
                label = "settings",
                description = "configure api keys & models",
                onClick = onSettings,
                alpha = alpha.value,
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Version
            Text(
                text = "v1.0.0",
                color = TuiColors.OnSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontFamily = TuiFont.Mono,
                modifier = Modifier.alpha(alpha.value),
            )
        }
    }
}

@Composable
private fun MenuButton(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, TuiColors.Divider, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TuiColors.TerminalGreen,
            modifier = Modifier.size(20.dp),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = TuiColors.OnBackground,
                fontSize = 14.sp,
                fontFamily = TuiFont.Mono,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = description,
                color = TuiColors.OnSurfaceVariant,
                fontSize = 11.sp,
                fontFamily = TuiFont.Mono,
            )
        }

        Text(
            text = ">",
            color = TuiColors.TerminalGreen,
            fontSize = 14.sp,
            fontFamily = TuiFont.Mono,
        )
    }
}
