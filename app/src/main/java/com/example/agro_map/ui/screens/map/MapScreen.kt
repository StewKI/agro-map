package com.example.agro_map.ui.screens.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.agro_map.AppContainer
import com.example.agro_map.data.model.Report
import com.example.agro_map.service.LocationService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun MapScreen(
    container: AppContainer,
    onAddReport: () -> Unit,
    onReportClick: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reports by remember { mutableStateOf<List<Report>>(emptyList()) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPositionState = rememberCameraPositionState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

        // Also request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Notification permission handled separately if needed
        }
    }

    // Request permissions on first launch
    LaunchedEffect(Unit) {
        val permsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permsToRequest.toTypedArray())
    }

    // Listen to reports
    DisposableEffect(Unit) {
        val listener = container.reportRepository.listenToReports { reports = it }
        onDispose { listener.remove() }
    }

    // Get location and start service once we have permission
    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@LaunchedEffect
        try {
            // Start location service
            val serviceIntent = Intent(context, LocationService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)

            // Center map on user
            val client = LocationServices.getFusedLocationProviderClient(context)
            val location = client.lastLocation.await()
            if (location != null) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.fromLatLngZoom(LatLng(location.latitude, location.longitude), 14f)
                    )
                )
            }
        } catch (_: Exception) {}
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
        ) {
            reports.forEach { report ->
                val position = LatLng(report.latitude, report.longitude)
                Marker(
                    state = MarkerState(position = position),
                    title = report.title,
                    snippet = "${report.category} - ${report.authorUsername}",
                    onInfoWindowClick = { onReportClick(report.id) }
                )
            }
        }

        FloatingActionButton(
            onClick = onAddReport,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Report")
        }
    }
}
