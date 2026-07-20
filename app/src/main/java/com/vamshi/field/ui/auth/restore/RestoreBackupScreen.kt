package com.vamshi.field.ui.auth.restore

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.vamshi.field.ui.auth.components.AuthErrorBanner
import com.vamshi.field.ui.components.AppTopBar

/**
 * Pre-auth "restore from Google Drive" screen, reachable from both Onboarding and
 * Unlock for a coach reinstalling the app who already has a backup.
 *
 * Uses the same [GoogleSignInClient] construction / [rememberLauncherForActivityResult]
 * pattern as [com.vamshi.field.ui.settings.SettingsScreen] — see that file for the
 * original. No password field, no security question: restoring the backup re-establishes
 * the whole account and session by itself.
 */
@Composable
fun RestoreBackupScreen(
    onNavigateBack: () -> Unit,
    onRestoreSuccess: () -> Unit,
    viewModel: RestoreBackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.restoreSuccess) {
        if (uiState.restoreSuccess) {
            viewModel.onAction(RestoreBackupAction.NavigationConsumed)
            onRestoreSuccess()
        }
    }

    RestoreBackupContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreBackupContent(
    uiState: RestoreBackupUiState,
    onAction: (RestoreBackupAction) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val googleSignInClient: GoogleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != android.app.Activity.RESULT_OK) {
            onAction(RestoreBackupAction.GoogleSignInFailed("Sign-in cancelled or failed."))
            return@rememberLauncherForActivityResult
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            task.getResult(ApiException::class.java)
            onAction(RestoreBackupAction.GoogleSignInSucceeded)
        } catch (e: ApiException) {
            onAction(RestoreBackupAction.GoogleSignInFailed("Sign-in failed (Code: ${e.statusCode}): ${e.message}"))
        } catch (e: Exception) {
            onAction(RestoreBackupAction.GoogleSignInFailed("Sign-in failed: ${e.message}"))
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Restore data",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (uiState.errorMessage != null) {
                AuthErrorBanner(
                    message = uiState.errorMessage,
                    onDismiss = { onAction(RestoreBackupAction.DismissError) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "Already coaching with Field?",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "If you've backed up your data to Google Drive before, sign in " +
                    "with the same Google account to restore your athletes, groups, and results.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Downloading and restoring your data...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Button(
                    onClick = { launcher.launch(googleSignInClient.signInIntent) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Sign in with Google")
                }
            }
        }
    }
}
