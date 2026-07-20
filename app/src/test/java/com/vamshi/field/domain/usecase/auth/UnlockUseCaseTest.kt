package com.vamshi.field.domain.usecase.auth

import com.vamshi.field.domain.model.auth.AuthError
import com.vamshi.field.domain.model.auth.AuthResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockUseCaseTest {

    private val repository = FakeAuthRepository()
    private val unlock = UnlockUseCase(repository)

    private suspend fun seedAccount(): String {
        val result = repository.signUp(
            firstName = "Jordan Reyes",
            lastName = "",
            username = "jordan.reyes",
            password = "goodpass1",
            email = null
        )
        return (result as AuthResult.Success).user.id
    }

    @Test
    fun `correct password unlocks and establishes a session`() = runTest {
        val userId = seedAccount()
        repository.signOut()

        val result = unlock(userId, "goodpass1")

        assertTrue(result is AuthResult.Success)
        assertEquals(userId, repository.currentSessionUserId())
    }

    @Test
    fun `wrong password is rejected with the generic InvalidCredentials error`() = runTest {
        val userId = seedAccount()
        repository.signOut()

        val result = unlock(userId, "wrongpass1")

        assertEquals(AuthResult.Failure(AuthError.InvalidCredentials), result)
        assertNull(repository.currentSessionUserId())
    }

    @Test
    fun `unknown account id is rejected the same way as a wrong password`() = runTest {
        val result = unlock("no-such-user", "goodpass1")

        assertEquals(AuthResult.Failure(AuthError.InvalidCredentials), result)
    }

    @Test
    fun `a failed unlock does not disturb an already-active session`() = runTest {
        val userId = seedAccount()
        // seedAccount() left this user signed in (signUp auto-signs-in) — a bad unlock
        // attempt for a *different* id should not sign the current user out.
        unlock("no-such-user", "whatever")

        assertEquals(userId, repository.currentSessionUserId())
    }
}
