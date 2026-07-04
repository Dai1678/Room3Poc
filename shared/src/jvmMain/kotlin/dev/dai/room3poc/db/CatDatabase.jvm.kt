package dev.dai.room3poc.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

actual fun databaseBuilder(): RoomDatabase.Builder<CatDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "cats.db")
    return Room.databaseBuilder<CatDatabase>(name = dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
}
