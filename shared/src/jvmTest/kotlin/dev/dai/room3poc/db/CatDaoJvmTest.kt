package dev.dai.room3poc.db

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class CatDaoJvmTest {

    private fun builderFor(dbFile: File) =
        Room.databaseBuilder<CatDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())

    @Test
    fun insertAndQueryThenReopenPersists() = runBlocking {
        val dbFile = File.createTempFile("cats-test", ".db").apply { deleteOnExit() }

        val db = builderFor(dbFile).build()
        db.dao().insertCat("Tama")
        db.dao().insertCat("Mike")
        assertEquals(listOf("Tama", "Mike"), db.dao().getCatsFlow().first().map { it.name })
        db.close()

        // 再オープンでデータが残っていること（アプリ再起動の代替検証）
        val reopened = builderFor(dbFile).build()
        assertEquals(listOf("Tama", "Mike"), reopened.dao().getCatsFlow().first().map { it.name })
        reopened.close()
    }
}
