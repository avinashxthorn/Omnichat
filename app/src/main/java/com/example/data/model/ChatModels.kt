package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ChatRoom(
  @DocumentId val id: String = "",
  val name: String = "",
  val description: String = "",
  val category: String = "general",
  val isPublic: Boolean = true,
  val lastMessageText: String? = null,
  @ServerTimestamp val lastMessageTime: Timestamp? = null,
  val lastMessageSender: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
  @DocumentId val id: String = "",
  val roomId: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val senderAvatar: String = "bot_1",
  val text: String = "",
  val replyToText: String? = null,
  val replyToSender: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StrangerSession(
  @DocumentId val id: String = "",
  val status: String = "waiting", // "waiting", "connected", "ended"
  val user1Id: String = "",
  val user1Name: String = "",
  val user2Id: String? = null,
  val user2Name: String? = null,
  val topic: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val connectedAt: Timestamp? = null,
  @ServerTimestamp val endedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StrangerMessage(
  @DocumentId val id: String = "",
  val sessionId: String = "",
  val senderId: String = "",
  val text: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val avatar: String = "avatar_1",
  val customBio: String = "",
  val isOnline: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)
