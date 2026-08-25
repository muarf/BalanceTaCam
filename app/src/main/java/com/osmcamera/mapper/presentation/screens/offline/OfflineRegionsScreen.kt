package com.osmcamera.mapper.presentation.screens.offline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osmcamera.mapper.offline.BasemapInfo
import com.osmcamera.mapper.offline.RegionInfo
import com.osmcamera.mapper.presentation.viewmodel.CatalogMatch
import com.osmcamera.mapper.presentation.viewmodel.OfflineRegionsViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * Screen to browse, download and delete offline routing regions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineRegionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: OfflineRegionsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Régions hors-ligne") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "Téléchargez une région pour calculer vos itinéraires anti-caméras sans aucune connexion Internet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (state.installed.isNotEmpty()) {
                Text(
                    text = "Installées : ${state.installed.joinToString()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            when {
                state.loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.regions.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Aucune région disponible.\nVérifiez votre connexion puis réessayez.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = state.query,
                                onValueChange = viewModel::onQueryChange,
                                label = { Text("Rechercher un pays ou une région dans le monde") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (state.query.isNotBlank()) {
                            if (state.searchResults.isEmpty()) {
                                item {
                                    Text(
                                        "Aucun résultat pour « ${state.query} »",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            items(state.searchResults, key = { "search_${it.slug}" }) { match ->
                                SearchMatchCard(
                                    match = match,
                                    installed = match.slug in state.installed,
                                    tileCacheInstalled = match.slug in state.installedTileCaches,
                                    updatable = match.slug in state.updatable,
                                    downloading = state.downloadingId == match.slug,
                                    progress = state.downloadProgress,
                                    statusText = if (state.downloadingId == match.slug) state.statusText else "",
                                    onDownload = { match.region?.let { viewModel.download(it) } },
                                    onDownloadTiles = { match.region?.let { viewModel.downloadTiles(it) } },
                                    onUpdate = { match.region?.let { viewModel.download(it, overwrite = true) } },
                                    onDelete = {
                                        match.region?.let { region ->
                                            viewModel.delete(region)
                                            viewModel.onQueryChange(state.query)
                                        }
                                    },
                                    onDeleteTiles = {
                                        match.region?.let { region ->
                                            viewModel.deleteTileCache(region)
                                            viewModel.onQueryChange(state.query)
                                        }
                                    }
                                )
                            }
                        } else {
                            items(state.regions, key = { "region_${it.id}" }) { region ->
                                RegionCard(
                                    region = region,
                                    installed = region.id in state.installed,
                                    tileCacheInstalled = region.id in state.installedTileCaches,
                                    updatable = region.id in state.updatable,
                                    downloading = state.downloadingId == region.id,
                                    progress = state.downloadProgress,
                                    statusText = if (state.downloadingId == region.id) state.statusText else "",
                                    onDownload = { viewModel.download(region) },
                                    onDownloadTiles = { viewModel.downloadTiles(region) },
                                    onUpdate = { viewModel.download(region, overwrite = true) },
                                    onDelete = { viewModel.delete(region) },
                                    onDeleteTiles = { viewModel.deleteTileCache(region) }
                                )
                            }

                            if (state.basemaps.isNotEmpty()) {
                            item {
                                Text(
                                    "Cartes hors-ligne (affichage)",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            items(state.basemaps, key = { "basemap_${it.id}" }) { basemap ->
                                BasemapCard(
                                    basemap = basemap,
                                    installed = basemap.id in state.installedBasemaps,
                                    updatable = basemap.id in state.updatableBasemaps,
                                    downloading = state.downloadingId == basemap.id,
                                    progress = state.downloadProgress,
                                    statusText = if (state.downloadingId == basemap.id) state.statusText else "",
                                    onDownload = { viewModel.downloadBasemap(basemap) },
                                    onUpdate = { viewModel.downloadBasemap(basemap, overwrite = true) },
                                     onDelete = { viewModel.deleteBasemap(basemap) }
                                )
                }
            }
        }
    }
}
            }
        }
    }
}

@Composable
private fun RegionCard(
    region: RegionInfo,
    installed: Boolean,
    tileCacheInstalled: Boolean = false,
    updatable: Boolean,
    downloading: Boolean,
    progress: Float,
    statusText: String,
    onDownload: () -> Unit,
    onDownloadTiles: () -> Unit = {},
    onUpdate: () -> Unit,
    onDelete: () -> Unit,
    onDeleteTiles: () -> Unit = {}
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(region.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        formatBytes(region.graphBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                when {
                    installed && updatable -> {
                        var confirmDelete by remember { mutableStateOf(false) }
                        Button(
                            onClick = onUpdate,
                            enabled = !downloading,
                            modifier = Modifier.padding(end = 4.dp)
                        ) { Text("Mettre à jour") }
                        IconButton(onClick = { confirmDelete = true }, enabled = !downloading) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer",
                                tint = MaterialTheme.colorScheme.error)
                        }
                        if (confirmDelete) {
                            AlertDialog(
                                onDismissRequest = { confirmDelete = false },
                                title = { Text("Supprimer ${region.name} ?") },
                                text = { Text("Le routage hors-ligne sera indisponible pour cette région.") },
                                confirmButton = {
                                    TextButton(onClick = { confirmDelete = false; onDelete() }) {
                                        Text("Supprimer", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { confirmDelete = false }) { Text("Annuler") }
                                }
                            )
                        }
                    }
                    installed -> {
                        var confirmDelete by remember { mutableStateOf(false) }
                        TextButton(onClick = { confirmDelete = true }, enabled = !downloading) {
                            Text("Supprimer", color = MaterialTheme.colorScheme.error)
                        }
                        if (confirmDelete) {
                            AlertDialog(
                                onDismissRequest = { confirmDelete = false },
                                title = { Text("Supprimer ${region.name} ?") },
                                text = { Text("Le routage hors-ligne sera indisponible pour cette région.") },
                                confirmButton = {
                                    TextButton(onClick = { confirmDelete = false; onDelete() }) {
                                        Text("Supprimer", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { confirmDelete = false }) { Text("Annuler") }
                                }
                            )
                        }
                    }
                    downloading -> {}
                    else -> {
                        Button(onClick = onDownload) { Text("Télécharger") }
                    }
                }
            }

            if (downloading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                if (statusText.isNotEmpty()) {
                    Text(statusText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            } else if (installed) {
                Spacer(Modifier.height(4.dp))
                if (updatable) {
                    Text(
                        "⬆️ Nouvelle version disponible",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                } else {
                    Text(
                        "✅ Installée",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (!downloading && installed) {
                Spacer(Modifier.height(4.dp))
                if (tileCacheInstalled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "🗺️ Carte OSM installée",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDeleteTiles, enabled = !downloading) {
                            Text("Supprimer carte", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onDownloadTiles,
                        enabled = !downloading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Télécharger la carte (OSM)")
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val nf = NumberFormat.getIntegerInstance(Locale.FRENCH)
    return when {
        bytes >= 1_000_000_000 -> "${nf.format(bytes / 100_000_000 / 10.0)} Go"
        else -> "${nf.format(bytes / 100_000 / 10.0)} Mo"
    }
}

@Composable
private fun BasemapCard(
    basemap: BasemapInfo,
    installed: Boolean,
    updatable: Boolean,
    downloading: Boolean,
    progress: Float,
    statusText: String,
    onDownload: () -> Unit,
    onUpdate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(basemap.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        formatBytes(basemap.bytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                when {
                    installed && updatable -> {
                        var confirmDelete by remember { mutableStateOf(false) }
                        Button(onClick = onUpdate, enabled = !downloading,
                            modifier = Modifier.padding(end = 4.dp)) { Text("Mettre à jour") }
                        IconButton(onClick = { confirmDelete = true }, enabled = !downloading) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer",
                                tint = MaterialTheme.colorScheme.error)
                        }
                        if (confirmDelete) {
                            AlertDialog(
                                onDismissRequest = { confirmDelete = false },
                                title = { Text("Supprimer ${basemap.name} ?") },
                                text = { Text("La carte reviendra en mode en-ligne.") },
                                confirmButton = {
                                    TextButton(onClick = { confirmDelete = false; onDelete() }) {
                                        Text("Supprimer", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { confirmDelete = false }) { Text("Annuler") }
                                }
                            )
                        }
                    }
                    installed -> {
                        var confirmDelete by remember { mutableStateOf(false) }
                        TextButton(onClick = { confirmDelete = true }, enabled = !downloading) {
                            Text("Supprimer", color = MaterialTheme.colorScheme.error)
                        }
                        if (confirmDelete) {
                            AlertDialog(
                                onDismissRequest = { confirmDelete = false },
                                title = { Text("Supprimer ${basemap.name} ?") },
                                text = { Text("La carte reviendra en mode en-ligne.") },
                                confirmButton = {
                                    TextButton(onClick = { confirmDelete = false; onDelete() }) {
                                        Text("Supprimer", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { confirmDelete = false }) { Text("Annuler") }
                                }
                            )
                        }
                    }
                    downloading -> {}
                    else -> {
                        Button(onClick = onDownload) { Text("Télécharger") }
                    }
                }
            }

            if (downloading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                if (statusText.isNotEmpty()) {
                    Text(statusText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            } else if (installed) {
                Spacer(Modifier.height(4.dp))
                Text(
                    if (updatable) "⬆️ Nouvelle version disponible" else "✅ Installée",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun SearchMatchCard(
    match: CatalogMatch,
    installed: Boolean,
    tileCacheInstalled: Boolean = false,
    updatable: Boolean,
    downloading: Boolean,
    progress: Float,
    statusText: String,
    onDownload: () -> Unit,
    onDownloadTiles: () -> Unit = {},
    onUpdate: () -> Unit,
    onDelete: () -> Unit,
    onDeleteTiles: () -> Unit = {}
) {
    val region = match.region
    if (region != null) {
        RegionCard(
            region = region,
            installed = installed,
            tileCacheInstalled = tileCacheInstalled,
            updatable = updatable,
            downloading = downloading,
            progress = progress,
            statusText = statusText,
            onDownload = onDownload,
            onDownloadTiles = onDownloadTiles,
            onUpdate = onUpdate,
            onDelete = onDelete,
            onDeleteTiles = onDeleteTiles
        )
    } else {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(match.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    continentLabel(match.continent),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Pas encore construite — disponible après la prochaine construction mensuelle",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun continentLabel(code: String): String = when (code) {
    "AF" -> "Afrique"
    "AS" -> "Asie"
    "CA" -> "Amérique centrale"
    "EU" -> "Europe"
    "NA" -> "Amérique du Nord"
    "SA" -> "Amérique du Sud"
    "OC" -> "Océanie"
    else -> ""
}
