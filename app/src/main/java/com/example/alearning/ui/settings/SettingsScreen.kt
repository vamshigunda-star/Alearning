package com.example.alearning.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.ui.components.AppTopBar
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsContent(
        uiState = uiState,
        onAction = { action ->
            if (action == SettingsAction.NavigateBack) {
                onNavigateBack()
            } else {
                viewModel.onAction(action)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
) {
    val context = LocalContext.current

    // Move launcher and client to top level to avoid recreation/loss during conditional renders
    val googleSignInClient: GoogleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
            onAction(SettingsAction.ConnectDriveSuccess(account?.email ?: "Connected"))
        } catch (e: Exception) {
            onAction(SettingsAction.ConnectDriveError("Sign-in failed: ${e.message}"))
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Settings",
                navigationIcon = {
                    IconButton(onClick = { onAction(SettingsAction.NavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Data Backup & Restore",
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    color = if (uiState.errorMessage.contains("successful", ignoreCase = true)) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }

            if (uiState.isSyncing) {
                CircularProgressIndicator()
                Text("Syncing in progress...")
            } else {
                Button(
                    onClick = { launcher.launch(googleSignInClient.signInIntent) },
                    enabled = !uiState.isDriveConnected,
                ) {
                    Text(if (uiState.isDriveConnected) "Connected to Google Drive" else "Connect Google Drive")
                }

                Button(
                    onClick = { onAction(SettingsAction.BackupNow) },
                    enabled = uiState.isDriveConnected,
                ) {
                    Text("Backup Now")
                }

                Button(
                    onClick = { onAction(SettingsAction.RestoreData) },
                    enabled = uiState.isDriveConnected,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Restore Data (Overwrites Local)")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.lastBackupTimestamp != null) {
                val dateString = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
                    .format(Date(uiState.lastBackupTimestamp))
                Text(
                    text = "Last Backup: $dateString",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = "No previous backup found locally.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
