package com.mikewarren.myclerkapp

import android.app.Application
import com.clerk.api.Clerk

class MyClerkApp: Application() {
    override fun onCreate() {
        super.onCreate()
        Clerk.initialize(
            this,
            publishableKey = "pk_test_ZW5vdWdoLWplbm5ldC0xNy5jbGVyay5hY2NvdW50cy5kZXYk"
        )
    }
}