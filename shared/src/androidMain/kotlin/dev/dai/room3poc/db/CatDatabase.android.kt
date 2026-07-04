package dev.dai.room3poc.db

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

// PoCのための簡易holder。アプリ側のApplication.onCreate()でappContextを設定する
object AndroidContextHolder {
    lateinit var appContext: Context
}

actual fun databaseBuilder(): RoomDatabase.Builder<CatDatabase> {
    val ctx = AndroidContextHolder.appContext
    return Room.databaseBuilder<CatDatabase>(
        context = ctx,
        name = ctx.getDatabasePath("cats.db").absolutePath,
    ).setDriver(BundledSQLiteDriver())
}
