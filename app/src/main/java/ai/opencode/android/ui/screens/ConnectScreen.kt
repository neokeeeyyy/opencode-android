package ai.opencode.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.opencode.android.ui.theme.TuiColors
import ai.opencode.android.ui.theme.TuiFont

@Composable
fun ConnectScreen(
    onConnected: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConnectViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.isConnected) {
        if (uiState.isConnected) {
            onConnected()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "opencode",
            fontSize = 28.sp,
            fontFamily = TuiFont.Mono,
            fontWeight = FontWeight.Bold,
            color = TuiColors.TerminalGreen,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "android client",
            fontSize = 12.sp,
            fontFamily = TuiFont.Mono,
            color = TuiColors.OnSurface,
        )

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = uiState.serverUrl,
            onValueChange = { viewModel.updateUrl(it) },
            label = {
                Text(
                    "server url",
                    fontFamily = TuiFont.Mono,
                    fontSize = 12.sp,
                )
            },
            placeholder = {
                Text(
                    "http://192.168.1.100:4096",
                    fontFamily = TuiFont.Mono,
                    fontSize = 12.sp,
                    color = TuiColors.OnSurfaceVariant,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go,
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    focusManager.clearFocus()
                    viewModel.connect()
                },
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TuiColors.TerminalGreen,
                unfocusedBorderColor = TuiColors.CodeBorder,
                focusedTextColor = TuiColors.OnBackground,
                unfocusedTextColor = TuiColors.OnBackground,
                cursorColor = TuiColors.TerminalGreen,
                focusedLabelColor = TuiColors.TerminalGreen,
                unfocusedLabelColor = TuiColors.OnSurface,
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = TuiFont.Mono,
                fontSize = 13.sp,
            ),
        )

        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "error: $error",
                color = TuiColors.TerminalRed,
                fontSize = 11.sp,
                fontFamily = TuiFont.Mono,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                viewModel.connect()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isConnecting && uiState.serverUrl.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = TuiColors.TerminalGreen,
                contentColor = TuiColors.Background,
                disabledContainerColor = TuiColors.TerminalGreen.copy(alpha = 0.3f),
                disabledContentColor = TuiColors.Background.copy(alpha = 0.5f),
            ),
            shape = RoundedCornerShape(2.dp),
        ) {
            if (uiState.isConnecting) {
                CircularProgressIndicator(
                    color = TuiColors.Background,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .height(16.dp)
                        .fillMaxWidth(0.05f),
                )
            } else {
                Text(
                    text = "> connect",
                    fontFamily = TuiFont.Mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TuiColors.Surface),
            border = BorderStroke(1.dp, TuiColors.CodeBorder),
            shape = RoundedCornerShape(2.dp),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "# setup",
                    color = TuiColors.TerminalGreen,
                    fontSize = 11.sp,
                    fontFamily = TuiFont.Mono,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. run opencode serve on your server",
                    color = TuiColors.OnSurface,
                    fontSize = 11.sp,
                    fontFamily = TuiFont.Mono,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "2. ensure port 4096 is accessible",
                    color = TuiColors.OnSurface,
                    fontSize = 11.sp,
                    fontFamily = TuiFont.Mono,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "3. enter the server url above",
                    color = TuiColors.OnSurface,
                    fontSize = 11.sp,
                    fontFamily = TuiFont.Mono,
                )
            }
        }
    }
}
