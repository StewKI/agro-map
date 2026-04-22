package com.example.agro_map.ui.screens.addreport

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.agro_map.AppContainer
import com.example.agro_map.data.model.Report
import com.example.agro_map.data.model.ReportCategory
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReportScreen(
    container: AppContainer,
    onReportAdded: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ReportCategory.OTHER) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) photoUri = cameraUri
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { photoUri = it }
    }
    var showImagePicker by remember { mutableStateOf(false) }

    // Get location
    LaunchedEffect(Unit) {
        try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            @SuppressWarnings("MissingPermission")
            val location = client.lastLocation.await()
            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude
            }
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("New Report", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Photo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .clickable { showImagePicker = true },
            contentAlignment = Alignment.Center
        ) {
            if (photoUri != null) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Report photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(48.dp))
                    Text("Tap to add photo")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        // Category dropdown
        ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
            OutlinedTextField(
                value = category.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                ReportCategory.entries.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.name) },
                        onClick = { category = cat; categoryExpanded = false }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (latitude != 0.0) {
            Text("Location: %.4f, %.4f".format(latitude, longitude), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                loading = true
                error = null
                scope.launch {
                    try {
                        val user = container.authRepository.currentUser!!
                        val userData = container.userRepository.getUser(user.uid)

                        val reportId = container.reportRepository.createReport(
                            Report(
                                title = title,
                                description = description,
                                category = category,
                                latitude = latitude,
                                longitude = longitude,
                                authorId = user.uid,
                                authorUsername = userData?.username ?: "Unknown",
                                createdAt = System.currentTimeMillis()
                            )
                        )

                        if (photoUri != null) {
                            val photoUrl = container.storageRepository.uploadReportPhoto(reportId, photoUri!!)
                            // Update report with photo URL
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("reports").document(reportId)
                                .update("photoUrl", photoUrl).await()
                        }

                        container.userRepository.addPoints(user.uid, 10)
                        onReportAdded()
                    } catch (e: Exception) {
                        error = e.message
                    }
                    loading = false
                }
            },
            enabled = !loading && title.isNotBlank() && description.isNotBlank() && photoUri != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
            else Text("Submit Report")
        }
    }

    if (showImagePicker) {
        AlertDialog(
            onDismissRequest = { showImagePicker = false },
            title = { Text("Choose photo") },
            text = {
                Column {
                    TextButton(onClick = {
                        showImagePicker = false
                        galleryLauncher.launch("image/*")
                    }) { Text("Pick from Gallery") }
                    TextButton(onClick = {
                        showImagePicker = false
                        val file = File(context.cacheDir, "report_photo.jpg")
                        cameraUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        cameraLauncher.launch(cameraUri!!)
                    }) { Text("Take Photo") }
                }
            },
            confirmButton = {}
        )
    }
}
