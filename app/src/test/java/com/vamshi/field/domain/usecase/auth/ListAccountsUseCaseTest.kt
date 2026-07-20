package com.vamshi.field.domain.usecase.auth

import com.vamshi.field.domain.model.auth.AuthResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListAccountsUseCaseTest {

    private val repository = FakeAuthRepository()
    private val listAccounts = ListAccountsUseCase(repository)

    @Test
    fun `empty repository returns an empty list`() = runTest {
        assertTrue(listAccounts().isEmpty())
    }

    @Test
    fun `accounts are returned most recently created first`() = runTest {
        val first = (repository.signUp(
            firstName = "Jordan Reyes", lastName = "", username = "jordan.reyes",
            password = "goodpass1", email = null
        ) as AuthResult.Success).user
        val second = (repository.signUp(
            firstName = "Alex Kim", lastName = "", username = "alex.kim",
            password = "goodpass1", email = null
        ) as AuthResult.Success).user

        assertEquals(listOf(second, first), listAccounts())
    }
}
