package dev.dai.room3poc.db

import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import kotlinx.coroutines.flow.Flow

@Entity
data class Cat(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Dao
interface CatDao {
    // Room 3.0ではブロッキングDAO関数は不可。suspendまたはFlow等のリアクティブ型が必須
    @Query("INSERT INTO Cat (name) VALUES (:name)")
    suspend fun insertCat(name: String)

    @Query("SELECT * FROM Cat")
    fun getCatsFlow(): Flow<List<Cat>>
}

@Database(entities = [Cat::class], version = 1)
@ConstructedBy(CatDatabaseConstructor::class)
abstract class CatDatabase : RoomDatabase() {
    abstract fun dao(): CatDao
}

// actualはRoomのKSPコンパイラが各プラットフォーム向けに自動生成する
@Suppress("KotlinNoActualForExpected")
expect object CatDatabaseConstructor : RoomDatabaseConstructor<CatDatabase> {
    override fun initialize(): CatDatabase
}

expect fun databaseBuilder(): RoomDatabase.Builder<CatDatabase>
