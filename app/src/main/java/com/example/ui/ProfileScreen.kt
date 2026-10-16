package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
  currentUserId: String,
  initialDisplayName: String,
  initialAvatar: String,
  initialBio: String,
  onSaveProfile: (name: String, avatar: String, bio: String) -> Unit,
  onSignOut: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  val snackbarHostState = remember { SnackbarHostState() }

  var displayName by remember { mutableStateOf(initialDisplayName) }
  var selectedAvatar by remember { mutableStateOf(initialAvatar) }
  var bioText by remember { mutableStateOf(initialBio) }
  var isOnlinePresence by remember { mutableStateOf(true) }

  val avatarOptions = listOf("avatar_1", "avatar_2", "avatar_3", "avatar_4", "avatar_5")

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(BackgroundDark)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
    ) {
      TopAppBar(
        title = {
          Text(
            text = "Profile & Identity",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
      )

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Main Avatar Preview
        AvatarBadgeBig(avatar = selectedAvatar, name = displayName)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = displayName.ifBlank { "Stranger" },
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )

        Text(
          text = "UID: ${currentUserId.take(12)}...",
          fontSize = 12.sp,
          color = Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Avatar Selection
        Text(
          text = "Choose Avatar Accent",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = NeonCyan,
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          avatarOptions.forEach { av ->
            val isSelected = selectedAvatar == av
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(getAvatarBrush(av))
                .border(
                  width = if (isSelected) 3.dp else 1.dp,
                  color = if (isSelected) Color.White else Color.Transparent,
                  shape = CircleShape
                )
                .clickable { selectedAvatar = av },
              contentAlignment = Alignment.Center
            ) {
              if (isSelected) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Nickname Field
        Text(
          text = "Display Name / Stranger Persona",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = NeonCyan,
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
        )

        OutlinedTextField(
          value = displayName,
          onValueChange = { if (it.length <= 30) displayName = it },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("display_name_field"),
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceElevated,
            unfocusedContainerColor = SurfaceElevated,
            focusedBorderColor = NeonCyan,
            unfocusedBorderColor = SurfaceDark,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Bio Field
        Text(
          text = "Short Bio or Icebreaker Note",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = NeonCyan,
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
        )

        OutlinedTextField(
          value = bioText,
          onValueChange = { if (it.length <= 150) bioText = it },
          placeholder = { Text("e.g. Night owl gamer and music enthusiast", color = Color(0xFF64748B)) },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("bio_field"),
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceElevated,
            unfocusedContainerColor = SurfaceElevated,
            focusedBorderColor = NeonCyan,
            unfocusedBorderColor = SurfaceDark,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          maxLines = 3
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Save Profile Button
        Button(
          onClick = {
            onSaveProfile(displayName, selectedAvatar, bioText)
            coroutineScope.launch {
              snackbarHostState.showSnackbar("Profile saved successfully")
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("save_profile_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
        ) {
          Icon(
            imageVector = Icons.Default.Save,
            contentDescription = null,
            tint = Color(0xFF0F172A),
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Save Profile",
            color = Color(0xFF0F172A),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Safety Guidelines Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = SurfaceDark),
          shape = RoundedCornerShape(16.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceElevated)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Community Safety Guidelines",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "• Universal chats are visible to all users worldwide.\n" +
                  "• Do not share private personal information (phone numbers, addresses, passwords).\n" +
                  "• Be courteous and respectful. Harassment or hate speech results in account suspension.",
              fontSize = 12.sp,
              color = Color(0xFF94A3B8),
              lineHeight = 18.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Sign Out Button
        OutlinedButton(
          onClick = {
            appSignOut(
              context = context,
              credentialManager = credentialManager,
              scope = coroutineScope,
              onComplete = onSignOut
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("sign_out_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Sign Out",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
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
fun AvatarBadgeBig(avatar: String, name: String) {
  val initial = name.firstOrNull()?.uppercase() ?: "S"
  Box(
    modifier = Modifier
      .size(80.dp)
      .clip(CircleShape)
      .background(getAvatarBrush(avatar))
      .border(3.dp, Color.White.copy(alpha = 0.4f), CircleShape),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = initial,
      fontSize = 32.sp,
      fontWeight = FontWeight.ExtraBold,
      color = Color.White
    )
  }
}

fun getAvatarBrush(avatar: String): Brush {
  return when (avatar) {
    "avatar_1" -> Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF0284C7)))
    "avatar_2" -> Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)))
    "avatar_3" -> Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0xFFBE123C)))
    "avatar_4" -> Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF047857)))
    else -> Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
  }
}
