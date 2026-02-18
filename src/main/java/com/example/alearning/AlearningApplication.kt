package com.example.alearning

import android.app.Application
import com.example.alearning.data.seed.SeedDataManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AlearningApplication : Application() {

    @Inject lateinit var seedDataManager: SeedDataManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            seedDataManager.seedIfNeeded()
        }
    }
}
