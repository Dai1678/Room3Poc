package dev.dai.room3poc.db

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSTemporaryDirectory
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class CatDaoIosTest {

    private fun builderFor(dbPath: String) =
        Room.databaseBuilder<CatDatabase>(name = dbPath)
            .setDriver(BundledSQLiteDriver())

    @Test
    fun insertAndQueryThenReopenPersists() = runBlocking {
        val dbPath = NSTemporaryDirectory() + "cats-test-${Random.nextLong()}.db"

        val db = builderFor(dbPath).build()
        db.dao().insertCat("Tama")
        db.dao().insertCat("Mike")
        assertEquals(listOf("Tama", "Mike"), db.dao().getCatsFlow().first().map { it.name })
        db.close()

        // 再オープンでデータが残っていること（アプリ再起動の代替検証）
        val reopened = builderFor(dbPath).build()
        assertEquals(listOf("Tama", "Mike"), reopened.dao().getCatsFlow().first().map { it.name })
        reopened.close()
    }
}
