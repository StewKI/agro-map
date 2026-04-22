package com.example.agro_map.data.repository

import com.example.agro_map.data.model.Comment
import com.example.agro_map.data.model.Report
import com.example.agro_map.data.model.ReportCategory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ReportRepository {
    private val db = FirebaseFirestore.getInstance()
    private val reportsCollection = db.collection("reports")

    suspend fun createReport(report: Report): String {
        val doc = reportsCollection.add(
            mapOf(
                "title" to report.title,
                "description" to report.description,
                "category" to report.category.name,
                "photoUrl" to report.photoUrl,
                "latitude" to report.latitude,
                "longitude" to report.longitude,
                "authorId" to report.authorId,
                "authorUsername" to report.authorUsername,
                "createdAt" to report.createdAt
            )
        ).await()
        return doc.id
    }

    suspend fun getReport(id: String): Report? {
        val doc = reportsCollection.document(id).get().await()
        if (!doc.exists()) return null
        return docToReport(doc)
    }

    suspend fun getAllReports(): List<Report> {
        val snapshot = reportsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
        return snapshot.documents.mapNotNull { docToReport(it) }
    }

    fun listenToReports(onUpdate: (List<Report>) -> Unit): ListenerRegistration {
        return reportsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val reports = snapshot.documents.mapNotNull { docToReport(it) }
                    onUpdate(reports)
                }
            }
    }

    suspend fun addComment(reportId: String, comment: Comment) {
        reportsCollection.document(reportId).collection("comments").add(
            mapOf(
                "text" to comment.text,
                "rating" to comment.rating,
                "authorId" to comment.authorId,
                "authorUsername" to comment.authorUsername,
                "createdAt" to comment.createdAt
            )
        ).await()
    }

    fun listenToComments(reportId: String, onUpdate: (List<Comment>) -> Unit): ListenerRegistration {
        return reportsCollection.document(reportId).collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val comments = snapshot.documents.map { doc ->
                        Comment(
                            id = doc.id,
                            text = doc.getString("text") ?: "",
                            rating = doc.getLong("rating")?.toInt(),
                            authorId = doc.getString("authorId") ?: "",
                            authorUsername = doc.getString("authorUsername") ?: "",
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    }
                    onUpdate(comments)
                }
            }
    }

    private fun docToReport(doc: com.google.firebase.firestore.DocumentSnapshot): Report? {
        return try {
            Report(
                id = doc.id,
                title = doc.getString("title") ?: "",
                description = doc.getString("description") ?: "",
                category = try {
                    ReportCategory.valueOf(doc.getString("category") ?: "OTHER")
                } catch (_: Exception) {
                    ReportCategory.OTHER
                },
                photoUrl = doc.getString("photoUrl") ?: "",
                latitude = doc.getDouble("latitude") ?: 0.0,
                longitude = doc.getDouble("longitude") ?: 0.0,
                authorId = doc.getString("authorId") ?: "",
                authorUsername = doc.getString("authorUsername") ?: "",
                createdAt = doc.getLong("createdAt") ?: 0L
            )
        } catch (_: Exception) {
            null
        }
    }
}
