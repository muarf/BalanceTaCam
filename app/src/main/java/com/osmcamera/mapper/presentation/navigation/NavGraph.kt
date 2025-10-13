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
        
        composable(Screen.Map.route) { backStackEntry ->
            val mapViewModel: com.osmcamera.mapper.presentation.viewmodel.MapViewModel = 
                androidx.hilt.navigation.compose.hiltViewModel()
            
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
                },
                mapViewModel = mapViewModel
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
            // Get MapViewModel from previous screen (Map)
            val previousEntry = navController.previousBackStackEntry
            val mapViewModel: com.osmcamera.mapper.presentation.viewmodel.MapViewModel? = 
                if (previousEntry?.destination?.route == Screen.Map.route) {
                    androidx.hilt.navigation.compose.hiltViewModel(previousEntry)
                } else null
            
            RoutingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onShowRouteOnMap = { route ->
                    // Set the route in MapViewModel to display it
                    mapViewModel?.setSelectedRoute(route)
                    navController.popBackStack()
                },
                userLocation = mapViewModel?.userLocation?.value
            )
        }
    }
}


