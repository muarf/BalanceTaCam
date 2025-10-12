package com.osmcamera.mapper.presentation.screens.map

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.osmcamera.mapper.R
import com.osmcamera.mapper.presentation.viewmodel.AuthViewModel
import com.osmcamera.mapper.presentation.viewmodel.MapViewModel
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Main map screen
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onAddCamera: (Double, Double) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAuth: () -> Unit,
    mapViewModel: MapViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    val cameras by mapViewModel.cameras.collectAsState()
    val userLocation by mapViewModel.userLocation.collectAsState()
    val isAuthenticated = authViewModel.isAuthenticated()
    val user by authViewModel.user.collectAsState()
    
    var mapView by remember { mutableStateOf<MapView?>(null) }
    
    // Location permissions
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    
    LaunchedEffect(Unit) {
        locationPermissions.launchMultiplePermissionRequest()
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    user = user,
                    isAuthenticated = isAuthenticated,
                    onNavigateToSettings = {
                        scope.launch {
                            drawerState.close()
                            onNavigateToSettings()
                        }
                    },
                    onNavigateToAuth = {
                        scope.launch {
                            drawerState.close()
                            onNavigateToAuth()
                        }
                    },
                    onLogout = {
                        authViewModel.logout()
                        scope.launch {
                            drawerState.close()
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.map_title)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            mapView?.let { map ->
                                val bounds = map.boundingBox
                                mapViewModel.refreshCameras(
                                    south = bounds.latSouth,
                                    west = bounds.lonWest,
                                    north = bounds.latNorth,
                                    east = bounds.lonEast
                                )
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            },
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    // Center on user location button
                    if (userLocation != null) {
                        FloatingActionButton(
                            onClick = {
                                userLocation?.let { location ->
                                    mapView?.controller?.animateTo(location)
                                }
                            },
                            modifier = Modifier.padding(bottom = 16.dp),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                        }
                    }
                    
                    // Add camera button
                    FloatingActionButton(
                        onClick = {
                            if (isAuthenticated) {
                                mapView?.let { map ->
                                    val center = map.mapCenter as GeoPoint
                                    onAddCamera(center.latitude, center.longitude)
                                }
                            } else {
                                onNavigateToAuth()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_camera))
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // OSMDroid MapView
                AndroidView(
                    factory = { context ->
                        MapView(context).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            
                            // Set initial position
                            controller.setZoom(15.0)
                            val startPoint = userLocation ?: GeoPoint(48.8566, 2.3522) // Paris default
                            controller.setCenter(startPoint)
                            
                            // Add location overlay if permissions granted
                            if (locationPermissions.allPermissionsGranted) {
                                val locationOverlay = MyLocationNewOverlay(this)
                                locationOverlay.enableMyLocation()
                                overlays.add(locationOverlay)
                            }
                            
                            mapView = this
                            
                            // Load cameras when map is ready
                            post {
                                val bounds = boundingBox
                                mapViewModel.loadCamerasInBounds(
                                    south = bounds.latSouth,
                                    west = bounds.lonWest,
                                    north = bounds.latNorth,
                                    east = bounds.lonEast
                                )
                            }
                        }
                    },
                    update = { map ->
                        // Update camera markers
                        map.overlays.removeAll { it is Marker && it.id?.startsWith("camera_") == true }
                        
                        cameras.forEach { camera ->
                            val marker = Marker(map).apply {
                                position = GeoPoint(camera.latitude, camera.longitude)
                                id = "camera_${camera.id}"
                                title = camera.cameraType ?: "Camera"
                                snippet = camera.operator ?: ""
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            map.overlays.add(marker)
                        }
                        
                        map.invalidate()
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Show camera count
                if (cameras.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "${cameras.size} cameras",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDetach()
        }
    }
}

@Composable
private fun DrawerContent(
    user: com.osmcamera.mapper.data.model.User?,
    isAuthenticated: Boolean,
    onNavigateToSettings: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (isAuthenticated && user != null) {
            Text(
                text = stringResource(R.string.auth_logged_in_as, user.displayName),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
        if (!isAuthenticated) {
            TextButton(
                onClick = onNavigateToAuth,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Login, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.auth_login_button))
            }
        }
        
        TextButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.nav_settings))
        }
        
        if (isAuthenticated) {
            TextButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.auth_logout))
            }
        }
    }
}


