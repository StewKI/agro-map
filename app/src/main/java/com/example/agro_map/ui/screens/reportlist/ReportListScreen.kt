package com.example.agro_map.ui.screens.reportlist

import android.location.Location
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.agro_map.AppContainer
import com.example.agro_map.data.model.Report
import com.example.agro_map.data.model.ReportCategory
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportListScreen(
    container: AppContainer,
    onReportClick: (String) -> Unit
) {
    var reports by remember { mutableStateOf<List<Report>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ReportCategory?>(null) }
    var radiusKm by remember { mutableFloatStateOf(0f) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var userLat by remember { mutableDoubleStateOf(0.0) }
    var userLng by remember { mutableDoubleStateOf(0.0) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val context = LocalContext.current

    // Listen to reports
    DisposableEffect(Unit) {
        val listener = container.reportRepository.listenToReports { reports = it }
        onDispose { listener.remove() }
    }

    // Get user location for radius filter
    LaunchedEffect(Unit) {
        try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            @SuppressWarnings("MissingPermission")
            val location = client.lastLocation.await()
            if (location != null) {
                userLat = location.latitude
                userLng = location.longitude
            }
        } catch (_: Exception) {}
    }

    val filteredReports = reports.filter { report ->
        val matchesSearch = searchQuery.isBlank() ||
                report.title.contains(searchQuery, ignoreCase = true) ||
                report.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == null || report.category == selectedCategory
        val matchesRadius = if (radiusKm > 0 && userLat != 0.0) {
            val results = FloatArray(1)
            Location.distanceBetween(userLat, userLng, report.latitude, report.longitude, results)
            results[0] / 1000f <= radiusKm
        } else true
        matchesSearch && matchesCategory && matchesRadius
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Reports", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by title or description...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        // Filters row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Category filter
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedCategory?.name ?: "All",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    DropdownMenuItem(text = { Text("All") }, onClick = { selectedCategory = null; categoryExpanded = false })
                    ReportCategory.entries.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat.name) }, onClick = { selectedCategory = cat; categoryExpanded = false })
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        // Radius slider
        Text("Radius: ${if (radiusKm == 0f) "Off" else "%.1f km".format(radiusKm)}", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = radiusKm,
            onValueChange = { radiusKm = it },
            valueRange = 0f..50f,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        Text("${filteredReports.size} results", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(4.dp))

        LazyColumn {
            items(filteredReports) { report ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onReportClick(report.id) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(report.title, style = MaterialTheme.typography.titleMedium)
                            AssistChip(onClick = {}, label = { Text(report.category.name) })
                        }
                        Text(
                            "By ${report.authorUsername} - ${dateFormat.format(Date(report.createdAt))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
