package com.example.agro_map.data.repository

import com.example.agro_map.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    suspend fun createUser(user: User) {
        usersCollection.document(user.uid).set(
            mapOf(
                "username" to user.username,
                "fullName" to user.fullName,
                "phone" to user.phone,
                "photoUrl" to user.photoUrl,
                "points" to user.points,
                "latitude" to user.latitude,
                "longitude" to user.longitude
            )
        ).await()
    }

    suspend fun getUser(uid: String): User? {
        val doc = usersCollection.document(uid).get().await()
        if (!doc.exists()) return null
        return User(
            uid = uid,
            username = doc.getString("username") ?: "",
            fullName = doc.getString("fullName") ?: "",
            phone = doc.getString("phone") ?: "",
            photoUrl = doc.getString("photoUrl") ?: "",
            points = doc.getLong("points")?.toInt() ?: 0,
            latitude = doc.getDouble("latitude") ?: 0.0,
            longitude = doc.getDouble("longitude") ?: 0.0
        )
    }

    suspend fun updateLocation(uid: String, lat: Double, lng: Double) {
        usersCollection.document(uid).update(
            mapOf("latitude" to lat, "longitude" to lng)
        ).await()
    }

    suspend fun addPoints(uid: String, points: Int) {
        val user = getUser(uid) ?: return
        usersCollection.document(uid).update("points", user.points + points).await()
    }

    suspend fun getLeaderboard(): List<User> {
        val snapshot = usersCollection
            .orderBy("points", Query.Direction.DESCENDING)
            .get().await()
        return snapshot.documents.map { doc ->
            User(
                uid = doc.id,
                username = doc.getString("username") ?: "",
                fullName = doc.getString("fullName") ?: "",
                phone = doc.getString("phone") ?: "",
                photoUrl = doc.getString("photoUrl") ?: "",
                points = doc.getLong("points")?.toInt() ?: 0
            )
        }
    }
}
