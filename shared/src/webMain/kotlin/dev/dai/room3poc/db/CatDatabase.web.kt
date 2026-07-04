package dev.dai.room3poc.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import dev.dai.room3poc.worker.createSQLiteWasmWorker

actual fun databaseBuilder(): RoomDatabase.Builder<CatDatabase> {
    // デモリポジトリはinMemoryDatabaseBuilderだが、OPFS永続化には名前付きbuilderを使う。
    // このnameがworker.js内でOpfsDbのファイル名になる
    return Room.databaseBuilder<CatDatabase>(name = "cats.db")
        .setDriver(createSQLiteWasmWorker())
}
