package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.StrangerMessage
import com.example.data.model.StrangerSession
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StrangerMatchStatus {
  IDLE,
  SEARCHING,
  CONNECTED,
  ENDED
}

class StrangerChatViewModel(
  private val repository: ChatRepository,
  val currentUserId: String
) : ViewModel() {

  private val _matchStatus = MutableStateFlow(StrangerMatchStatus.IDLE)
  val matchStatus = _matchStatus.asStateFlow()

  private val _currentSession = MutableStateFlow<StrangerSession?>(null)
  val currentSession = _currentSession.asStateFlow()

  private val _selectedTopic = MutableStateFlow("All Interests")
  val selectedTopic = _selectedTopic.asStateFlow()

  private var sessionObserverJob: Job? = null

  val strangerMessages: StateFlow<List<StrangerMessage>> = _currentSession
    .flatMapLatest { session ->
      if (session != null && session.id.isNotBlank()) {
        repository.observeStrangerMessages(session.id)
          .catch { e ->
            Log.e(TAG, "Error observing stranger messages", e)
            emit(emptyList())
          }
      } else {
        flowOf(emptyList())
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  fun setTopic(topic: String) {
    _selectedTopic.value = topic
  }

  fun startSearching(userName: String) {
    _matchStatus.value = StrangerMatchStatus.SEARCHING
    viewModelScope.launch {
      val topicFilter = if (_selectedTopic.value == "All Interests") null else _selectedTopic.value
      val result = repository.matchStranger(userName, topicFilter)
      if (result.isSuccess) {
        val session = result.getOrThrow()
        _currentSession.value = session
        if (session.status == "connected") {
          _matchStatus.value = StrangerMatchStatus.CONNECTED
        } else {
          _matchStatus.value = StrangerMatchStatus.SEARCHING
        }
        observeActiveSession(session.id)
      } else {
        _matchStatus.value = StrangerMatchStatus.IDLE
        Log.e(TAG, "Failed to match stranger", result.exceptionOrNull())
      }
    }
  }

  private fun observeActiveSession(sessionId: String) {
    sessionObserverJob?.cancel()
    sessionObserverJob = viewModelScope.launch {
      repository.observeStrangerSession(sessionId).collect { session ->
        if (session != null) {
          _currentSession.value = session
          when (session.status) {
            "connected" -> _matchStatus.value = StrangerMatchStatus.CONNECTED
            "ended" -> _matchStatus.value = StrangerMatchStatus.ENDED
            "waiting" -> _matchStatus.value = StrangerMatchStatus.SEARCHING
          }
        } else {
          _matchStatus.value = StrangerMatchStatus.ENDED
        }
      }
    }
  }

  fun sendMessage(text: String) {
    val session = _currentSession.value ?: return
    if (text.isBlank() || session.status != "connected") return
    viewModelScope.launch {
      repository.sendStrangerMessage(session.id, text)
    }
  }

  fun disconnect() {
    val session = _currentSession.value
    if (session != null) {
      viewModelScope.launch {
        repository.endStrangerSession(session.id)
      }
    }
    sessionObserverJob?.cancel()
    _currentSession.value = null
    _matchStatus.value = StrangerMatchStatus.IDLE
  }

  fun nextStranger(userName: String) {
    disconnect()
    startSearching(userName)
  }

  override fun onCleared() {
    super.onCleared()
    sessionObserverJob?.cancel()
  }

  private companion object {
    const val TAG = "StrangerChatVM"
  }
}
