
package com.aditi.lostfoundindia

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

data class FindQitItem(
    val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val description: String = "",
    val college: String = "",
    val category: String = "Others",
    val status: String = "LOST",
    val imageUrl: String = "",
    val createdAt: Long = 0L
)

class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val itemsCollection = db.collection("items")

    // Get current logged-in user
    private fun currentUserId(): String? {
        return auth.currentUser?.uid
    }

    // 1. Post Lost or Found Item
    fun postItem(
        title: String,
        description: String,
        college: String,
        category: String,
        status: String,
        imageUrl: String = "",
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        val uid = currentUserId()

        if (uid == null) {
            onError("Please login first")
            return
        }

        if (status !in listOf("LOST", "FOUND")) {
            onError("Invalid item status")
            return
        }

        val item = hashMapOf(
            "ownerId" to uid,
            "title" to title,
            "description" to description,
            "college" to college,
            "category" to category,
            "status" to status,
            "imageUrl" to imageUrl,
            "createdAt" to FieldValue.serverTimestamp()
        )

        itemsCollection
            .add(item)
            .addOnSuccessListener { document ->
                onSuccess(document.id)
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Upload failed")
            }
    }

    // 2. Listen to Active Lost and Found Items
    fun observeActiveItems(
        onData: (List<FindQitItem>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {

        return itemsCollection
            .whereIn("status", listOf("LOST", "FOUND"))
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error.message ?: "Loading failed")
                    return@addSnapshotListener
                }

                val result = snapshot?.documents?.mapNotNull {
                    document ->

                    val created = document.getTimestamp("createdAt")
                        ?.toDate()?.time ?: 0L

                    FindQitItem(
                        id = document.id,
                        ownerId = document.getString("ownerId") ?: "",
                        title = document.getString("title") ?: "",
                        description = document.getString("description") ?: "",
                        college = document.getString("college") ?: "",
                        category = document.getString("category") ?: "Others",
                        status = document.getString("status") ?: "",
                        imageUrl = document.getString("imageUrl") ?: "",
                        createdAt = created
                    )
                } ?: emptyList()

                onData(result)
            }
    }

    // 3. Mark Item as Solved
    // Item remains in Firebase for history.
    fun markSolved(
        itemId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val uid = currentUserId()

        if (uid == null) {
            onError("Please login first")
            return
        }

        itemsCollection.document(itemId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    onError("Item does not exist")
                    return@addOnSuccessListener
                }

                if (document.getString("ownerId") != uid) {
                    onError("Only item owner can solve it")
                    return@addOnSuccessListener
                }

                val status = document.getString("status")

                if (status !in listOf("LOST", "FOUND")) {
                    onError("Item is already solved")
                    return@addOnSuccessListener
                }

                itemsCollection.document(itemId)
                    .update(
                        mapOf(
                            "status" to "SOLVED",
                            "solvedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { error ->
                        onError(error.message ?: "Update failed")
                    }
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Could not verify item")
            }
    }

    // 4. Load My Solved History
    fun observeSolvedHistory(
        onData: (List<FindQitItem>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {

        val uid = currentUserId()

        if (uid == null) {
            onError("Please login first")
            return db.collection("items").addSnapshotListener { _, _ -> }
        }

        return itemsCollection
            .whereEqualTo("ownerId", uid)
            .whereEqualTo("status", "SOLVED")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error.message ?: "History loading failed")
                    return@addSnapshotListener
                }

                val result = snapshot?.documents?.mapNotNull {
                    document ->

                    FindQitItem(
                        id = document.id,
                        ownerId = document.getString("ownerId") ?: "",
                        title = document.getString("title") ?: "",
                        description = document.getString("description") ?: "",
                        college = document.getString("college") ?: "",
                        category = document.getString("category") ?: "Others",
                        status = document.getString("status") ?: "",
                        imageUrl = document.getString("imageUrl") ?: ""
                    )
                } ?: emptyList()

                onData(result)
            }
    }

    // 5. Load My Own Active Items
    fun observeMyItems(
        onData: (List<FindQitItem>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {

        val uid = currentUserId()

        if (uid == null) {
            onError("Please login first")
            return db.collection("items").addSnapshotListener { _, _ -> }
        }

        return itemsCollection
            .whereEqualTo("ownerId", uid)
            .whereIn("status", listOf("LOST", "FOUND"))
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error.message ?: "Loading failed")
                    return@addSnapshotListener
                }

                val result = snapshot?.documents?.mapNotNull {
                    document ->
                    FindQitItem(
                        id = document.id,
                        ownerId = document.getString("ownerId") ?: "",
                        title = document.getString("title") ?: "",
                        description = document.getString("description") ?: "",
                        college = document.getString("college") ?: "",
                        category = document.getString("category") ?: "Others",
                        status = document.getString("status") ?: "",
                        imageUrl = document.getString("imageUrl") ?: ""
                    )
                } ?: emptyList()

                onData(result)
            }
    }
}
