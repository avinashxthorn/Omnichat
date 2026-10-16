package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StrangerBubbleBackground
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.UserBubbleGradientStart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrangerChatScreen(
  viewModel: StrangerChatViewModel,
  userDisplayName: String
) {
  val matchStatus by viewModel.matchStatus.collectAsStateWithLifecycle()
  val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
  val messages by viewModel.strangerMessages.collectAsStateWithLifecycle()
  val selectedTopic by viewModel.selectedTopic.collectAsStateWithLifecycle()

  var inputText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  val topics = listOf(
    "All Interests",
    "Gaming",
    "Music & Arts",
    "Tech & AI",
    "Deep Night Talks",
    "Anime & Movies",
    "Random Banter"
  )

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BackgroundDark)
  ) {
    // Top Bar
    TopAppBar(
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(Brush.linearGradient(listOf(ElectricViolet, Color(0xFFF43F5E)))),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Shuffle,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Stranger Match",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            val subtitleText = when (matchStatus) {
              StrangerMatchStatus.IDLE -> "Omegle-style pairing"
              StrangerMatchStatus.SEARCHING -> "Finding a stranger..."
              StrangerMatchStatus.CONNECTED -> "Connected to Stranger"
              StrangerMatchStatus.ENDED -> "Stranger disconnected"
            }
            Text(
              text = subtitleText,
              fontSize = 12.sp,
              color = when (matchStatus) {
                StrangerMatchStatus.CONNECTED -> Color(0xFF10B981)
                StrangerMatchStatus.SEARCHING -> NeonCyan
                StrangerMatchStatus.ENDED -> Color(0xFFEF4444)
                else -> Color(0xFF94A3B8)
              }
            )
          }
        }
      },
      actions = {
        if (matchStatus == StrangerMatchStatus.CONNECTED) {
          // Next Stranger button
          IconButton(
            onClick = { viewModel.nextStranger(userDisplayName) },
            modifier = Modifier.testTag("next_stranger_button")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Next stranger",
              tint = NeonCyan
            )
          }
          // Disconnect button
          IconButton(
            onClick = { viewModel.disconnect() },
            modifier = Modifier.testTag("disconnect_stranger_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Disconnect",
              tint = Color(0xFFEF4444)
            )
          }
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
    )

    // Main Content by Status
    when (matchStatus) {
      StrangerMatchStatus.IDLE -> {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(110.dp)
              .clip(CircleShape)
              .background(SurfaceElevated)
              .border(2.dp, Brush.linearGradient(listOf(ElectricViolet, NeonCyan)), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.PersonSearch,
              contentDescription = null,
              tint = NeonCyan,
              modifier = Modifier.size(56.dp)
            )
          }

          Spacer(modifier = Modifier.height(24.dp))

          Text(
            text = "Connect with a Stranger",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )

          Text(
            text = "Pair 1-on-1 with a random stranger worldwide. Skip anytime to meet someone new.",
            fontSize = 14.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
          )

          // Interest Topic Selector
          Text(
            text = "Choose an Interest (Optional):",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = NeonCyan,
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 8.dp)
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            topics.forEach { topic ->
              val isSelected = selectedTopic == topic
              FilterChip(
                selected = isSelected,
                onClick = { viewModel.setTopic(topic) },
                label = { Text(topic, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = ElectricViolet,
                  selectedLabelColor = Color.White,
                  containerColor = SurfaceDark,
                  labelColor = Color(0xFFCBD5E1)
                ),
                shape = RoundedCornerShape(16.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(32.dp))

          Button(
            onClick = { viewModel.startSearching(userDisplayName) },
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("start_stranger_chat_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
          ) {
            Icon(
              imageVector = Icons.Default.ElectricBolt,
              contentDescription = null,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Find a Stranger Now",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      StrangerMatchStatus.SEARCHING -> {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          val infiniteTransition = rememberInfiniteTransition(label = "pulse")
          val scale by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
              animation = tween(1000, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
          )

          Box(
            modifier = Modifier
              .size(130.dp)
              .scale(scale)
              .clip(CircleShape)
              .background(NeonCyan.copy(alpha = 0.15f))
              .border(2.dp, NeonCyan.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(64.dp),
              color = NeonCyan,
              strokeWidth = 3.dp
            )
          }

          Spacer(modifier = Modifier.height(32.dp))

          Text(
            text = "Searching for someone...",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )

          Text(
            text = "Topic: $selectedTopic",
            fontSize = 14.sp,
            color = NeonCyan,
            modifier = Modifier.padding(top = 6.dp, bottom = 28.dp)
          )

          OutlinedButton(
            onClick = { viewModel.disconnect() },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.testTag("cancel_search_button")
          ) {
            Text("Cancel Search", color = Color(0xFFEF4444))
          }
        }
      }

      StrangerMatchStatus.CONNECTED -> {
        Column(modifier = Modifier.fillMaxSize()) {
          // Omegle Banner
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF10B981))
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "You're chatting with a random stranger. Say hi!",
                fontSize = 13.sp,
                color = Color(0xFFCBD5E1),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
              )
            }
          }

          // Messages List
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
          ) {
            if (messages.isEmpty()) {
              Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "Break the ice! Send the first message.",
                  color = Color(0xFF64748B),
                  fontSize = 14.sp
                )
              }
            } else {
              LazyColumn(
                state = listState,
                modifier = Modifier
                  .fillMaxSize()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                items(messages, key = { it.id }) { msg ->
                  val isMe = msg.senderId == viewModel.currentUserId
                  StrangerMessageBubble(text = msg.text, isMe = isMe)
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
              // Next Stranger quick icon
              IconButton(
                onClick = { viewModel.nextStranger(userDisplayName) },
                modifier = Modifier.size(44.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Shuffle,
                  contentDescription = "Next stranger",
                  tint = ElectricViolet
                )
              }

              OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                  Text(
                    text = "Message stranger...",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                  )
                },
                modifier = Modifier
                  .weight(1f)
                  .testTag("stranger_input_field"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = SurfaceElevated,
                  unfocusedContainerColor = SurfaceElevated,
                  focusedBorderColor = NeonCyan,
                  unfocusedBorderColor = Color.Transparent,
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White
                ),
                maxLines = 3
              )

              Spacer(modifier = Modifier.width(8.dp))

              IconButton(
                onClick = {
                  if (inputText.isNotBlank()) {
                    val textToSend = inputText
                    inputText = ""
                    viewModel.sendMessage(textToSend)
                  }
                },
                enabled = inputText.isNotBlank(),
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(
                    if (inputText.isNotBlank()) ElectricViolet else Color(0xFF334155)
                  )
                  .testTag("stranger_send_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Send,
                  contentDescription = "Send",
                  tint = if (inputText.isNotBlank()) Color.White else Color(0xFF94A3B8),
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }
      }

      StrangerMatchStatus.ENDED -> {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Default.MeetingRoom,
            contentDescription = null,
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(64.dp)
          )

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "Stranger has disconnected.",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )

          Text(
            text = "Your conversation has ended. Start a new chat to meet someone else!",
            fontSize = 14.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
          )

          Button(
            onClick = { viewModel.startSearching(userDisplayName) },
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("find_new_stranger_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
          ) {
            Text(
              text = "Find New Stranger",
              color = Color(0xFF0F172A),
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            )
          }
        }
      }
    }
  }
}

@Composable
fun StrangerMessageBubble(text: String, isMe: Boolean) {
  val alignment = if (isMe) Alignment.End else Alignment.Start

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = alignment
  ) {
    Text(
      text = if (isMe) "You" else "Stranger",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = if (isMe) NeonCyan else Color(0xFFF43F5E),
      modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
    )

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
      border = if (isMe) null else androidx.compose.foundation.BorderStroke(1.dp, SurfaceElevated),
      modifier = Modifier.widthIn(max = 290.dp)
    ) {
      Text(
        text = text,
        fontSize = 14.sp,
        color = Color.White,
        lineHeight = 20.sp,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
      )
    }
  }
}
