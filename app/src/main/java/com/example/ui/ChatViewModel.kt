package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

class ChatViewModel(
  private val repository: ChatRepository,
  val currentUserId: String
) : ViewModel() {

  private val _selectedRoomId = MutableStateFlow("global")
  val selectedRoomId = _selectedRoomId.asStateFlow()

  val rooms: StateFlow<UiState<List<ChatRoom>>> = repository.observeRooms()
    .map<List<ChatRoom>, UiState<List<ChatRoom>>> { UiState.Success(it) }
    .catch { error ->
      Log.e(TAG, "Error observing rooms", error)
      emit(UiState.Error(error.localizedMessage ?: "Failed to load chat channels"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = UiState.Loading
    )

  val messages: StateFlow<UiState<List<ChatMessage>>> = _selectedRoomId
    .flatMapLatest { roomId ->
      repository.observeMessages(roomId)
        .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
        .catch { error ->
          Log.e(TAG, "Error observing messages in $roomId", error)
          emit(UiState.Error(error.localizedMessage ?: "Failed to load messages"))
        }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = UiState.Loading
    )

  private val _replyingTo = MutableStateFlow<ChatMessage?>(null)
  val replyingTo = _replyingTo.asStateFlow()

  private val _sendError = MutableStateFlow<String?>(null)
  val sendError = _sendError.asStateFlow()

  private val icebreakers = listOf(
    "What's the best piece of advice you've ever received?",
    "If you could teleport anywhere in the universe right now, where would you go?",
    "What is your ultimate comfort food?",
    "What is an unpopular opinion that you will defend to the grave?",
    "What song are you listening to on repeat this week?",
    "If you could have dinner with anyone in history, who would it be?",
    "What's the weirdest dream you've ever remembered?",
    "Early bird or night owl? Why?",
    "What's a hobby you've always wanted to try but haven't yet?",
    "What made you smile today?"
  )

  init {
    viewModelScope.launch {
      repository.ensureDefaultRoomsExist()
    }
  }

  fun selectRoom(roomId: String) {
    _selectedRoomId.value = roomId
    _replyingTo.value = null
  }

  fun setReplyingTo(message: ChatMessage?) {
    _replyingTo.value = message
  }

  fun getRandomIcebreaker(): String {
    return icebreakers.random()
  }

  fun sendMessage(text: String, senderName: String, senderAvatar: String = "avatar_1") {
    if (text.isBlank()) return
    val currentReplying = _replyingTo.value
    viewModelScope.launch {
      val result = repository.sendMessage(
        roomId = _selectedRoomId.value,
        text = text,
        senderName = senderName,
        senderAvatar = senderAvatar,
        replyToText = currentReplying?.text,
        replyToSender = currentReplying?.senderName
      )
      if (result.isSuccess) {
        _replyingTo.value = null
        _sendError.value = null
      } else {
        _sendError.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to send message"
      }
    }
  }

  fun deleteMessage(messageId: String) {
    viewModelScope.launch {
      repository.deleteMessage(_selectedRoomId.value, messageId)
    }
  }

  fun clearError() {
    _sendError.value = null
  }

  private companion object {
    const val TAG = "ChatViewModel"
  }
}
