package com.example.agro_map.ui.screens.leaderboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.agro_map.AppContainer
import com.example.agro_map.data.model.User
import kotlinx.coroutines.launch

@Composable
fun LeaderboardScreen(container: AppContainer) {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val currentUid = container.authRepository.currentUser?.uid
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        users = container.userRepository.getLeaderboard()
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Leaderboard", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn {
                itemsIndexed(users) { index, user ->
                    val isCurrentUser = user.uid == currentUid
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = if (isCurrentUser) CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) else CardDefaults.cardColors()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "#${index + 1}",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.width(48.dp)
                            )
                            if (user.photoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = user.photoUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.username, style = MaterialTheme.typography.titleMedium)
                                Text(user.fullName, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "${user.points} pts",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
