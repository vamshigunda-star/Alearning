package com.example.alearning.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.repository.AuthRepository
import com.example.alearning.domain.repository.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Determines the app's initial destination on launch.
 *
 * Decision logic:
 *  - [AuthGateState.Loading]              — async check in progress; show a spinner.
 *  - [AuthGateState.Authenticated]        — session exists → navigate to Dashboard.
 *  - [AuthGateState.UnauthenticatedHasUsers] — accounts exist, no session → go to SignIn.
 *  - [AuthGateState.UnauthenticatedNoUsers]  — no accounts at all → go to SignUp.
 *
 * This ViewModel is consumed by [com.example.alearning.ui.navigation.ALearningNavGraph]
 * to determine the [NavHost] start destination. Once the destination is resolved the
 * [NavHost] takes over and this ViewModel is not observed further.
 */
sealed interface AuthGateState {
    data object Loading : AuthGateState
    data object Authenticated : AuthGateState
    data object UnauthenticatedHasUsers : AuthGateState
    data object UnauthenticatedNoUsers : AuthGateState
}

@HiltViewModel
class AuthGateViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthGateState>(AuthGateState.Loading)
    val state: StateFlow<AuthGateState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val currentId = sessionManager.currentUserIdOnce()
            val state = if (currentId != null) {
                AuthGateState.Authenticated
            } else {
                val count = authRepository.userCount()
                if (count == 0) AuthGateState.UnauthenticatedNoUsers
                else AuthGateState.UnauthenticatedHasUsers
            }
            _state.value = state
        }
    }
}
