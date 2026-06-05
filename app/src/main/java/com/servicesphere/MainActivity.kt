package com.servicesphere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.servicesphere.ui.ServiceSphereApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent { ServiceSphereApp(initialJobId = intent.getStringExtra(EXTRA_JOB_ID)) }
    }

    companion object {
        const val EXTRA_JOB_ID = "job_id"
    }
}
