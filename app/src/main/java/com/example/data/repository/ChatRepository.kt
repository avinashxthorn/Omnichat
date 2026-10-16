package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.example.data.model.StrangerMessage
import com.example.data.model.StrangerSession
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  fun currentUserId(): String? = auth.currentUser?.uid

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google to access chat.")
  }

  // --- UNIVERSAL ROOMS & MESSAGES ---

  fun observeRooms(): Flow<List<ChatRoom>> {
    return db.collection("rooms")
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(ChatRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, "rooms")
        throw error
      }
  }

  suspend fun ensureDefaultRoomsExist() {
    val defaultRooms = listOf(
      ChatRoom(
        id = "global",
        name = "Universal Lounge",
        description = "World public feed. Messages sent here are instantly visible to everyone.",
        category = "global",
        isPublic = true
      ),
      ChatRoom(
        id = "tech_talk",
        name = "Tech & Dev",
        description = "Coding, gadgets, AI, and futuristic ideas with techies worldwide.",
        category = "interests",
        isPublic = true
      ),
      ChatRoom(
        id = "night_owls",
        name = "Night Owls",
        description = "Late night deep conversations, music picks, and random thoughts.",
        category = "casual",
        isPublic = true
      ),
      ChatRoom(
        id = "gaming",
        name = "Gaming & Esports",
        description = "Squad up, discuss upcoming releases, game clips, and banter.",
        category = "interests",
        isPublic = true
      )
    )

    for (room in defaultRooms) {
      val docRef = db.collection("rooms").document(room.id)
      val snapshot = try {
        docRef.get().await()
      } catch (e: Exception) {
        null
      }
      if (snapshot == null || !snapshot.exists()) {
        try {
          docRef.set(
            mapOf(
              "name" to room.name,
              "description" to room.description,
              "category" to room.category,
              "isPublic" to room.isPublic,
              "createdAt" to FieldValue.serverTimestamp()
            )
          ).await()
        } catch (e: Exception) {
          handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }
      }
    }
  }

  fun observeMessages(roomId: String): Flow<List<ChatMessage>> {
    return db.collection("rooms").document(roomId).collection("messages")
      .orderBy("createdAt", Query.Direction.ASCENDING)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, "rooms/$roomId/messages")
        throw error
      }
  }

  suspend fun sendMessage(
    roomId: String,
    text: String,
    senderName: String,
    senderAvatar: String = "avatar_1",
    replyToText: String? = null,
    replyToSender: String? = null
  ): Result<String> {
    return try {
      val uid = requireUserId()
      val msgId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
      val docRef = db.collection("rooms").document(roomId).collection("messages").document(msgId)

      val payload = mutableMapOf<String, Any>(
        "roomId" to roomId,
        "senderId" to uid,
        "senderName" to senderName.ifBlank { "Stranger" },
        "senderAvatar" to senderAvatar,
        "text" to text.trim(),
        "createdAt" to FieldValue.serverTimestamp()
      )
      if (!replyToText.isNullOrBlank()) payload["replyToText"] = replyToText.take(300)
      if (!replyToSender.isNullOrBlank()) payload["replyToSender"] = replyToSender.take(60)

      docRef.set(payload).await()
      Result.success(msgId)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "rooms/$roomId/messages")
      Result.failure(e)
    }
  }

  suspend fun deleteMessage(roomId: String, messageId: String): Result<Unit> {
    return try {
      val docRef = db.collection("rooms").document(roomId).collection("messages").document(messageId)
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "rooms/$roomId/messages/$messageId")
      Result.failure(e)
    }
  }

  // --- OMEGLE-STYLE 1-ON-1 STRANGER MATCHING ---

  fun observeStrangerSession(sessionId: String): Flow<StrangerSession?> {
    return db.collection("stranger_sessions").document(sessionId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(StrangerSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, "stranger_sessions/$sessionId")
        throw error
      }
  }

  fun observeStrangerMessages(sessionId: String): Flow<List<StrangerMessage>> {
    return db.collection("stranger_sessions").document(sessionId).collection("messages")
      .orderBy("createdAt", Query.Direction.ASCENDING)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(StrangerMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, "stranger_sessions/$sessionId/messages")
        throw error
      }
  }

  suspend fun matchStranger(userName: String, topic: String? = null): Result<StrangerSession> {
    return try {
      val uid = requireUserId()

      // Look for a waiting session not started by this user
      val waitingQuery = db.collection("stranger_sessions")
        .whereEqualTo("status", "waiting")
        .limit(10)
        .get()
        .await()

      val eligibleSessionDoc = waitingQuery.documents.firstOrNull { doc ->
        val user1 = doc.getString("user1Id")
        user1 != null && user1 != uid
      }

      if (eligibleSessionDoc != null) {
        // Join this session as user2
        val sessionId = eligibleSessionDoc.id
        db.collection("stranger_sessions").document(sessionId).update(
          mapOf(
            "status" to "connected",
            "user2Id" to uid,
            "user2Name" to userName.ifBlank { "Stranger" },
            "connectedAt" to FieldValue.serverTimestamp()
          )
        ).await()

        val updated = db.collection("stranger_sessions").document(sessionId).get().await()
        val session = updated.toObject(StrangerSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        Result.success(session ?: StrangerSession(id = sessionId, status = "connected", user1Id = eligibleSessionDoc.getString("user1Id") ?: "", user1Name = eligibleSessionDoc.getString("user1Name") ?: "Stranger", user2Id = uid, user2Name = userName))
      } else {
        // Create a new waiting session
        val newSessionId = "session_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val docRef = db.collection("stranger_sessions").document(newSessionId)
        val payload = mutableMapOf<String, Any>(
          "status" to "waiting",
          "user1Id" to uid,
          "user1Name" to userName.ifBlank { "Stranger" },
          "createdAt" to FieldValue.serverTimestamp()
        )
        if (!topic.isNullOrBlank()) payload["topic"] = topic.take(50)

        docRef.set(payload).await()
        val created = docRef.get().await().toObject(StrangerSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        Result.success(created ?: StrangerSession(id = newSessionId, status = "waiting", user1Id = uid, user1Name = userName))
      }
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "stranger_sessions")
      Result.failure(e)
    }
  }

  suspend fun sendStrangerMessage(sessionId: String, text: String): Result<String> {
    return try {
      val uid = requireUserId()
      val msgId = "smsg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
      val docRef = db.collection("stranger_sessions").document(sessionId).collection("messages").document(msgId)

      docRef.set(
        mapOf(
          "sessionId" to sessionId,
          "senderId" to uid,
          "text" to text.trim(),
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(msgId)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "stranger_sessions/$sessionId/messages")
      Result.failure(e)
    }
  }

  suspend fun endStrangerSession(sessionId: String): Result<Unit> {
    return try {
      db.collection("stranger_sessions").document(sessionId).update(
        mapOf(
          "status" to "ended",
          "endedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "stranger_sessions/$sessionId")
      Result.failure(e)
    }
  }

  // --- USER PROFILE & PRESENCE ---

  fun observeUserProfile(userId: String): Flow<UserProfile?> {
    return db.collection("users").document(userId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, "users/$userId")
        throw error
      }
  }

  suspend fun saveUserProfile(displayName: String, avatar: String, bio: String): Result<Unit> {
    return try {
      val uid = requireUserId()
      val docRef = db.collection("users").document(uid)
      val existingDoc = try { docRef.get().await() } catch (e: Exception) { null }

      if (existingDoc != null && existingDoc.exists()) {
        docRef.update(
          mapOf(
            "displayName" to displayName.trim(),
            "avatar" to avatar,
            "customBio" to bio.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        docRef.set(
          mapOf(
            "userId" to uid,
            "displayName" to displayName.trim(),
            "avatar" to avatar,
            "customBio" to bio.trim(),
            "isOnline" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users")
      Result.failure(e)
    }
  }
}
