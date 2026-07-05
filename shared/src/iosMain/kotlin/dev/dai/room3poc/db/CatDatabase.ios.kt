package dev.dai.room3poc.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun databaseBuilder(): RoomDatabase.Builder<CatDatabase> {
    @OptIn(ExperimentalForeignApi::class)
    val documentDir = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )!!.path!!
    return Room.databaseBuilder<CatDatabase>(name = "$documentDir/cats.db")
        .setDriver(BundledSQLiteDriver())
}
