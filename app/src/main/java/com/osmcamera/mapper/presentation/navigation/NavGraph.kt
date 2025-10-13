package com.osmcamera.mapper.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.osmcamera.mapper.presentation.screens.auth.AuthScreen
import com.osmcamera.mapper.presentation.screens.map.MapScreen
import com.osmcamera.mapper.presentation.screens.addcamera.AddCameraScreen
import com.osmcamera.mapper.presentation.screens.settings.SettingsScreen
import com.osmcamera.mapper.presentation.screens.about.AboutScreen
import com.osmcamera.mapper.presentation.screens.routing.RoutingScreen

/**
 * Navigation destinations
 */
sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Map : Screen("map")
    object AddCamera : Screen("add_camera/{latitude}/{longitude}") {
        fun createRoute(latitude: Double, longitude: Double) = 
            "add_camera/$latitude/$longitude"
    }
    object Settings : Screen("settings")
    object About : Screen("about")
    object Routing : Screen("routing")
}

/**
 * App navigation composable
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Map.route
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Map.route) {
            MapScreen(
                onAddCamera = { latitude, longitude ->
                    navController.navigate(Screen.AddCamera.createRoute(latitude, longitude))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToAuth = {
                    navController.navigate(Screen.Auth.route)
                },
                onNavigateToRouting = {
                    navController.navigate(Screen.Routing.route)
                }
            )
        }
        
        composable(Screen.AddCamera.route) { backStackEntry ->
            val latitude = backStackEntry.arguments?.getString("latitude")?.toDoubleOrNull() ?: 0.0
            val longitude = backStackEntry.arguments?.getString("longitude")?.toDoubleOrNull() ?: 0.0
            
            AddCameraScreen(
                initialLatitude = latitude,
                initialLongitude = longitude,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onCameraAdded = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAbout = {
                    navController.navigate(Screen.About.route)
                }
            )
        }
        
        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Routing.route) { backStackEntry ->
            // Get user location from map screen if available
            val savedStateHandle = backStackEntry.savedStateHandle
            val userLat = savedStateHandle.get<Double>("userLat")
            val userLon = savedStateHandle.get<Double>("userLon")
            val userLocation = if (userLat != null && userLon != null) {
                org.osmdroid.util.GeoPoint(userLat, userLon)
            } else null
            
            RoutingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onShowRouteOnMap = { route ->
                    // TODO: Show route on map
                    navController.popBackStack()
                },
                userLocation = userLocation
            )
        }
    }
}


