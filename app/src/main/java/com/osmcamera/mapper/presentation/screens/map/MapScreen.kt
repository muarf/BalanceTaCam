package com.osmcamera.mapper.presentation.screens.map

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.osmcamera.mapper.R
import com.osmcamera.mapper.presentation.viewmodel.AuthViewModel
import com.osmcamera.mapper.presentation.viewmodel.MapViewModel
import kotlinx.coroutines.launch
import org.osmdroid.mapsforge.MapsForgeTileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
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
    onNavigateToRouting: () -> Unit = {},
    mapViewModel: MapViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    val cameras by mapViewModel.cameras.collectAsState()
    val userLocation by mapViewModel.userLocation.collectAsState()
    val routePickTarget by mapViewModel.routePickTarget.collectAsState()
    val isAuthenticated = authViewModel.isAuthenticated()
    val user by authViewModel.user.collectAsState()
    
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var currentZoom by remember { mutableDoubleStateOf(15.0) }
    val dotDrawable = remember(context) { ContextCompat.getDrawable(context, R.drawable.ic_camera_dot) }
    val pinDrawable = remember(context) { ContextCompat.getDrawable(context, R.drawable.ic_camera_pin) }
    var isAddingCamera by remember { mutableStateOf(false) }
    var snackbarHostState = remember { SnackbarHostState() }
    var showPublicOnly by remember { mutableStateOf(true) }
    
    val selectedRoute by mapViewModel.selectedRoute.collectAsState()
    var lastDrawnRouteId by remember { mutableStateOf<String?>(null) }
    var lastRenderedCameras by remember { mutableStateOf<List<com.osmcamera.mapper.data.model.Camera>?>(null) }
    var lastRenderedZoomOutState by remember { mutableStateOf<Boolean?>(null) }
    
    // Filtered cameras - only public ones
    val filteredCameras = if (showPublicOnly) {
        cameras.filter { it.surveillance == "public" || it.surveillance == null }
    } else {
        cameras
    }
    
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
    
    // Removed ModalNavigationDrawer to avoid swipe conflict with map
    Box {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.map_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    actions = {
                        // Routing button
                        IconButton(onClick = {
                            // Pass user location to routing screen
                            onNavigateToRouting()
                        }) {
                            Icon(Icons.Default.Route, contentDescription = "Itinéraire", tint = MaterialTheme.colorScheme.tertiary)
                        }
                        
                        // Filter toggle
                        IconButton(onClick = { showPublicOnly = !showPublicOnly }) {
                            if (showPublicOnly) {
                                Icon(Icons.Default.FilterAlt, contentDescription = "Toutes les caméras", tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Icon(Icons.Default.FilterAltOff, contentDescription = "Seulement publiques")
                            }
                        }
                        
                        // Auth button in top bar
                        if (!isAuthenticated) {
                            IconButton(onClick = onNavigateToAuth) {
                                Icon(Icons.Default.Login, contentDescription = "Login")
                            }
                        } else if (user != null) {
                            var showLogoutDialog by remember { mutableStateOf(false) }
                            
                            IconButton(onClick = { showLogoutDialog = true }) {
                                Icon(Icons.Default.Logout, contentDescription = "Logout")
                            }
                            
                            if (showLogoutDialog) {
                                AlertDialog(
                                    onDismissRequest = { showLogoutDialog = false },
                                    title = { Text("Déconnexion") },
                                    text = { Text("Voulez-vous vous déconnecter de ${user!!.displayName} ?") },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            authViewModel.logout()
                                            showLogoutDialog = false
                                        }) {
                                            Text("Déconnexion")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showLogoutDialog = false }) {
                                            Text("Annuler")
                                        }
                                    }
                                )
                            }
                        }
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
                    FloatingActionButton(
                        onClick = {
                            if (userLocation != null) {
                                mapView?.controller?.apply {
                                    setZoom(18.0)
                                    animateTo(userLocation)
                                }
                            } else {
                                // Request location if not available
                                mapViewModel.getUserLocation()
                            }
                        },
                        modifier = Modifier.padding(bottom = 16.dp),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                    }
                    
                    // Add camera button
                    FloatingActionButton(
                        onClick = {
                            if (isAuthenticated) {
                                isAddingCamera = true
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "📍 Déplacez la carte puis tapez sur le marqueur pour confirmer",
                                        duration = SnackbarDuration.Long
                                    )
                                }
                            } else {
                                onNavigateToAuth()
                            }
                        },
                        containerColor = if (isAddingCamera) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
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
                val forgeSource = remember { mutableStateOf<MapsForgeTileSource?>(null) }
                AndroidView(
                    factory = { context ->
                        val startPoint = userLocation ?: GeoPoint(48.8566, 2.3522) // Paris default
                        val forge = OfflineBasemapHelper.prepare(
                            context, mapViewModel.installedBasemapFiles(),
                            startPoint.latitude, startPoint.longitude
                        )
                        val mv = if (forge != null) {
                            forgeSource.value = forge.source
                            MapView(context, forge.provider).apply {
                                setUseDataConnection(false)
                                setTileSource(forge.source)
                            }
                        } else {
                            MapView(context)
                        }
                        mv.apply {
                            if (forge == null) {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setUseDataConnection(true)
                            }
                            setMultiTouchControls(true)
                            
                            // Enable all gestures
                            isClickable = true
                            isFocusable = true
                            setBuiltInZoomControls(false)
                                                    
                            // Set initial position
                            controller.setZoom(15.0)
                            controller.setCenter(startPoint)
                            
                            // Force enable touch
                            minZoomLevel = 3.0
                            maxZoomLevel = 20.0
                            
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
                            
                            // Capture map taps for route point selection
                            overlays.add(0, MapEventsOverlay(object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    val target = mapViewModel.routePickTarget.value ?: return false
                                    if (p != null) {
                                        mapViewModel.setRoutePoint(target, p)
                                        mapViewModel.setRoutePickTarget(null)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (target == com.osmcamera.mapper.presentation.viewmodel.RoutePickTarget.START) {
                                                    "📍 Départ défini"
                                                } else {
                                                    "📍 Destination définie"
                                                }
                                            )
                                        }
                                        onNavigateToRouting()
                                    }
                                    return true
                                }

                                override fun longPressHelper(p: GeoPoint?): Boolean = false
                            }))

                            // Add scroll listener to load cameras when map moves
                            addMapListener(object : org.osmdroid.events.MapListener {
                                private var lastUpdate = 0L
                                
                                override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                                    // Debounce: only update every 2 seconds
                                    val now = System.currentTimeMillis()
                                    if (now - lastUpdate > 2000) {
                                        lastUpdate = now
                                        post {
                                            val bounds = boundingBox
                                            if (bounds != null) {
                                                mapViewModel.refreshCameras(
                                                    south = bounds.latSouth,
                                                    west = bounds.lonWest,
                                                    north = bounds.latNorth,
                                                    east = bounds.lonEast
                                                )
                                            }
                                        }
                                    }
                                    return true
                                }
                                
                                override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                                    val newZoom = event?.zoomLevel ?: zoomLevelDouble
                                    if (kotlin.math.abs(newZoom - currentZoom) >= 0.5 || (newZoom < 16.0) != (currentZoom < 16.0)) {
                                        currentZoom = newZoom
                                    }
                                    val now = System.currentTimeMillis()
                                    if (now - lastUpdate > 2500) {
                                        lastUpdate = now
                                        post {
                                            val bounds = boundingBox
                                            if (bounds != null) {
                                                mapViewModel.refreshCameras(
                                                    south = bounds.latSouth,
                                                    west = bounds.lonWest,
                                                    north = bounds.latNorth,
                                                    east = bounds.lonEast
                                                )
                                            }
                                        }
                                    }
                                    return true
                                }
                            })
                        }
                    },
                    update = { map ->                        // 1. Update route ONLY when the selected route actually changes
                        if (selectedRoute?.id != lastDrawnRouteId) {
                            lastDrawnRouteId = selectedRoute?.id
                            val route = selectedRoute
                            if (route != null && route.points.isNotEmpty()) {
                                map.post {
                                    try {
                                        map.overlays.removeAll { it is org.osmdroid.views.overlay.Polyline && it.id == "selected_route" }
                                        
                                        val routeLine = org.osmdroid.views.overlay.Polyline(map).apply {
                                            id = "selected_route"
                                            setPoints(route.points)
                                            outlinePaint.color = android.graphics.Color.parseColor("#2196F3")
                                            outlinePaint.strokeWidth = 12f
                                            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                                            title = "Itinéraire (${route.cameraCount} caméras)"
                                        }
                                        map.overlays.add(0, routeLine) // Add below markers
                                        
                                        // Zoom to route bounds ONLY ONCE when newly set
                                        val bounds = org.osmdroid.util.BoundingBox.fromGeoPoints(route.points)
                                        map.zoomToBoundingBox(bounds, true, 120)
                                        map.invalidate()
                                    } catch (e: Exception) {
                                        android.util.Log.e("BalanceTaCam", "Error displaying route", e)
                                    }
                                }
                            } else {
                                map.post {
                                    map.overlays.removeAll { it is org.osmdroid.views.overlay.Polyline && it.id == "selected_route" }
                                    map.invalidate()
                                }
                            }
                        }
                        
                        // 2. Update camera markers ONLY when cameras or icon state actually changes
                        val isZoomedOut = currentZoom < 16.0
                        if (filteredCameras !== lastRenderedCameras || isZoomedOut != lastRenderedZoomOutState) {
                            lastRenderedCameras = filteredCameras
                            lastRenderedZoomOutState = isZoomedOut
                            
                            map.overlays.removeAll { it is Marker && it.id?.startsWith("camera_") == true }
                            
                            val markerIcon = if (isZoomedOut) dotDrawable else pinDrawable
                            
                            filteredCameras.forEach { camera ->
                                val marker = Marker(map).apply {
                                    position = GeoPoint(camera.latitude, camera.longitude)
                                    id = "camera_${camera.id}"
                                    
                                    markerIcon?.let { icon = it }
                                    
                                    if (isZoomedOut) {
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    } else {
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    }
                                    
                                    val type = camera.cameraType ?: "non spécifié"
                                    val mount = camera.cameraMount ?: "non spécifié"
                                    title = "📷 Caméra de Surveillance"
                                    
                                    val infos = mutableListOf<String>()
                                    infos.add("━━━━━━━━━━━━━━━━")
                                    infos.add("Type: $type")
                                    infos.add("Support: $mount")
                                    camera.cameraDirection?.let { infos.add("Direction: ${it}° (0=Nord)") }
                                    camera.surveillance?.let { infos.add("Surveillance: $it") }
                                    camera.operator?.let { infos.add("Opérateur: $it") }
                                    camera.operatorType?.let { infos.add("Type opérateur: $it") }
                                    camera.surveillanceZone?.let { infos.add("Zone: $it") }
                                    camera.height?.let { infos.add("Hauteur: $it") }
                                    camera.level?.let { infos.add("Niveau: $it") }
                                    camera.description?.let { infos.add("Description: $it") }
                                    infos.add("━━━━━━━━━━━━━━━━")
                                    infos.add("ID OSM: ${camera.id}")
                                    snippet = infos.joinToString("\n")
                                    
                                    setOnMarkerClickListener { clickedMarker, _ ->
                                        clickedMarker.showInfoWindow()
                                        map.controller.animateTo(clickedMarker.position)
                                        true
                                    }
                                }
                                map.overlays.add(marker)
                            }
                            
                            map.invalidate()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                DisposableEffect(Unit) {
                    onDispose { OfflineBasemapHelper.dispose(forgeSource.value) }
                }
                
                // Show route point selection banner
                if (routePickTarget != null) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 64.dp)
                    ) {
                        Text(
                            text = if (routePickTarget == com.osmcamera.mapper.presentation.viewmodel.RoutePickTarget.START) {
                                "📍 Touchez la carte pour placer le DÉPART"
                            } else {
                                "📍 Touchez la carte pour placer l'ARRIVÉE"
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    // Show crosshair for precise placement
                    Icon(
                        painter = painterResource(id = R.drawable.ic_crosshair),
                        contentDescription = "Crosshair",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(64.dp),
                        tint = Color.Unspecified
                    )
                    
                    Button(
                        onClick = { mapViewModel.setRoutePickTarget(null) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp)
                    ) {
                        Text("Annuler la sélection")
                    }
                }
                
                // Show camera count
                if (filteredCameras.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (showPublicOnly) {
                                "${filteredCameras.size} caméras publiques"
                            } else {
                                "${filteredCameras.size} caméras"
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                // Show crosshair when adding camera
                if (isAddingCamera) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_crosshair),
                        contentDescription = "Crosshair",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(64.dp),
                        tint = Color.Unspecified
                    )
                    
                    // Show confirm button
                    Button(
                        onClick = {
                            mapView?.let { map ->
                                val center = map.mapCenter as GeoPoint
                                isAddingCamera = false
                                onAddCamera(center.latitude, center.longitude)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 100.dp)
                    ) {
                        Text("Confirmer cette position")
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

// DrawerContent removed - using top bar buttons instead


