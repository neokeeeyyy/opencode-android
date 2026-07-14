package ai.opencode.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.opencode.android.ui.theme.TuiColors
import ai.opencode.android.ui.theme.TuiFont

@Composable
fun CodeBlock(
    code: String,
    language: String?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = TuiColors.CodeBackground),
        border = BorderStroke(1.dp, TuiColors.CodeBorder),
        shape = RoundedCornerShape(2.dp),
    ) {
        Column {
            if (!language.isNullOrBlank()) {
                Text(
                    text = language,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TuiColors.CodeBorder)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    color = TuiColors.OnSurface,
                    fontSize = 9.sp,
                    fontFamily = TuiFont.Mono,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                text = code.trimEnd(),
                modifier = Modifier.padding(10.dp),
                color = TuiColors.OnBackground,
                fontSize = 11.sp,
                fontFamily = TuiFont.Mono,
                lineHeight = 15.sp,
            )
        }
    }
}
