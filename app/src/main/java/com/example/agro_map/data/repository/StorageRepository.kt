package com.example.agro_map.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class StorageRepository {
    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadProfilePhoto(uid: String, imageUri: Uri): String {
        val ref = storage.reference.child("profile_photos/$uid.jpg")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun uploadReportPhoto(reportId: String, imageUri: Uri): String {
        val ref = storage.reference.child("report_photos/$reportId.jpg")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }
}
