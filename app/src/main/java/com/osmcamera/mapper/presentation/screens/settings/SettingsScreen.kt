package com.osmcamera.mapper.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.osmcamera.mapper.R
import com.osmcamera.mapper.presentation.viewmodel.AuthViewModel

/**
 * Settings screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val user by authViewModel.user.collectAsState()
    val isAuthenticated = authViewModel.isAuthenticated()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Account section
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_account)) },
                supportingContent = {
                    if (isAuthenticated && user != null) {
                        Text(user!!.displayName)
                    } else {
                        Text("Not logged in")
                    }
                }
            )
            
            Divider()
            
            // Language section
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_language)) },
                supportingContent = { Text("System Default") }
            )
            
            Divider()
            
            // About section
            ListItem(
                headlineContent = { Text(stringResource(R.string.nav_about)) },
                leadingContent = {
                    Icon(Icons.Default.Info, contentDescription = null)
                },
                modifier = Modifier.clickable { onNavigateToAbout() }
            )
        }
    }
}


