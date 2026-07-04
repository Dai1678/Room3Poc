package dev.dai.room3poc

import android.app.Application
import dev.dai.room3poc.db.AndroidContextHolder

class Room3PocApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidContextHolder.appContext = applicationContext
    }
}
