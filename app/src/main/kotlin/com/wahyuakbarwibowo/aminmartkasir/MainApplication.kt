package com.wahyuakbarwibowo.aminmartkasir

import android.app.Application
import com.wahyuakbarwibowo.aminmartkasir.data.remote.FeatureAccessManager
import com.wahyuakbarwibowo.aminmartkasir.data.remote.AuthManager
import com.wahyuakbarwibowo.aminmartkasir.data.sync.ProductSyncScheduler

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        FeatureAccessManager.init(applicationContext)
        ProductSyncScheduler.init(applicationContext)
        AuthManager.init(applicationContext)
    }
}
