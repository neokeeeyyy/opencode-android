package ai.opencode.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.opencode.android.ui.theme.TuiColors
import ai.opencode.android.ui.theme.TuiFont

@Composable
fun LoadingIndicator(
    message: String = "loading...",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            color = TuiColors.TerminalGreen,
            strokeWidth = 2.dp,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            color = TuiColors.OnSurface,
            fontSize = 12.sp,
            fontFamily = TuiFont.Mono,
        )
    }
}
