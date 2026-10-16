package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ChatMessage
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ChatRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun sendMessage_authenticatedUser_successAndEmitted() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = ChatRepository(firestore)

    val testMsgText = "Test message from Alice: ${UUID.randomUUID()}"
    val sendResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.sendMessage(
        roomId = "global",
        text = testMsgText,
        senderName = "Alice",
        senderAvatar = "avatar_1"
      )
    }
    assertTrue(sendResult.isSuccess)
    val msgId = sendResult.getOrThrow()
    assertNotNull(msgId)
    assertTrue(msgId.isNotEmpty())

    val emittedMessages = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeMessages("global").first { list: List<ChatMessage> ->
        list.any { it.id == msgId }
      }
    }
    assertTrue(emittedMessages.any { it.id == msgId })
  }

  @Test
  fun deleteMessage_crossUser_failsWithPermissionDenied() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val aliceRepo = ChatRepository(firestore)

    val sendResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.sendMessage(
        roomId = "global",
        text = "Alice's immutable message",
        senderName = "Alice"
      )
    }
    val msgId = sendResult.getOrThrow()

    // Sign in as Bob and attempt to delete Alice's message
    signInTestUser(BOB_EMAIL)
    val bobRepo = ChatRepository(firestore)

    val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.deleteMessage("global", msgId)
    }
    assertTrue(deleteResult.isFailure)
  }

  @Test
  fun saveUserProfile_authenticatedOwner_updatesSuccessfully() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = ChatRepository(firestore)

    val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.saveUserProfile(
        displayName = "Cosmic Traveler",
        avatar = "avatar_2",
        bio = "Exploring the universal airwaves"
      )
    }
    assertTrue(saveResult.isSuccess)
  }

  @Test
  fun sendMessage_unauthenticatedUser_fails() = runBlocking {
    auth.signOut()
    val repository = ChatRepository(firestore)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.sendMessage(
        roomId = "global",
        text = "Unauth attempt",
        senderName = "Ghost"
      )
    }
    assertTrue(result.isFailure)
  }

  private companion object {
    const val ALICE_EMAIL = "alice@test.com"
    const val BOB_EMAIL = "bob@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
