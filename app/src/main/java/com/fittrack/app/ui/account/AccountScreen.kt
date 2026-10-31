package com.fittrack.app.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.fittrack.app.di.AppModule
import com.fittrack.app.sync.SupabaseProvider
import com.fittrack.app.sync.SyncScheduler
import com.fittrack.app.sync.SyncStatus
import com.fittrack.app.ui.components.DetailHeader
import com.fittrack.app.ui.components.IconBadge
import com.fittrack.app.ui.components.PrimaryButton
import com.fittrack.app.ui.components.ScreenGutter
import com.fittrack.app.ui.components.Tile
import com.fittrack.app.ui.components.TonalButton
import com.fittrack.app.ui.components.fitTextFieldColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cloud sync account: sign in / create account (email + password), and when
 * signed in, sync status, manual sync, and sign out.
 */
@Composable
fun AccountScreen(appModule: AppModule, onBack: () -> Unit) {
    val account = appModule.accountManager
    val email by account.email.collectAsState()
    val status by appModule.syncPrefs.status.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { DetailHeader(title = "Cloud sync", onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenGutter, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                !SupabaseProvider.isConfigured -> NotConfigured()
                email == null -> SignInForm(
                    onSignIn = { e, p -> account.signIn(e, p) },
                    onSignUp = { e, p -> account.signUp(e, p) },
                    onSignedIn = { SyncScheduler.requestSync(context) },
                )
                else -> SignedIn(
                    email = email!!,
                    status = status,
                    lastSyncedAt = appModule.syncPrefs.lastSyncedAt,
                    onSyncNow = { scope.launch { appModule.syncEngine.syncNow() } },
                    onSignOut = { scope.launch { account.signOut(); appModule.syncPrefs.reset() } },
                )
            }
        }
    }
}

@Composable
private fun NotConfigured() {
    Tile {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Default.CloudOff, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text("Sync isn't set up in this build", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Add the Supabase URL and key to local.properties and rebuild.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SignInForm(
    onSignIn: suspend (String, String) -> String?,
    onSignUp: suspend (String, String) -> String?,
    onSignedIn: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val valid = email.contains("@") && password.length >= 6

    Tile {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Default.Sync)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text("Keep your workouts on every device", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Sign in once per phone. Your history syncs in the background and still works offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    Tile(contentPadding = PaddingValues(16.dp)) {
        Text(
            if (creating) "Create account" else "Sign in",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextField(
            value = email, onValueChange = { email = it; error = null },
            placeholder = { Text("Email") }, singleLine = true,
            leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = MaterialTheme.shapes.small, colors = fitTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password, onValueChange = { password = it; error = null },
            placeholder = { Text("Password (6+ characters)") }, singleLine = true,
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = MaterialTheme.shapes.small, colors = fitTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        notice?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            PrimaryButton(
                text = if (creating) "Create account" else "Sign in",
                enabled = valid,
                onClick = {
                    busy = true; error = null; notice = null
                    scope.launch {
                        val err = if (creating) onSignUp(email, password) else onSignIn(email, password)
                        busy = false
                        if (err != null) error = err
                        else if (creating) notice = "Account created. If your project requires email confirmation, check your inbox, then sign in."
                        else onSignedIn()
                    }
                },
            )
        }
        TextButton(
            onClick = { creating = !creating; error = null; notice = null },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(if (creating) "Have an account? Sign in" else "New here? Create an account")
        }
    }
}

@Composable
private fun SignedIn(
    email: String,
    status: SyncStatus,
    lastSyncedAt: Long,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
) {
    Tile {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Default.CloudDone)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(email, style = MaterialTheme.typography.titleSmall)
                Text(
                    when (status) {
                        is SyncStatus.Syncing -> "Syncing…"
                        is SyncStatus.Error -> "Last attempt failed: ${status.message}"
                        else -> if (lastSyncedAt > 0)
                            "Last synced " + SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(lastSyncedAt))
                        else "Not synced yet"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status is SyncStatus.Error) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    PrimaryButton(text = "Sync now", icon = Icons.Default.Sync, onClick = onSyncNow, enabled = status !is SyncStatus.Syncing)
    TonalButton(text = "Sign out", onClick = onSignOut)
    Text(
        "Signing out keeps everything on this phone. Workouts you log while signed out sync the next time you sign in.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
