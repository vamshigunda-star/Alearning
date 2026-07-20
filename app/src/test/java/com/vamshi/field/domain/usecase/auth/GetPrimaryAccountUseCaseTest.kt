package com.vamshi.field.domain.usecase.auth

import com.vamshi.field.domain.model.auth.AuthResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetPrimaryAccountUseCaseTest {

    private val repository = FakeAuthRepository()
    private val getPrimaryAccount = GetPrimaryAccountUseCase(repository)

    @Test
    fun `zero accounts resolves to null`() = runTest {
        assertNull(getPrimaryAccount())
    }

    @Test
    fun `single account is always the primary account`() = runTest {
        val result = repository.signUp(
            firstName = "Jordan Reyes", lastName = "", username = "jordan.reyes",
            password = "goodpass1", email = null
        )
        val user = (result as AuthResult.Success).user

        assertEquals(user, getPrimaryAccount())
    }

    @Test
    fun `multiple accounts resolve to the most recently created one`() = runTest {
        val first = (repository.signUp(
            firstName = "Jordan Reyes", lastName = "", username = "jordan.reyes",
            password = "goodpass1", email = null
        ) as AuthResult.Success).user
        val second = (repository.signUp(
            firstName = "Alex Kim", lastName = "", username = "alex.kim",
            password = "goodpass1", email = null
        ) as AuthResult.Success).user

        assertTrue(second.createdAt > first.createdAt)
        assertEquals(second, getPrimaryAccount())
    }
}
