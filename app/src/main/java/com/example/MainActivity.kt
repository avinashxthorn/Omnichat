package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.ChatRepository
import com.example.ui.AuthScreen
import com.example.ui.ChatViewModel
import com.example.ui.ProfileScreen
import com.example.ui.StrangerChatScreen
import com.example.ui.StrangerChatViewModel
import com.example.ui.UniversalChatScreen
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OmniChatTheme
import com.example.ui.theme.SurfaceDark
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      OmniChatTheme {
        AppContent()
      }
    }
  }
}

@Composable
fun AppContent() {
  var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth ->
      currentUser = auth.currentUser
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose {
      Firebase.auth.removeAuthStateListener(listener)
    }
  }

  val user = currentUser
  if (user == null) {
    AuthScreen(
      onAuthSuccess = {
        currentUser = Firebase.auth.currentUser
      }
    )
  } else {
    OmniChatMainApp(
      currentUserId = user.uid,
      initialName = user.displayName?.ifBlank { null } ?: "Stranger #${user.uid.takeLast(4)}"
    )
  }
}

@Composable
fun OmniChatMainApp(
  currentUserId: String,
  initialName: String
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  // Initialize Repository using custom named database ID from string resource
  val databaseId = context.getString(R.string.firestore_database_id)
  val db = remember(databaseId) { FirebaseFirestore.getInstance(databaseId) }
  val repository = remember(db) { ChatRepository(db) }

  var userDisplayName by remember { mutableStateOf(initialName) }
  var userAvatar by remember { mutableStateOf("avatar_1") }
  var userBio by remember { mutableStateOf("Live on OmniChat") }

  // Load saved profile if available
  LaunchedEffect(currentUserId) {
    repository.observeUserProfile(currentUserId).collect { profile ->
      if (profile != null) {
        if (profile.displayName.isNotBlank()) userDisplayName = profile.displayName
        if (profile.avatar.isNotBlank()) userAvatar = profile.avatar
        if (profile.customBio.isNotBlank()) userBio = profile.customBio
      }
    }
  }

  // Keyed ViewModels by user ID
  val chatViewModel: ChatViewModel = viewModel(
    key = "chat_$currentUserId",
    factory = viewModelFactory {
      addInitializer(ChatViewModel::class) {
        ChatViewModel(repository, currentUserId)
      }
    }
  )

  val strangerViewModel: StrangerChatViewModel = viewModel(
    key = "stranger_$currentUserId",
    factory = viewModelFactory {
      addInitializer(StrangerChatViewModel::class) {
        StrangerChatViewModel(repository, currentUserId)
      }
    }
  )

  var selectedTab by remember { mutableIntStateOf(0) }

  // Handle back button on sub-tabs
  BackHandler(enabled = selectedTab != 0) {
    selectedTab = 0
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    bottomBar = {
      NavigationBar(
        containerColor = SurfaceDark,
        contentColor = Color.White
      ) {
        NavigationBarItem(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          icon = { Icon(Icons.Default.Forum, contentDescription = "Universal Chat") },
          label = { Text("Universal") },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = NeonCyan,
            selectedTextColor = NeonCyan,
            unselectedIconColor = Color(0xFF94A3B8),
            unselectedTextColor = Color(0xFF94A3B8),
            indicatorColor = ElectricViolet.copy(alpha = 0.25f)
          ),
          modifier = Modifier.testTag("tab_universal")
        )
        NavigationBarItem(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          icon = { Icon(Icons.Default.Shuffle, contentDescription = "Omegle Match") },
          label = { Text("Stranger") },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = ElectricViolet,
            selectedTextColor = ElectricViolet,
            unselectedIconColor = Color(0xFF94A3B8),
            unselectedTextColor = Color(0xFF94A3B8),
            indicatorColor = ElectricViolet.copy(alpha = 0.25f)
          ),
          modifier = Modifier.testTag("tab_stranger")
        )
        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = { Icon(Icons.Default.Person, contentDescription = "Profile & Settings") },
          label = { Text("Profile") },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = NeonCyan,
            selectedTextColor = NeonCyan,
            unselectedIconColor = Color(0xFF94A3B8),
            unselectedTextColor = Color(0xFF94A3B8),
            indicatorColor = NeonCyan.copy(alpha = 0.25f)
          ),
          modifier = Modifier.testTag("tab_profile")
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (selectedTab) {
        0 -> UniversalChatScreen(
          viewModel = chatViewModel,
          userDisplayName = userDisplayName,
          userAvatar = userAvatar
        )
        1 -> StrangerChatScreen(
          viewModel = strangerViewModel,
          userDisplayName = userDisplayName
        )
        2 -> ProfileScreen(
          currentUserId = currentUserId,
          initialDisplayName = userDisplayName,
          initialAvatar = userAvatar,
          initialBio = userBio,
          onSaveProfile = { name, avatar, bio ->
            userDisplayName = name
            userAvatar = avatar
            userBio = bio
            coroutineScope.launch {
              repository.saveUserProfile(name, avatar, bio)
            }
          },
          onSignOut = {
            Firebase.auth.signOut()
          }
        )
      }
    }
  }
}
