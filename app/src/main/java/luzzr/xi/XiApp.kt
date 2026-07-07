package luzzr.xi

import android.app.Application
import android.util.Log
import luzzr.xi.BuildConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class XiApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("Xi", "Uncaught exception in thread ${thread.name}", throwable)
            
            // Local crash logging
            try {
                val crashDir = java.io.File(cacheDir, "crash_logs")
                if (!crashDir.exists()) crashDir.mkdirs()
                
                val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                val crashFile = java.io.File(crashDir, "crash_$timeStamp.txt")
                
                crashFile.printWriter().use { out ->
                    out.println("App Version: luzzr.xi v${BuildConfig.VERSION_NAME}")
                    out.println("OS Version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
                    out.println("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                    out.println("Thread: ${thread.name}")
                    out.println("Time: $timeStamp")
                    out.println("\n--- Stack Trace ---")
                    throwable.printStackTrace(out)
                }
            } catch (e: Exception) {
                Log.e("Xi", "Failed to write crash log", e)
            }
            
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
