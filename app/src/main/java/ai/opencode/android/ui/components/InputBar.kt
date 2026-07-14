package ai.opencode.android.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.opencode.android.ui.theme.TuiColors
import ai.opencode.android.ui.theme.TuiFont

@Composable
fun InputBar(
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "type a message...",
) {
    var text by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TuiColors.Surface,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = ">",
                color = if (enabled) TuiColors.TerminalGreen else TuiColors.OnSurface.copy(alpha = 0.3f),
                fontSize = 16.sp,
                fontFamily = TuiFont.Mono,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = placeholder,
                        color = TuiColors.OnSurface.copy(alpha = 0.4f),
                        fontFamily = TuiFont.Mono,
                        fontSize = 13.sp,
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = TuiColors.OnBackground,
                    unfocusedTextColor = TuiColors.OnBackground,
                    cursorColor = TuiColors.TerminalGreen,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (text.isNotBlank() && enabled) {
                            onSend(text.trim())
                            text = ""
                        }
                    },
                ),
                maxLines = 5,
                textStyle = TextStyle(
                    fontFamily = TuiFont.Mono,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                ),
                enabled = enabled,
            )

            IconButton(
                onClick = {
                    if (text.isNotBlank() && enabled) {
                        onSend(text.trim())
                        text = ""
                    }
                },
                enabled = enabled && text.isNotBlank(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (enabled && text.isNotBlank())
                        TuiColors.TerminalGreen
                    else TuiColors.OnSurface.copy(alpha = 0.2f),
                )
            }
        }
    }
}
