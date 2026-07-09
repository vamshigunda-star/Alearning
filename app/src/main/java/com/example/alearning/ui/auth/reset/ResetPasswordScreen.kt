package com.example.alearning.ui.auth.reset

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.ui.auth.signup.AuthErrorBanner
import com.example.alearning.ui.components.AppTopBar
import kotlinx.coroutines.launch

@Composable
fun ResetPasswordScreen(
    onBack: () -> Unit,
    onResetSuccess: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.resetSuccess) {
        if (uiState.resetSuccess) {
            viewModel.onAction(ResetPasswordAction.NavigationConsumed)
            scope.launch {
                snackbarHostState.showSnackbar("Password updated. Please sign in.")
            }
            onResetSuccess()
        }
    }

    ResetPasswordContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordContent(
    uiState: ResetPasswordUiState,
    onAction: (ResetPasswordAction) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Reset Password",
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.step == 2) onAction(ResetPasswordAction.BackClicked)
                        else onBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (uiState.errorMessage != null) {
                AuthErrorBanner(
                    message = uiState.errorMessage,
                    onDismiss = { onAction(ResetPasswordAction.DismissError) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.step == 1) {
                ResetStep1(
                    username = uiState.username,
                    isLoading = uiState.isLoading,
                    onAction = onAction,
                    focusManager = focusManager
                )
            } else {
                ResetStep2(
                    uiState = uiState,
                    onAction = onAction,
                    focusManager = focusManager
                )
            }
        }
    }
}

@Composable
private fun ResetStep1(
    username: String,
    isLoading: Boolean,
    onAction: (ResetPasswordAction) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Text(
        text = "Enter your username",
        style = MaterialTheme.typography.headlineSmall
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "We'll show you your security question.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = username,
        onValueChange = { onAction(ResetPasswordAction.UsernameChanged(it)) },
        label = { Text("Username") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = {
            focusManager.clearFocus()
            onAction(ResetPasswordAction.ContinueClicked)
        }),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = { onAction(ResetPasswordAction.ContinueClicked) },
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = MaterialTheme.shapes.large
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text("Continue")
        }
    }
}

@Composable
private fun ResetStep2(
    uiState: ResetPasswordUiState,
    onAction: (ResetPasswordAction) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Text(
        text = "Answer your security question",
        style = MaterialTheme.typography.headlineSmall
    )
    Spacer(modifier = Modifier.height(8.dp))

    if (uiState.securityQuestion != null) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = uiState.securityQuestion,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    OutlinedTextField(
        value = uiState.securityAnswer,
        onValueChange = { onAction(ResetPasswordAction.AnswerChanged(it)) },
        label = { Text("Your answer") },
        isError = uiState.securityAnswerError != null,
        supportingText = uiState.securityAnswerError?.let { { Text(it) } }
            ?: { Text("Case-insensitive") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = uiState.newPassword,
        onValueChange = { onAction(ResetPasswordAction.NewPasswordChanged(it)) },
        label = { Text("New password") },
        isError = uiState.newPasswordError != null,
        supportingText = uiState.newPasswordError?.let { { Text(it) } }
            ?: { Text("At least 8 characters, one letter, one digit") },
        singleLine = true,
        visualTransformation = if (uiState.newPasswordVisible)
            VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { onAction(ResetPasswordAction.ToggleNewPasswordVisibility) }) {
                Icon(
                    imageVector = if (uiState.newPasswordVisible)
                        Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (uiState.newPasswordVisible)
                        "Hide password" else "Show password"
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = uiState.confirmPassword,
        onValueChange = { onAction(ResetPasswordAction.ConfirmPasswordChanged(it)) },
        label = { Text("Confirm new password") },
        isError = uiState.confirmPasswordError != null,
        supportingText = uiState.confirmPasswordError?.let { { Text(it) } },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = {
            focusManager.clearFocus()
            onAction(ResetPasswordAction.Submit)
        }),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = { onAction(ResetPasswordAction.Submit) },
        enabled = !uiState.isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = MaterialTheme.shapes.large
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text("Update Password")
        }
    }
}
