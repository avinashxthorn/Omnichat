package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StrangerBubbleBackground
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.UserBubbleGradientEnd
import com.example.ui.theme.UserBubbleGradientStart
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalChatScreen(
  viewModel: ChatViewModel,
  userDisplayName: String,
  userAvatar: String
) {
  val selectedRoomId by viewModel.selectedRoomId.collectAsStateWithLifecycle()
  val roomsState by viewModel.rooms.collectAsStateWithLifecycle()
  val messagesState by viewModel.messages.collectAsStateWithLifecycle()
  val replyingTo by viewModel.replyingTo.collectAsStateWithLifecycle()
  val sendError by viewModel.sendError.collectAsStateWithLifecycle()

  var inputText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(sendError) {
    sendError?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearError()
    }
  }

  // Scroll to bottom when new messages arrive
  LaunchedEffect(messagesState) {
    if (messagesState is UiState.Success) {
      val count = (messagesState as UiState.Success<List<ChatMessage>>).data.size
      if (count > 0) {
        listState.animateScrollToItem(count - 1)
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(BackgroundDark)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Bar
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(NeonCyan, ElectricViolet))),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Forum,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Universal Chat",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "Live Worldwide Feed",
                fontSize = 12.sp,
                color = NeonCyan
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
      )

      // Channel Selector Chips Carousel
      val defaultChannelList = listOf(
        Pair("global", "🌐 Global Lounge"),
        Pair("tech_talk", "⚡ Tech & AI"),
        Pair("night_owls", "🌙 Night Owls"),
        Pair("gaming", "🎮 Gaming & Fun")
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(SurfaceDark.copy(alpha = 0.8f))
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        defaultChannelList.forEach { (id, label) ->
          val isSelected = selectedRoomId == id
          FilterChip(
            selected = isSelected,
            onClick = { viewModel.selectRoom(id) },
            label = {
              Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = ElectricViolet,
              selectedLabelColor = Color.White,
              containerColor = SurfaceElevated,
              labelColor = Color(0xFFCBD5E1)
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = isSelected,
              borderColor = if (isSelected) NeonCyan else Color.Transparent
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.testTag("room_chip_$id")
          )
        }
      }

      // Message Feed Area
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        when (val state = messagesState) {
          is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(color = NeonCyan)
            }
          }
          is UiState.Error -> {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = state.message,
                color = Color(0xFFF87171),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
          is UiState.Success -> {
            val msgList = state.data
            if (msgList.isEmpty()) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(24.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(
                    imageVector = Icons.Default.Forum,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(56.dp)
                  )
                  Spacer(modifier = Modifier.height(12.dp))
                  Text(
                    text = "No messages yet in this channel",
                    color = Color(0xFF94A3B8),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                  )
                  Text(
                    text = "Be the first to say hello to everyone worldwide!",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                  )
                }
              }
            } else {
              LazyColumn(
                state = listState,
                modifier = Modifier
                  .fillMaxSize()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                items(msgList, key = { it.id }) { msg ->
                  val isMe = msg.senderId == viewModel.currentUserId
                  ChatMessageItem(
                    message = msg,
                    isMe = isMe,
                    onReply = { viewModel.setReplyingTo(msg) },
                    onDelete = { viewModel.deleteMessage(msg.id) }
                  )
                }
              }
            }
          }
        }
      }

      // Reply Preview Banner
      AnimatedVisibility(
        visible = replyingTo != null,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        replyingTo?.let { replyTarget ->
          Surface(
            color = SurfaceElevated,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Reply,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Replying to ${replyTarget.senderName}",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = NeonCyan
                )
                Text(
                  text = replyTarget.text,
                  fontSize = 12.sp,
                  color = Color(0xFFCBD5E1),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              IconButton(
                onClick = { viewModel.setReplyingTo(null) },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Cancel reply",
                  tint = Color(0xFF94A3B8),
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }
      }

      // Input Bar
      Surface(
        color = SurfaceDark,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Icebreaker / Random Prompt button
          IconButton(
            onClick = {
              inputText = viewModel.getRandomIcebreaker()
            },
            modifier = Modifier.size(44.dp)
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = "Insert conversation starter",
              tint = NeonCyan
            )
          }

          OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            placeholder = {
              Text(
                text = "Broadcast to everyone...",
                fontSize = 14.sp,
                color = Color(0xFF64748B)
              )
            },
            modifier = Modifier
              .weight(1f)
              .testTag("chat_input_field"),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = SurfaceElevated,
              unfocusedContainerColor = SurfaceElevated,
              focusedBorderColor = NeonCyan,
              unfocusedBorderColor = Color.Transparent,
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            ),
            maxLines = 4
          )

          Spacer(modifier = Modifier.width(8.dp))

          // Send Button
          IconButton(
            onClick = {
              if (inputText.isNotBlank()) {
                val textToSend = inputText
                inputText = ""
                viewModel.sendMessage(
                  text = textToSend,
                  senderName = userDisplayName,
                  senderAvatar = userAvatar
                )
              }
            },
            enabled = inputText.isNotBlank(),
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(
                if (inputText.isNotBlank()) {
                  Brush.linearGradient(listOf(NeonCyan, ElectricViolet))
                } else {
                  Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF334155)))
                }
              )
              .testTag("send_message_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = "Send message",
              tint = if (inputText.isNotBlank()) Color.White else Color(0xFF94A3B8),
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier.align(Alignment.BottomCenter)
    )
  }
}

@Composable
fun ChatMessageItem(
  message: ChatMessage,
  isMe: Boolean,
  onReply: () -> Unit,
  onDelete: () -> Unit
) {
  val alignment = if (isMe) Alignment.End else Alignment.Start

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = alignment
  ) {
    Row(
      horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
      verticalAlignment = Alignment.Bottom,
      modifier = Modifier.widthIn(max = 320.dp)
    ) {
      if (!isMe) {
        AvatarBadge(avatar = message.senderAvatar, name = message.senderName)
        Spacer(modifier = Modifier.width(8.dp))
      }

      Column(horizontalAlignment = alignment) {
        // Sender name and timestamp
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
        ) {
          Text(
            text = if (isMe) "You" else message.senderName,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isMe) NeonCyan else Color(0xFFCBD5E1)
          )
          Spacer(modifier = Modifier.width(6.dp))
          val formattedTime = remember(message.createdAt) {
            val date = message.createdAt?.toDate() ?: Date()
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
          }
          Text(
            text = formattedTime,
            fontSize = 10.sp,
            color = Color(0xFF64748B)
          )
        }

        // Message Bubble
        Card(
          shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isMe) 16.dp else 4.dp,
            bottomEnd = if (isMe) 4.dp else 16.dp
          ),
          colors = CardDefaults.cardColors(
            containerColor = if (isMe) UserBubbleGradientStart else StrangerBubbleBackground
          ),
          border = if (isMe) null else androidx.compose.foundation.BorderStroke(1.dp, SurfaceElevated)
        ) {
          Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            // Quoted reply if present
            if (!message.replyToText.isNullOrBlank()) {
              Surface(
                color = Color.Black.copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 6.dp)
              ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                  Text(
                    text = message.replyToSender ?: "Stranger",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                  )
                  Text(
                    text = message.replyToText ?: "",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }

            Text(
              text = message.text,
              fontSize = 14.sp,
              color = Color.White,
              lineHeight = 20.sp
            )
          }
        }

        // Quick action row
        Row(
          modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "Reply",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier
              .clickable { onReply() }
              .padding(2.dp)
          )
          if (isMe) {
            Text(
              text = "Delete",
              fontSize = 11.sp,
              color = Color(0xFFEF4444).copy(alpha = 0.8f),
              modifier = Modifier
                .clickable { onDelete() }
                .padding(2.dp)
            )
          }
        }
      }

      if (isMe) {
        Spacer(modifier = Modifier.width(8.dp))
        AvatarBadge(avatar = message.senderAvatar, name = "You")
      }
    }
  }
}

@Composable
fun AvatarBadge(avatar: String, name: String) {
  val initial = name.firstOrNull()?.uppercase() ?: "S"
  val bgBrush = remember(avatar) {
    when (avatar) {
      "avatar_1" -> Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF0284C7)))
      "avatar_2" -> Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)))
      "avatar_3" -> Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0xFFBE123C)))
      "avatar_4" -> Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF047857)))
      else -> Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
    }
  }

  Box(
    modifier = Modifier
      .size(34.dp)
      .clip(CircleShape)
      .background(bgBrush),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = initial,
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = Color.White
    )
  }
}
