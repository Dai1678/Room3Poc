package dev.dai.room3poc.db

import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Query
import androidx.room3.RoomDatabase

// 【検証用】Room 3.0の「suspend必須」の適用範囲を実証するプローブ。
//
// commonMainに非suspendのDAO関数を書くとKSPがエラーで拒否するが（再現はこのブランチの
// 1つ前のコミットをcheckout）、このようにAndroid専用ソースセットに置いたDAOなら
// 従来型（blocking）の関数もコンパイルが通る。
// つまり「suspend必須」の正確なスコープは「非Androidプラットフォームを対象とする
// ソースセット」であり、Android専用DAOは2.x互換のまま3.0へ移行できる。
//
// 確認コマンド: ./gradlew :shared:compileAndroidMain（成功する）
@Dao
interface BlockingProbeDao {
    @Query("SELECT * FROM Cat")
    fun getCatsBlocking(): List<Cat>
}

@Database(entities = [Cat::class], version = 1)
abstract class BlockingProbeDatabase : RoomDatabase() {
    abstract fun dao(): BlockingProbeDao
}
