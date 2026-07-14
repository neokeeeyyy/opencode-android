package ai.opencode.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import ai.opencode.android.server.EmbeddedServer
import ai.opencode.android.ui.navigation.OpenCodeNavHost
import ai.opencode.android.ui.theme.OpenCodeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var embeddedServer: EmbeddedServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        embeddedServer.start()

        setContent {
            OpenCodeTheme {
                OpenCodeNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        embeddedServer.stop()
    }
}
