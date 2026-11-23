package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.VerifiedUser
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.components.AxiomBrandLogo
import com.example.ui.components.AxiomPillBadge
import com.example.ui.theme.AxiomCharcoal
import com.example.ui.theme.AxiomCrimson
import com.example.ui.theme.AxiomCrimsonBg
import com.example.ui.theme.AxiomGold
import com.example.ui.theme.AxiomGoldChampagne
import com.example.ui.theme.AxiomGoldSubtleContainer
import com.example.ui.theme.AxiomHairline
import com.example.ui.theme.AxiomIvory
import com.example.ui.theme.AxiomObsidian
import com.example.ui.theme.AxiomSilver
import com.example.ui.theme.AxiomSlate
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope,
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {},
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
    scope.launch {
        try {
            val activity = context as? Activity
                ?: throw IllegalStateException("Activity context required for Credential Manager.")
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type received.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOutAxiom(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {},
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var isCheckingAuto by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingAuto = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingAuto = false
            },
            scope = scope,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AxiomObsidian)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_command_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .alpha(0.35f),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, AxiomObsidian)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(modifier = Modifier.widthIn(max = 520.dp)) {
                AxiomBrandLogo(
                    sizeDp = 54,
                    showWordmark = true,
                    subtitle = "Intelligent Fleet & Logistics Intelligence",
                )

                Spacer(Modifier.height(24.dp))

                AxiomPillBadge(
                    text = "DATA → ANALYSIS → RISK → INSIGHT → ACTION",
                    fgColor = AxiomGoldChampagne,
                    bgColor = AxiomGoldSubtleContainer,
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Enterprise Command Access",
                    style = MaterialTheme.typography.displaySmall,
                    color = AxiomIvory,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Sign in with your verified Google Workspace or operator account to unlock multi-company fleet telemetry, Z-score & CUSUM fuel forensics, and Ottilia AI decision support.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AxiomSilver,
                )

                Spacer(Modifier.height(24.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = AxiomCharcoal),
                    border = BorderStroke(1.dp, AxiomHairline),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        FeatureRow(
                            icon = Icons.Outlined.Analytics,
                            title = "Fuel & Statistical Forensics",
                            desc = "Z-score anomaly signals, CUSUM drift tracking & tank validation"
                        )
                        Spacer(Modifier.height(14.dp))
                        FeatureRow(
                            icon = Icons.Outlined.Security,
                            title = "7-Role RBAC & Multi-Company",
                            desc = "Zero-trust Firestore tenant isolation & immutable audit trail"
                        )
                        Spacer(Modifier.height(14.dp))
                        FeatureRow(
                            icon = Icons.Outlined.VerifiedUser,
                            title = "Ottilia AI Decision Engine",
                            desc = "Grounded operational briefings backed by real fleet calculations"
                        )

                        Spacer(Modifier.height(22.dp))

                        if (errorMessage != null) {
                            Surface(
                                color = AxiomCrimsonBg,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, AxiomCrimson.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp),
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AxiomCrimson,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        }

                        Button(
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isLoading = false
                                    }
                                )
                            },
                            enabled = !isLoading && !isCheckingAuto,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AxiomGold,
                                contentColor = AxiomObsidian,
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_sign_in_button"),
                        ) {
                            if (isLoading || isCheckingAuto) {
                                CircularProgressIndicator(
                                    color = AxiomObsidian,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = if (isCheckingAuto) "Verifying session..." else "Authenticating...",
                                    fontWeight = FontWeight.SemiBold,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Sign in with Google",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Surface(
                    color = AxiomSlate,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AxiomHairline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Authenticated via Google Identity Services & Jetpack Credential Manager. All telemetry and operational data are strictly scoped per tenant.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AxiomSilver,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = AxiomGold,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = AxiomIvory)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
        }
    }
}
