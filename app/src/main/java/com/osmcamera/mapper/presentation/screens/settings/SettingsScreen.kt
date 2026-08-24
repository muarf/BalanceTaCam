package com.osmcamera.mapper.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    onNavigateToOfflineRegions: () -> Unit = {},
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
            // Account section - clickable to show user info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable {
                        if (isAuthenticated && user != null) {
                            // Show user info dialog or navigate to profile
                        }
                    }
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_account)) },
                    supportingContent = {
                        if (isAuthenticated && user != null) {
                            Column {
                                Text("👤 ${user!!.displayName}")
                                Text("📊 ${user!!.changesetCount} contributions", style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            Text("Non connecté")
                        }
                    },
                    leadingContent = {
                        Icon(
                            if (isAuthenticated) Icons.Default.AccountCircle else Icons.Default.Person,
                            contentDescription = null
                        )
                    }
                )
            }
            
            Divider()
            
            // Language section - with dialog
            var showLanguageDialog by remember { mutableStateOf(false) }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { showLanguageDialog = true }
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_language)) },
                    supportingContent = { Text("Français (Système)") },
                    leadingContent = {
                        Icon(Icons.Default.Language, contentDescription = null)
                    }
                )
            }
            
            if (showLanguageDialog) {
                AlertDialog(
                    onDismissRequest = { showLanguageDialog = false },
                    title = { Text(stringResource(R.string.settings_language)) },
                    text = {
                        Column {
                            Text("Langues disponibles :")
                            Spacer(modifier = Modifier.height(8.dp))
                            listOf("🇫🇷 Français", "🇬🇧 English", "🇪🇸 Español", "🇩🇪 Deutsch").forEach { lang ->
                                TextButton(
                                    onClick = { 
                                        showLanguageDialog = false
                                        // Language change would require app restart
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(lang, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showLanguageDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
            
            Divider()
            
            // Offline routing section
            val offlineViewModel: com.osmcamera.mapper.presentation.viewmodel.OfflineSettingsViewModel =
                androidx.hilt.navigation.compose.hiltViewModel()
            val offlineMode by offlineViewModel.offlineMode.collectAsState(initial = false)
            val torProxy by offlineViewModel.torProxyEnabled.collectAsState(initial = false)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onNavigateToOfflineRegions() }
            ) {
                ListItem(
                    headlineContent = { Text("Régions hors-ligne") },
                    supportingContent = { Text("Itinéraires anti-caméras sans Internet") },
                    leadingContent = {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                )
                ListItem(
                    headlineContent = { Text("Mode hors-ligne") },
                    supportingContent = {
                        Text(
                            if (offlineMode) "Routage 100% local activé"
                            else "Désactivé — routage via serveurs en ligne"
                        )
                    },
                    trailingContent = {
                        Switch(checked = offlineMode, onCheckedChange = { offlineViewModel.setOfflineMode(it) })
                    }
                )
                ListItem(
                    headlineContent = { Text("Proxy Tor (Orbot)") },
                    supportingContent = { Text("Requêtes réseau via 127.0.0.1:9050") },
                    trailingContent = {
                        Switch(checked = torProxy, onCheckedChange = { offlineViewModel.setTorProxy(it) })
                    }
                )
            }

            Divider()
            
            // About section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onNavigateToAbout() }
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.nav_about)) },
                    leadingContent = {
                        Icon(Icons.Default.Info, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                )
            }
        }
    }
}


