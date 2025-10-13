package com.osmcamera.mapper.presentation.screens.addcamera

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.osmcamera.mapper.R
import com.osmcamera.mapper.data.model.*
import com.osmcamera.mapper.presentation.viewmodel.AddCameraUiState
import com.osmcamera.mapper.presentation.viewmodel.AddCameraViewModel

/**
 * Add camera screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCameraScreen(
    initialLatitude: Double,
    initialLongitude: Double,
    onNavigateBack: () -> Unit,
    onCameraAdded: () -> Unit,
    viewModel: AddCameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraData by viewModel.cameraData.collectAsState()
    val isDetailedMode by viewModel.isDetailedMode.collectAsState()
    
    LaunchedEffect(initialLatitude, initialLongitude) {
        android.util.Log.d("BalanceTaCam", "AddCameraScreen - Received position: $initialLatitude, $initialLongitude")
        viewModel.setPosition(initialLatitude, initialLongitude)
    }
    
    LaunchedEffect(uiState) {
        when (uiState) {
            is AddCameraUiState.Success -> {
                onCameraAdded()
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_camera_title)) },
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
            // Mode tabs
            TabRow(
                selectedTabIndex = if (isDetailedMode) 1 else 0
            ) {
                Tab(
                    selected = !isDetailedMode,
                    onClick = { viewModel.setDetailedMode(false) },
                    text = { Text(stringResource(R.string.add_camera_quick_mode)) }
                )
                Tab(
                    selected = isDetailedMode,
                    onClick = { viewModel.setDetailedMode(true) },
                    text = { Text(stringResource(R.string.add_camera_detailed_mode)) }
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Position info
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.add_camera_position),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Lat: ${String.format("%.6f", cameraData.latitude)}")
                        Text("Lon: ${String.format("%.6f", cameraData.longitude)}")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (!isDetailedMode) {
                    // Quick mode - just position
                    Text(
                        text = "Quick mode adds only the basic camera information to OSM.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Detailed mode - all fields with all options
                    DetailedModeFieldsComplete(
                        cameraData = cameraData,
                        onUpdateCameraType = { viewModel.updateCameraType(it) },
                        onUpdateCameraMount = { viewModel.updateCameraMount(it) },
                        onUpdateDirection = { viewModel.updateCameraDirection(it) },
                        onUpdateSurveillance = { viewModel.updateSurveillance(it) },
                        onUpdateOperator = { viewModel.updateOperator(it) },
                        onUpdateOperatorType = { viewModel.updateOperatorType(it) },
                        onUpdateZone = { viewModel.updateSurveillanceZone(it) },
                        onUpdateDescription = { viewModel.updateDescription(it) },
                        onUpdateLevel = { viewModel.updateLevel(it) },
                        onUpdateHeight = { viewModel.updateHeight(it) }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Error message
                if (uiState is AddCameraUiState.Error) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = (uiState as AddCameraUiState.Error).message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Publish button
                Button(
                    onClick = { viewModel.publishCamera() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState !is AddCameraUiState.Publishing
                ) {
                    if (uiState is AddCameraUiState.Publishing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.add_camera_publishing))
                    } else {
                        Text(stringResource(R.string.add_camera_publish))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailedModeFieldsComplete(
    cameraData: CameraFormData,
    onUpdateCameraType: (String?) -> Unit,
    onUpdateCameraMount: (String?) -> Unit,
    onUpdateDirection: (String?) -> Unit,
    onUpdateSurveillance: (String?) -> Unit,
    onUpdateOperator: (String?) -> Unit,
    onUpdateOperatorType: (String?) -> Unit,
    onUpdateZone: (String?) -> Unit,
    onUpdateDescription: (String?) -> Unit,
    onUpdateLevel: (String?) -> Unit,
    onUpdateHeight: (String?) -> Unit
) {
    // Camera Type
    var cameraTypeExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = cameraTypeExpanded,
        onExpandedChange = { cameraTypeExpanded = it }
    ) {
        OutlinedTextField(
            value = cameraData.cameraType ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.camera_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cameraTypeExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = cameraTypeExpanded,
            onDismissRequest = { cameraTypeExpanded = false }
        ) {
            CameraTypes.all.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type) },
                    onClick = {
                        onUpdateCameraType(type)
                        cameraTypeExpanded = false
                    }
                )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // Camera Mount
    var cameraMountExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = cameraMountExpanded,
        onExpandedChange = { cameraMountExpanded = it }
    ) {
        OutlinedTextField(
            value = cameraData.cameraMount ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.camera_mount)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cameraMountExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = cameraMountExpanded,
            onDismissRequest = { cameraMountExpanded = false }
        ) {
            CameraMounts.all.forEach { mount ->
                DropdownMenuItem(
                    text = { Text(mount) },
                    onClick = {
                        onUpdateCameraMount(mount)
                        cameraMountExpanded = false
                    }
                )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // Direction
    OutlinedTextField(
        value = cameraData.cameraDirection ?: "",
        onValueChange = onUpdateDirection,
        label = { Text(stringResource(R.string.camera_direction)) },
        placeholder = { Text(stringResource(R.string.camera_direction_hint)) },
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // Operator
    OutlinedTextField(
        value = cameraData.operator ?: "",
        onValueChange = onUpdateOperator,
        label = { Text(stringResource(R.string.operator)) },
        placeholder = { Text(stringResource(R.string.operator_hint)) },
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // Surveillance Type dropdown
    var surveillanceExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = surveillanceExpanded,
        onExpandedChange = { surveillanceExpanded = it }
    ) {
        OutlinedTextField(
            value = cameraData.surveillance ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.surveillance_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = surveillanceExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = surveillanceExpanded,
            onDismissRequest = { surveillanceExpanded = false }
        ) {
            SurveillanceTypes.all.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type) },
                    onClick = {
                        onUpdateSurveillance(type)
                        surveillanceExpanded = false
                    }
                )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // Surveillance Zone dropdown
    var zoneExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = zoneExpanded,
        onExpandedChange = { zoneExpanded = it }
    ) {
        OutlinedTextField(
            value = cameraData.surveillanceZone ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.surveillance_zone)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoneExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = zoneExpanded,
            onDismissRequest = { zoneExpanded = false }
        ) {
            SurveillanceZones.all.forEach { zone ->
                DropdownMenuItem(
                    text = { Text(zone) },
                    onClick = {
                        onUpdateZone(zone)
                        zoneExpanded = false
                    }
                )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // Operator Type dropdown
    var operatorTypeExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = operatorTypeExpanded,
        onExpandedChange = { operatorTypeExpanded = it }
    ) {
        OutlinedTextField(
            value = cameraData.operatorType ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.operator_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = operatorTypeExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = operatorTypeExpanded,
            onDismissRequest = { operatorTypeExpanded = false }
        ) {
            OperatorTypes.all.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type) },
                    onClick = {
                        onUpdateOperatorType(type)
                        operatorTypeExpanded = false
                    }
                )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // Level
    OutlinedTextField(
        value = cameraData.level ?: "",
        onValueChange = onUpdateLevel,
        label = { Text(stringResource(R.string.level)) },
        placeholder = { Text(stringResource(R.string.level_hint)) },
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // Height
    OutlinedTextField(
        value = cameraData.height ?: "",
        onValueChange = onUpdateHeight,
        label = { Text(stringResource(R.string.height)) },
        placeholder = { Text(stringResource(R.string.height_hint)) },
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // Description
    OutlinedTextField(
        value = cameraData.description ?: "",
        onValueChange = onUpdateDescription,
        label = { Text(stringResource(R.string.description)) },
        placeholder = { Text(stringResource(R.string.description_hint)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 6
    )
}


