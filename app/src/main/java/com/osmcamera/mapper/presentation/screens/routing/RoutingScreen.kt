package com.osmcamera.mapper.presentation.screens.routing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.osmcamera.mapper.data.model.Route
import com.osmcamera.mapper.data.model.RouteComparison
import com.osmcamera.mapper.presentation.viewmodel.MapViewModel
import com.osmcamera.mapper.presentation.viewmodel.RoutePickTarget
import com.osmcamera.mapper.presentation.viewmodel.RoutingUiState
import com.osmcamera.mapper.presentation.viewmodel.RoutingViewModel

/**
 * Routing screen for camera avoidance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutingScreen(
    onNavigateBack: () -> Unit,
    onShowRouteOnMap: (Route) -> Unit,
    userLocation: org.osmdroid.util.GeoPoint? = null,
    mapViewModel: MapViewModel,
    onPickPointOnMap: (RoutePickTarget) -> Unit,
    viewModel: RoutingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val startPoint by mapViewModel.routeStartPoint.collectAsState()
    val endPoint by mapViewModel.routeEndPoint.collectAsState()
    
    var startAddressQuery by remember { mutableStateOf("") }
    var endAddressQuery by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🚗 Itinéraire Anti-Caméras") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (startPoint != null || endPoint != null) {
                        IconButton(onClick = {
                            mapViewModel.clearRoutePoints()
                            viewModel.clearResults()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Point selection cards
            
            // Start point
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Point de départ", style = MaterialTheme.typography.titleMedium)
                        }
                        
                        // Ma position button
                        if (userLocation != null) {
                            IconButton(onClick = { mapViewModel.setRoutePoint(RoutePickTarget.START, userLocation) }) {
                                Icon(Icons.Default.MyLocation, contentDescription = "Ma position")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (startPoint != null) {
                        Text("${String.format("%.5f", startPoint!!.latitude)}, ${String.format("%.5f", startPoint!!.longitude)}")
                        TextButton(onClick = { mapViewModel.setRoutePoint(RoutePickTarget.START, null) }) {
                            Text("Effacer")
                        }
                    } else {
                        // Search address
                        OutlinedTextField(
                            value = startAddressQuery,
                            onValueChange = { startAddressQuery = it },
                            label = { Text("Rechercher adresse") },
                            placeholder = { Text("Ex: 10 rue de Rivoli, Paris") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { 
                                    viewModel.searchAddress(startAddressQuery) { geo ->
                                        geo?.let { mapViewModel.setRoutePoint(RoutePickTarget.START, it) }
                                    }
                                }) {
                                    Icon(Icons.Default.Search, contentDescription = "Rechercher")
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { onPickPointOnMap(RoutePickTarget.START) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.EditLocation, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sélectionner sur la Carte")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // End point
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFFF44336))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Destination", style = MaterialTheme.typography.titleMedium)
                        }
                        
                        // Ma position button
                        if (userLocation != null) {
                            IconButton(onClick = { mapViewModel.setRoutePoint(RoutePickTarget.END, userLocation) }) {
                                Icon(Icons.Default.MyLocation, contentDescription = "Ma position")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (endPoint != null) {
                        Text("${String.format("%.5f", endPoint!!.latitude)}, ${String.format("%.5f", endPoint!!.longitude)}")
                        TextButton(onClick = { mapViewModel.setRoutePoint(RoutePickTarget.END, null) }) {
                            Text("Effacer")
                        }
                    } else {
                        // Search address
                        OutlinedTextField(
                            value = endAddressQuery,
                            onValueChange = { endAddressQuery = it },
                            label = { Text("Rechercher adresse") },
                            placeholder = { Text("Ex: Tour Eiffel, Paris") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { 
                                    viewModel.searchAddress(endAddressQuery) { geo ->
                                        geo?.let { mapViewModel.setRoutePoint(RoutePickTarget.END, it) }
                                    }
                                }) {
                                    Icon(Icons.Default.Search, contentDescription = "Rechercher")
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { onPickPointOnMap(RoutePickTarget.END) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.EditLocation, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sélectionner sur la Carte")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Distance d'évitement des caméras
            val avoidanceRadius by viewModel.avoidanceRadius.collectAsState()
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Marge d'évitement", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            "${avoidanceRadius.toInt()} mètres",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Slider(
                        value = avoidanceRadius.toFloat(),
                        onValueChange = { viewModel.setAvoidanceRadius(it.toDouble()) },
                        valueRange = 15f..100f,
                        steps = 16,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(25, 40, 60, 80).forEach { preset ->
                            FilterChip(
                                selected = avoidanceRadius.toInt() == preset,
                                onClick = { viewModel.setAvoidanceRadius(preset.toDouble()) },
                                label = { Text("${preset}m") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Calculate button
            Button(
                onClick = { viewModel.calculateRoute(startPoint, endPoint) },
                modifier = Modifier.fillMaxWidth(),
                enabled = startPoint != null && endPoint != null && uiState !is RoutingUiState.Calculating
            ) {
                if (uiState is RoutingUiState.Calculating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calcul en cours...")
                } else {
                    Icon(Icons.Default.Route, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calculer l'Itinéraire")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Results
            when (val state = uiState) {
                is RoutingUiState.RoutesCalculated -> {
                    RouteComparisonView(
                        comparison = state.comparison,
                        onRouteSelected = onShowRouteOnMap
                    )
                }
                is RoutingUiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "❌ ${state.message}",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                else -> {
                    // Idle or Calculating
                }
            }
        }
    }
}

@Composable
private fun RouteComparisonView(
    comparison: RouteComparison,
    onRouteSelected: (Route) -> Unit
) {
    Column {
        // Summary card
        if (comparison.camerasSaved > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "🎉 Vous pouvez éviter ${comparison.camerasSaved} caméra(s) !",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Text(
            "Routes Trouvées",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(comparison.routes) { route ->
                RouteCard(
                    route = route,
                    isBest = route.id == comparison.bestRoute.id,
                    isDirect = route.id == comparison.directRoute?.id,
                    onClick = { onRouteSelected(route) }
                )
            }
        }
    }
}

@Composable
private fun RouteCard(
    route: Route,
    isBest: Boolean,
    isDirect: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isBest -> MaterialTheme.colorScheme.primaryContainer
                isDirect -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isBest) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = route.getScoreLabel(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (isBest) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    "RECOMMANDÉ",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        if (isDirect) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    "DIRECT",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row {
                        Icon(
                            Icons.Default.Straighten,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${String.format("%.1f", route.distanceKm)} km",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${String.format("%.0f", route.durationMinutes)} min",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }
}

