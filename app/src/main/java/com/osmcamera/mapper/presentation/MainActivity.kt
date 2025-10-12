package com.osmcamera.mapper.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.osmcamera.mapper.presentation.navigation.AppNavigation
import com.osmcamera.mapper.presentation.theme.OSMCameraMapperTheme
import com.osmcamera.mapper.presentation.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for the app
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val authViewModel: AuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle OAuth callback
        handleIntent(intent)
        
        setContent {
            OSMCameraMapperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        val data: Uri? = intent?.data
        
        // Check if this is an OAuth callback
        if (data?.scheme == "osmcamera" && data.host == "oauth") {
            // OAuth 2.0 uses "code" parameter instead of "oauth_verifier"
            val code = data.getQueryParameter("code")
            if (code != null) {
                authViewModel.completeAuthentication(code)
            }
        }
    }
}


