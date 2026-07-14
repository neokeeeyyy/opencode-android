package ai.opencode.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.opencode.android.data.model.AssistantMessage
import ai.opencode.android.data.model.Message
import ai.opencode.android.data.model.Part
import ai.opencode.android.data.model.ToolState
import ai.opencode.android.data.model.UserMessage
import ai.opencode.android.ui.theme.TuiColors
import ai.opencode.android.ui.theme.TuiFont

@Composable
fun MessageBubble(
    role: String,
    content: String,
    modifier: Modifier = Modifier,
) {
    val isUser = role == "user"
    val accentColor = if (isUser) TuiColors.TerminalAmber else TuiColors.TerminalGreen
    val bubbleColor = accentColor.copy(alpha = 0.05f)
    val prefix = if (isUser) "> " else "< "

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = bubbleColor),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(2.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "${prefix}${if (isUser) "you" else "opencode"}",
                color = accentColor,
                fontSize = 10.sp,
                fontFamily = TuiFont.Mono,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = content,
                color = TuiColors.OnBackground,
                fontSize = 13.sp,
                fontFamily = TuiFont.Mono,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
fun ToolIndicator(
    toolName: String,
    state: ToolState,
    modifier: Modifier = Modifier,
) {
    val (color, icon, stateText) = when (state) {
        is ToolState.Pending -> Triple(TuiColors.OnSurfaceVariant, Icons.Default.Refresh, "pending")
        is ToolState.Running -> Triple(TuiColors.TerminalCyan, Icons.Default.Refresh, "running")
        is ToolState.Completed -> Triple(TuiColors.TerminalGreen, Icons.Default.CheckCircle, "done")
        is ToolState.Error -> Triple(TuiColors.TerminalRed, Icons.Default.Error, "error")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(2.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = toolName,
                color = color,
                fontSize = 11.sp,
                fontFamily = TuiFont.Mono,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stateText,
                color = color.copy(alpha = 0.7f),
                fontSize = 9.sp,
                fontFamily = TuiFont.Mono,
            )
        }

        // Show output for completed tools
        if (state is ToolState.Completed && state.output.isNotBlank()) {
            Text(
                text = state.output.take(500),
                color = TuiColors.OnSurface,
                fontSize = 10.sp,
                fontFamily = TuiFont.Mono,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                maxLines = 10,
            )
        }

        // Show error for failed tools
        if (state is ToolState.Error) {
            Text(
                text = state.error,
                color = TuiColors.TerminalRed,
                fontSize = 10.sp,
                fontFamily = TuiFont.Mono,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun StreamingIndicator(
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = TuiColors.TerminalGreen.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, TuiColors.TerminalGreen.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(2.dp),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = TuiColors.TerminalGreen,
                modifier = Modifier.size(12.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "thinking...",
                color = TuiColors.TerminalGreen,
                fontSize = 10.sp,
                fontFamily = TuiFont.Mono,
            )
        }
    }
}
