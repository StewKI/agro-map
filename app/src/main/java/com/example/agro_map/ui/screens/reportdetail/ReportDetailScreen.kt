package com.example.agro_map.ui.screens.reportdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.agro_map.AppContainer
import com.example.agro_map.data.model.Comment
import com.example.agro_map.data.model.Report
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    container: AppContainer,
    reportId: String,
    onBack: () -> Unit
) {
    var report by remember { mutableStateOf<Report?>(null) }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }
    var selectedRating by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()) }

    // Load report
    LaunchedEffect(reportId) {
        report = container.reportRepository.getReport(reportId)
        loading = false
    }

    // Listen to comments
    DisposableEffect(reportId) {
        val listener = container.reportRepository.listenToComments(reportId) { comments = it }
        onDispose { listener.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(report?.title ?: "Report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val r = report ?: return@Scaffold

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Photo
            if (r.photoUrl.isNotBlank()) {
                item {
                    AsyncImage(
                        model = r.photoUrl,
                        contentDescription = "Report photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Info
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    AssistChip(onClick = {}, label = { Text(r.category.name) })
                    Spacer(Modifier.height(8.dp))
                    Text(r.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(r.description, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "By ${r.authorUsername} on ${dateFormat.format(Date(r.createdAt))}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Comments (${comments.size})", style = MaterialTheme.typography.titleMedium)
                }
            }

            // Comments
            items(comments) { comment ->
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(comment.authorUsername, style = MaterialTheme.typography.labelLarge)
                        if (comment.rating != null) {
                            Spacer(Modifier.width(8.dp))
                            repeat(comment.rating) {
                                Icon(Icons.Default.Star, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                    Text(dateFormat.format(Date(comment.createdAt)), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                }
            }

            // Add comment
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    // Star rating
                    Row {
                        Text("Rating: ", style = MaterialTheme.typography.bodyMedium)
                        (1..5).forEach { star ->
                            IconButton(onClick = {
                                selectedRating = if (selectedRating == star) 0 else star
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    if (star <= selectedRating) Icons.Default.Star else Icons.Outlined.StarOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Add a comment...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val user = container.authRepository.currentUser ?: return@launch
                                    val userData = container.userRepository.getUser(user.uid)
                                    container.reportRepository.addComment(
                                        reportId,
                                        Comment(
                                            text = commentText,
                                            rating = if (selectedRating > 0) selectedRating else null,
                                            authorId = user.uid,
                                            authorUsername = userData?.username ?: "Unknown",
                                            createdAt = System.currentTimeMillis()
                                        )
                                    )
                                    container.userRepository.addPoints(user.uid, 2)
                                    commentText = ""
                                    selectedRating = 0
                                }
                            },
                            enabled = commentText.isNotBlank()
                        ) {
                            Icon(Icons.Default.Send, "Send")
                        }
                    }
                    Spacer(Modifier.height(80.dp)) // padding for bottom nav
                }
            }
        }
    }
}
