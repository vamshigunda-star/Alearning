package com.example.alearning.data.repository

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.alearning.domain.repository.AiCoachStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AiCoachRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var repository: AiCoachRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = AiCoachRepositoryImpl(context)
    }

    @Test
    fun `initial status is DOWNLOADING`() {
        assertEquals(AiCoachStatus.DOWNLOADING, repository.status.value)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun `checkAvailability emits UNSUPPORTED when SDK is below Q`() = runTest {
        repository.checkAvailability()
        assertEquals(AiCoachStatus.UNSUPPORTED, repository.status.value)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `checkAvailability emits ERROR on exception`() = runTest {
        // Under Robolectric, the GenerativeModel creation will likely fail 
        // because native AICore components aren't available, so we expect ERROR.
        repository.checkAvailability()
        assertEquals(AiCoachStatus.ERROR, repository.status.value)
    }
    
    @Test
    fun `sendMessage returns unavailability message when status is not READY`() = runTest {
        val message = repository.sendMessage("Hello", null)
        assertEquals("AI Coach is currently unavailable on this device.", message)
    }
}
