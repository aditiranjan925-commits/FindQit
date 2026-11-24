
package com.aditi.lostfoundindia

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.security.MessageDigest

data class ChatRoom(
    val id: String = "",
    val itemId: String = "",
    val itemTitle: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val updatedAt: Long = 0L
)

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun currentUid(): String? {
        return auth.currentUser?.uid
    }

    private fun makeChatId(
        itemId: String,
        uid1: String,
        uid2: String
    ): String {
        val users = listOf(uid1, uid2).sorted()
        val raw = "${itemId}_${users[0]}_${users[1]}"

        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(raw.toByteArray())

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }

    fun openChat(
        item: FindQitItem,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = currentUid()

        if (uid == null) {
            onError("Please login again.")
            return
        }

        if (item.ownerId.isBlank()) {
            onError("Owner information is unavailable.")
            return
        }

        if (uid == item.ownerId) {
            onError("You cannot chat with yourself.")
            return
        }

        val chatId = makeChatId(
            item.id,
            uid,
            item.ownerId
        )

        val chatRef = db.collection("chatRooms")
            .document(chatId)

        chatRef.get()
            .addOnSuccessListener { document ->

                if (document.exists()) {
                    onSuccess(chatId)
                } else {
                    val room = hashMapOf(
                        "itemId" to item.id,
                        "itemTitle" to item.title,
                        "participants" to listOf(uid, item.ownerId),
                        "lastMessage" to "",
                        "lastSenderId" to "",
                        "updatedAt" to System.currentTimeMillis()
                    )

                    chatRef.set(room)
                        .addOnSuccessListener {
                            onSuccess(chatId)
                        }
                        .addOnFailureListener {
                            onError(it.message ?: "Could not create chat.")
                        }
                }
            }
            .addOnFailureListener {
                onError(it.message ?: "Could not open chat.")
            }
    }

    fun observeMyChats(
        onData: (List<ChatRoom>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {

        val uid = currentUid() ?: return null

        return db.collection("chatRooms")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error.message ?: "Could not load chats.")
                    return@addSnapshotListener
                }

                val rooms = snapshot?.documents?.mapNotNull {
                    it.toObject(ChatRoom::class.java)?.copy(id = it.id)
                } ?: emptyList()

                onData(rooms.sortedByDescending { it.updatedAt })
            }
    }

    fun observeMessages(
        chatId: String,
        onData: (List<ChatMessage>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {

        return db.collection("chatRooms")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error.message ?: "Could not load messages.")
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull {
                    it.toObject(ChatMessage::class.java)?.copy(id = it.id)
                } ?: emptyList()

                onData(messages)
            }
    }

    fun sendMessage(
        chatId: String,
        message: String,
        onError: (String) -> Unit
    ) {
        val uid = currentUid()

        if (uid == null) {
            onError("Please login again.")
            return
        }

        val cleanMessage = message.trim()

        if (cleanMessage.isBlank()) return

        val chatRef = db.collection("chatRooms")
            .document(chatId)

        val messageData = hashMapOf(
            "senderId" to uid,
            "text" to cleanMessage,
            "timestamp" to System.currentTimeMillis()
        )

        db.runBatch { batch ->

            val messageRef = chatRef
                .collection("messages")
                .document()

            batch.set(messageRef, messageData)

            batch.update(
                chatRef,
                mapOf(
                    "lastMessage" to cleanMessage,
                    "lastSenderId" to uid,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
        }.addOnFailureListener {
            onError(it.message ?: "Message could not be sent.")
        }
    }
}
