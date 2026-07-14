package ai.opencode.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.opencode.android.ui.theme.TuiColors
import ai.opencode.android.ui.theme.TuiFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Surface(color = TuiColors.Background) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TuiColors.OnSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = "settings",
                        color = TuiColors.OnBackground,
                        fontSize = 13.sp,
                        fontFamily = TuiFont.Mono,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        },
        containerColor = TuiColors.Background,
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Connection info
            item {
                SectionHeader(title = "connection")
                Spacer(modifier = Modifier.height(4.dp))
                InfoCard(
                    label = "server",
                    value = uiState.serverUrl ?: "embedded (localhost:4096)",
                    isConnected = uiState.isConnected,
                )
            }

            // Free Models Section
            item {
                SectionHeader(title = "free models (no API key needed)")
                Spacer(modifier = Modifier.height(4.dp))
                FreeModelCard(
                    name = "Big Pickle",
                    provider = "Groq",
                    description = "Llama 3.3 70B - modelo del equipo",
                    isSelected = uiState.selectedModel == "opencode/big-pickle",
                    onClick = { viewModel.selectModel("opencode/big-pickle") }
                )
            }

            item {
                FreeModelCard(
                    name = "Llama 3.3 70B",
                    provider = "Groq",
                    description = "Modelo principal gratuito",
                    isSelected = uiState.selectedModel == "groq/llama-3.3-70b-versatile",
                    onClick = { viewModel.selectModel("groq/llama-3.3-70b-versatile") }
                )
            }

            item {
                FreeModelCard(
                    name = "Gemini 2.5 Flash",
                    provider = "Google",
                    description = "Modelo rápido de Google",
                    isSelected = uiState.selectedModel == "gemini/gemini-2.5-flash",
                    onClick = { viewModel.selectModel("gemini/gemini-2.5-flash") }
                )
            }

            item {
                FreeModelCard(
                    name = "DeepSeek R1 (temp)",
                    provider = "OpenCode",
                    description = "Modelo temporal semanal",
                    isSelected = uiState.selectedModel == "opencode/temp/deepseek-r1",
                    onClick = { viewModel.selectModel("opencode/temp/deepseek-r1") }
                )
            }

            // Paid API Keys
            item {
                SectionHeader(title = "paid api keys (optional)")
                Spacer(modifier = Modifier.height(4.dp))
                ApiKeyField(
                    label = "groq",
                    key = uiState.groqKey,
                    onKeyChange = { viewModel.updateGroqKey(it) },
                    isConfigured = uiState.groqKey.isNotBlank(),
                )
            }

            item {
                ApiKeyField(
                    label = "gemini",
                    key = uiState.geminiKey,
                    onKeyChange = { viewModel.updateGeminiKey(it) },
                    isConfigured = uiState.geminiKey.isNotBlank(),
                )
            }

            item {
                ApiKeyField(
                    label = "openai",
                    key = uiState.openaiKey,
                    onKeyChange = { viewModel.updateOpenaiKey(it) },
                    isConfigured = uiState.openaiKey.isNotBlank(),
                )
            }

            item {
                ApiKeyField(
                    label = "anthropic",
                    key = uiState.anthropicKey,
                    onKeyChange = { viewModel.updateAnthropicKey(it) },
                    isConfigured = uiState.anthropicKey.isNotBlank(),
                )
            }

            item {
                ApiKeyField(
                    label = "openrouter",
                    key = uiState.openrouterKey,
                    onKeyChange = { viewModel.updateOpenrouterKey(it) },
                    isConfigured = uiState.openrouterKey.isNotBlank(),
                )
            }

            uiState.error?.let { error ->
                item {
                    Text(
                        text = "error: $error",
                        color = TuiColors.TerminalRed,
                        fontSize = 11.sp,
                        fontFamily = TuiFont.Mono,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = "# $title",
        color = TuiColors.TerminalGreen,
        fontSize = 12.sp,
        fontFamily = TuiFont.Mono,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun InfoCard(
    label: String,
    value: String,
    isConnected: Boolean = false,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TuiColors.Surface),
        border = BorderStroke(1.dp, TuiColors.CodeBorder),
        shape = RoundedCornerShape(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = TuiColors.OnSurface,
                fontSize = 11.sp,
                fontFamily = TuiFont.Mono,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isConnected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = TuiColors.TerminalGreen,
                        modifier = Modifier.size(10.dp),
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                }
                Text(
                    text = value,
                    color = if (isConnected) TuiColors.TerminalGreen else TuiColors.OnBackground,
                    fontSize = 11.sp,
                    fontFamily = TuiFont.Mono,
                )
            }
        }
    }
}

@Composable
private fun FreeModelCard(
    name: String,
    provider: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) TuiColors.TerminalGreen.copy(alpha = 0.1f) else TuiColors.Surface
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) TuiColors.TerminalGreen else TuiColors.CodeBorder
        ),
        shape = RoundedCornerShape(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        color = TuiColors.OnBackground,
                        fontSize = 12.sp,
                        fontFamily = TuiFont.Mono,
                        fontWeight = FontWeight.Medium,
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.size(4.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = TuiColors.TerminalGreen,
                            modifier = Modifier.size(10.dp),
                        )
                    }
                }
                Text(
                    text = "$provider - $description",
                    color = TuiColors.OnSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = TuiFont.Mono,
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Active",
                    tint = TuiColors.TerminalGreen,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun ProviderCard(
    name: String,
    modelCount: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TuiColors.Surface),
        border = BorderStroke(1.dp, TuiColors.CodeBorder),
        shape = RoundedCornerShape(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                color = TuiColors.OnBackground,
                fontSize = 12.sp,
                fontFamily = TuiFont.Mono,
            )
            Text(
                text = "$modelCount models",
                color = TuiColors.OnSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = TuiFont.Mono,
            )
        }
    }
}

@Composable
private fun ApiKeyField(
    label: String,
    key: String,
    onKeyChange: (String) -> Unit,
    isConfigured: Boolean,
) {
    var showKey by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TuiColors.Surface),
        border = BorderStroke(1.dp, TuiColors.CodeBorder),
        shape = RoundedCornerShape(2.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    color = TuiColors.OnSurface,
                    fontSize = 11.sp,
                    fontFamily = TuiFont.Mono,
                )
                if (isConfigured) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Configured",
                        tint = TuiColors.TerminalGreen,
                        modifier = Modifier.size(10.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = key,
                onValueChange = onKeyChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "sk-...",
                        color = TuiColors.OnSurfaceVariant,
                        fontSize = 11.sp,
                        fontFamily = TuiFont.Mono,
                    )
                },
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showKey) "Hide" else "Show",
                            tint = TuiColors.OnSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TuiColors.TerminalGreen,
                    unfocusedBorderColor = TuiColors.CodeBorder,
                    cursorColor = TuiColors.TerminalGreen,
                    focusedTextColor = TuiColors.OnBackground,
                    unfocusedTextColor = TuiColors.OnBackground,
                ),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 11.sp,
                    fontFamily = TuiFont.Mono,
                ),
            )
        }
    }
}
