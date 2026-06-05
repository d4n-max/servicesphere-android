package com.servicesphere

import android.app.Application
import com.servicesphere.data.ServiceLocator

class ServiceSphereApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
