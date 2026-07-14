package ai.opencode.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.opencode.android.data.api.ConnectionState
import ai.opencode.android.data.model.AssistantMessage
import ai.opencode.android.data.model.Message
import ai.opencode.android.data.model.Part
import ai.opencode.android.data.model.ToolState
import ai.opencode.android.data.model.UserMessage
import ai.opencode.android.ui.components.InputBar
import ai.opencode.android.ui.components.MessageBubble
import ai.opencode.android.ui.components.PermissionDialog
import ai.opencode.android.ui.components.SessionCard
import ai.opencode.android.ui.components.StatusBar
import ai.opencode.android.ui.components.StreamingIndicator
import ai.opencode.android.ui.components.ToolIndicator
import ai.opencode.android.ui.theme.TuiColors
import ai.opencode.android.ui.theme.TuiFont
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val totalItems = uiState.messages.size + if (uiState.streamingText != null) 1 else 0

    LaunchedEffect(totalItems, uiState.streamingText) {
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                containerColor = TuiColors.Background,
                drawerContainerColor = TuiColors.Background,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        color = TuiColors.StatusBar,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "sessions",
                            modifier = Modifier.padding(16.dp),
                            color = TuiColors.TerminalGreen,
                            fontSize = 14.sp,
                            fontFamily = TuiFont.Mono,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    if (uiState.sessions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "no sessions yet",
                                color = TuiColors.OnSurfaceVariant,
                                fontSize = 12.sp,
                                fontFamily = TuiFont.Mono,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(uiState.sessions, key = { it.id }) { session ->
                                SessionCard(
                                    session = session,
                                    isSelected = session.id == uiState.currentSessionId,
                                    onClick = {
                                        viewModel.selectSession(session.id)
                                        scope.launch { drawerState.close() }
                                    },
                                    onDelete = { viewModel.deleteSession(session.id) },
                                )
                            }
                        }
                    }

                    Surface(
                        color = TuiColors.Surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    viewModel.createSession()
                                    scope.launch { drawerState.close() }
                                },
                                containerColor = TuiColors.TerminalGreen,
                                contentColor = TuiColors.Background,
                                modifier = Modifier.size(36.dp),
                                shape = androidx.compose.material3.MaterialTheme.shapes.small,
                                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "New Session",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                Surface(color = TuiColors.Background) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open()
                                else drawerState.close()
                            }
                        }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Sessions",
                                tint = TuiColors.OnSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Text(
                            text = uiState.currentSession?.title ?: "opencode",
                            color = TuiColors.OnBackground,
                            fontSize = 13.sp,
                            fontFamily = TuiFont.Mono,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        )

                        // Abort button when busy
                        if (uiState.isBusy || uiState.isSending) {
                            IconButton(onClick = { viewModel.abortSession() }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Stop",
                                    tint = TuiColors.TerminalRed,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }

                        IconButton(onClick = onSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TuiColors.OnSurface,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            },
            bottomBar = {
                StatusBar(
                    connectionState = uiState.connectionState,
                    currentModel = uiState.currentModel,
                    currentAgent = uiState.currentAgent,
                    sessionStatus = uiState.sessionStatus,
                    currentSessionTitle = uiState.currentSession?.title,
                )
            },
            containerColor = TuiColors.Background,
        ) { padding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when {
                    uiState.connectionState != ConnectionState.Connected -> {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (uiState.connectionState == ConnectionState.Connecting)
                                        "connecting..."
                                    else
                                        "disconnected",
                                    color = TuiColors.OnSurface,
                                    fontSize = 13.sp,
                                    fontFamily = TuiFont.Mono,
                                )
                            }
                        }
                    }

                    uiState.messages.isEmpty() && uiState.currentSessionId == null -> {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "opencode",
                                    color = TuiColors.TerminalGreen,
                                    fontSize = 20.sp,
                                    fontFamily = TuiFont.Mono,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "open the drawer to start",
                                    color = TuiColors.OnSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontFamily = TuiFont.Mono,
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            items(uiState.messages, key = { it.id }) { message ->
                                MessageContent(message = message)
                            }

                            // Streaming text indicator
                            if (uiState.streamingText != null) {
                                item(key = "streaming") {
                                    if (uiState.streamingText!!.isNotEmpty()) {
                                        MessageBubble(
                                            role = "assistant",
                                            content = uiState.streamingText!!,
                                        )
                                    } else {
                                        StreamingIndicator()
                                    }
                                }
                            }
                        }
                    }
                }

                InputBar(
                    onSend = { viewModel.sendMessage(it) },
                    enabled = uiState.canSend,
                )
            }
        }
    }

    uiState.pendingPermissionId?.let {
        PermissionDialog(
            toolName = uiState.pendingPermissionTool ?: "unknown",
            description = uiState.pendingPermissionDesc,
            onApprove = { viewModel.approvePermission() },
            onDeny = { viewModel.denyPermission() },
        )
    }
}

@Composable
private fun MessageContent(message: Message) {
    when (message) {
        is UserMessage -> {
            // User messages don't have parts in v1, show generic
            MessageBubble(
                role = "user",
                content = "sent",
            )
        }
        is AssistantMessage -> {
            // Assistant messages have parts stored separately
            // For now show a placeholder - parts come via message.part.updated events
            if (message.error != null) {
                MessageBubble(
                    role = "assistant",
                    content = "Error: ${message.error}",
                )
            }
        }
        else -> {}
    }
}
