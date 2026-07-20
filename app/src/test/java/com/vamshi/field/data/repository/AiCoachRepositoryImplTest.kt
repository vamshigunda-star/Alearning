package com.vamshi.field.data.repository

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.vamshi.field.domain.repository.AiCoachStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `checkAvailability on Q+ resolves past the SDK gate`() = runTest {
        // Whether GenerativeModel construction throws under Robolectric depends on the
        // aicore version, so accept either terminal outcome — the SDK gate is what's
        // under test: on Q+ the status must never be UNSUPPORTED or stuck DOWNLOADING.
        repository.checkAvailability()
        val status = repository.status.value
        assertTrue(
            "Expected READY or ERROR but was $status",
            status == AiCoachStatus.READY || status == AiCoachStatus.ERROR
        )
    }
    
    @Test
    fun `sendMessage returns unavailability message when status is not READY`() = runTest {
        val message = repository.sendMessage("Hello", null)
        assertEquals("AI Coach is currently unavailable on this device.", message)
    }
}
