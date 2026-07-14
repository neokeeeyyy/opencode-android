package ai.opencode.android

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OpenCodeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("OpenCodeApp", "Application created")
    }
}
