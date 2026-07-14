package ai.opencode.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.opencode.android.ui.theme.TuiColors
import ai.opencode.android.ui.theme.TuiFont

@Composable
fun PermissionDialog(
    toolName: String,
    description: String?,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDeny,
        containerColor = TuiColors.Surface,
        titleContentColor = TuiColors.TerminalAmber,
        textContentColor = TuiColors.OnBackground,
        title = {
            Text(
                text = "permission request",
                fontFamily = TuiFont.Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        },
        text = {
            Column {
                Text(
                    text = "tool: $toolName",
                    fontFamily = TuiFont.Mono,
                    fontSize = 12.sp,
                    color = TuiColors.TerminalCyan,
                )
                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = description,
                        fontFamily = TuiFont.Mono,
                        fontSize = 12.sp,
                        color = TuiColors.OnSurface,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onApprove,
            ) {
                Text(
                    text = "> approve",
                    fontFamily = TuiFont.Mono,
                    color = TuiColors.TerminalGreen,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDeny,
            ) {
                Text(
                    text = "> deny",
                    fontFamily = TuiFont.Mono,
                    color = TuiColors.TerminalRed,
                )
            }
        },
    )
}
