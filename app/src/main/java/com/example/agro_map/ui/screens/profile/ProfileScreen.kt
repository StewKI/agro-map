package com.example.agro_map.ui.screens.profile

import androidx.compose.foundation.layout.*
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

@Composable
fun ProfileScreen(
    container: AppContainer,
    onLogout: () -> Unit
) {
    var user by remember { mutableStateOf<User?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val uid = container.authRepository.currentUser?.uid ?: return@LaunchedEffect
        user = container.userRepository.getUser(uid)
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val u = user ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        if (u.photoUrl.isNotBlank()) {
            AsyncImage(
                model = u.photoUrl,
                contentDescription = "Profile photo",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(16.dp))
        }

        Text(u.username, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(u.fullName, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(4.dp))
        Text(u.phone, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        Card {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${u.points}", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Text("Points", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                container.authRepository.signOut()
                onLogout()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
    }
}
