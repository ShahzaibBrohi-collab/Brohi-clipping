package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.security.configureAppCheck
import com.example.ui.navigation.BrohiAppNavigation
import com.example.ui.theme.BrohiClippingTheme
import com.example.ui.viewmodel.BrohiViewModel

class MainActivity : ComponentActivity() {

    private var pendingSharedText by mutableStateOf<String?>(null)
    private var pendingSharedVideo by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        handleIncomingShareIntent(intent)

        setContent {
            BrohiClippingTheme {
                val brohiViewModel: BrohiViewModel = viewModel()
                val toastMsg by brohiViewModel.toastMessage.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(toastMsg) {
                    val msg = toastMsg
                    if (!msg.isNullOrBlank()) {
                        snackbarHostState.showSnackbar(msg)
                        brohiViewModel.consumeToast()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    BrohiAppNavigation(
                        viewModel = brohiViewModel,
                        sharedTextUrl = pendingSharedText,
                        sharedVideoUri = pendingSharedVideo,
                        onConsumedShareIntent = {
                            pendingSharedText = null
                            pendingSharedVideo = null
                        }
                    )
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShareIntent(intent)
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val type = intent.type.orEmpty()
        if (type.startsWith("text/")) {
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
            if (!shared.isNullOrBlank()) {
                val urlMatch = Regex("https?://\\S+").find(shared)?.value ?: shared
                pendingSharedText = urlMatch
            }
        } else if (type.startsWith("video/")) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            if (uri != null) {
                pendingSharedVideo = uri
            }
        }
    }
}
