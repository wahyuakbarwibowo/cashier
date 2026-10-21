package com.wahyuakbarwibowo.aminmartkasir

import android.app.Application
import com.wahyuakbarwibowo.aminmartkasir.data.remote.FeatureAccessManager

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize other application-level components here
        FeatureAccessManager.init(applicationContext)
    }
}
