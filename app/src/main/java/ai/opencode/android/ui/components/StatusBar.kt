package ai.opencode.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.opencode.android.data.api.ConnectionState
import ai.opencode.android.data.model.SessionStatus
import ai.opencode.android.ui.theme.TuiColors
import ai.opencode.android.ui.theme.TuiFont

@Composable
fun StatusBar(
    connectionState: ConnectionState,
    currentModel: String?,
    currentAgent: String? = null,
    sessionStatus: SessionStatus? = null,
    currentSessionTitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TuiColors.StatusBar,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Connection indicator
                Icon(
                    imageVector = when (connectionState) {
                        ConnectionState.Connected -> Icons.Default.CheckCircle
                        ConnectionState.Connecting -> Icons.Default.Refresh
                        ConnectionState.Disconnected -> Icons.Default.Error
                    },
                    contentDescription = null,
                    tint = when (connectionState) {
                        ConnectionState.Connected -> TuiColors.TerminalGreen
                        ConnectionState.Connecting -> TuiColors.TerminalAmber
                        ConnectionState.Disconnected -> TuiColors.TerminalRed
                    },
                    modifier = Modifier.size(10.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (connectionState) {
                        ConnectionState.Connected -> "connected"
                        ConnectionState.Connecting -> "connecting..."
                        ConnectionState.Disconnected -> "disconnected"
                    },
                    fontSize = 10.sp,
                    fontFamily = TuiFont.Mono,
                    color = TuiColors.OnSurface,
                )

                // Session status
                if (connectionState == ConnectionState.Connected && sessionStatus != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    when (sessionStatus) {
                        is SessionStatus.Busy -> {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = TuiColors.TerminalCyan,
                                modifier = Modifier.size(8.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "busy",
                                fontSize = 10.sp,
                                fontFamily = TuiFont.Mono,
                                color = TuiColors.TerminalCyan,
                            )
                        }
                        is SessionStatus.Retry -> {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = TuiColors.TerminalAmber,
                                modifier = Modifier.size(8.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "retry ${sessionStatus.attempt}",
                                fontSize = 10.sp,
                                fontFamily = TuiFont.Mono,
                                color = TuiColors.TerminalAmber,
                            )
                        }
                        is SessionStatus.Idle -> { /* No extra indicator */ }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentSessionTitle != null) {
                    Text(
                        text = currentSessionTitle,
                        fontSize = 10.sp,
                        fontFamily = TuiFont.Mono,
                        color = TuiColors.OnSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }
                if (currentAgent != null) {
                    Text(
                        text = currentAgent,
                        fontSize = 10.sp,
                        fontFamily = TuiFont.Mono,
                        color = TuiColors.TerminalMagenta,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                if (currentModel != null) {
                    Text(
                        text = currentModel,
                        fontSize = 10.sp,
                        fontFamily = TuiFont.Mono,
                        color = TuiColors.TerminalCyan,
                    )
                }
            }
        }
    }
}
