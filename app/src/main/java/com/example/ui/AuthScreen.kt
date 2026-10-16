package com.example.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  // Attempt silent auto-sign in on launch if account already authorized
  androidx.compose.runtime.LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      scope = coroutineScope,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {}
    )
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = BackgroundDark
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        // App Hero Badge
        Box(
          modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(
              Brush.linearGradient(
                colors = listOf(NeonCyan, ElectricViolet)
              )
            )
            .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Public,
            contentDescription = "Universal Chat Icon",
            tint = Color.White,
            modifier = Modifier.size(52.dp)
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
          text = "OmniChat",
          fontSize = 34.sp,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
          letterSpacing = 1.sp
        )

        Text(
          text = "Universal Live Chat & Omegle Stranger Mode",
          fontSize = 15.sp,
          color = NeonCyan,
          fontWeight = FontWeight.Medium,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
        )

        // Features Highlights Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = SurfaceDark),
          shape = RoundedCornerShape(16.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceElevated)
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            FeatureRow(
              icon = Icons.Default.Chat,
              iconColor = NeonCyan,
              title = "Universal Public Channels",
              subtitle = "Messages sent are visible live to everyone across the world"
            )
            FeatureRow(
              icon = Icons.Default.People,
              iconColor = ElectricViolet,
              title = "1-on-1 Stranger Chat",
              subtitle = "Randomly match with strangers Omegle-style by interest"
            )
            FeatureRow(
              icon = Icons.Default.Security,
              iconColor = Color(0xFF10B981),
              title = "Zero-Trust Safe Cloud",
              subtitle = "Cloud-synced in real-time with Google Security"
            )
          }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (errorMessage != null) {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D).copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text(
              text = errorMessage ?: "",
              color = Color(0xFFFECACA),
              fontSize = 13.sp,
              modifier = Modifier.padding(12.dp),
              textAlign = TextAlign.Center
            )
          }
        }

        // Primary Google Sign In Button
        Button(
          onClick = {
            isLoading = true
            errorMessage = null
            signInWithGoogle(
              context = context,
              credentialManager = credentialManager,
              scope = coroutineScope,
              onSuccess = {
                isLoading = false
                onAuthSuccess()
              },
              onError = { err ->
                isLoading = false
                errorMessage = err
              },
              onCancelled = {
                isLoading = false
              }
            )
          },
          enabled = !isLoading,
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("google_sign_in_button"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF0F172A)
          )
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              color = Color(0xFF0F172A),
              strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Connecting to OmniChat...",
              fontWeight = FontWeight.SemiBold,
              fontSize = 16.sp
            )
          } else {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF2563EB)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Sign in with Google",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "By signing in, you agree to respectful community conduct.",
          fontSize = 12.sp,
          color = Color(0xFF94A3B8),
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

@Composable
private fun FeatureRow(
  icon: ImageVector,
  iconColor: Color,
  title: String,
  subtitle: String
) {
  Row(
    verticalAlignment = Alignment.Top,
    modifier = Modifier.fillMaxWidth()
  ) {
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(iconColor.copy(alpha = 0.15f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconColor,
        modifier = Modifier.size(20.dp)
      )
    }
    Spacer(modifier = Modifier.width(14.dp))
    Column {
      Text(
        text = title,
        color = Color.White,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
      )
      Text(
        text = subtitle,
        color = Color(0xFF94A3B8),
        fontSize = 12.sp,
        lineHeight = 16.sp
      )
    }
  }
}

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onUnauthenticated()
    return
  }

  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(true)
    .setServerClientId(clientId)
    .setAutoSelectEnabled(true)
    .build()

  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (e: Exception) {
      onUnauthenticated()
    }
  }
}

fun signInWithGoogle(
  context: Context,
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSuccess: () -> Unit,
  onError: (String) -> Unit,
  onCancelled: () -> Unit
) {
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onError("Google configuration missing default_web_client_id")
    return
  }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context as Activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onSuccess()
      } else {
        onError("Unexpected credential format received")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
      onCancelled()
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onError(e.localizedMessage ?: "Sign-in failed")
    }
  }
}

fun appSignOut(
  context: Context,
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onComplete: () -> Unit
) {
  Firebase.auth.signOut()
  scope.launch {
    try {
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    } catch (e: Exception) {
      Log.e("Auth", "Failed to clear credential state", e)
    } finally {
      onComplete()
    }
  }
}
