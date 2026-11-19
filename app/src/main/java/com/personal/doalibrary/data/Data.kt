package com.personal.doalibrary.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Entity(tableName = "prayers")
data class PrayerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val arabicText: String,
    val translation: String,
    val isFavorite: Boolean = false,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Dao
abstract class PrayerDao {
    @Query("SELECT * FROM prayers WHERE deletedAt IS NULL ORDER BY sortOrder ASC")
    abstract fun observeActive(): Flow<List<PrayerEntity>>

    @Query("SELECT * FROM prayers WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    abstract fun observeTrash(): Flow<List<PrayerEntity>>

    @Query("SELECT * FROM prayers WHERE id = :id")
    abstract suspend fun get(id: String): PrayerEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM prayers")
    abstract suspend fun maxOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(p: PrayerEntity)

    @Query("UPDATE prayers SET isFavorite = :f WHERE id = :id")
    abstract suspend fun setFav(id: String, f: Boolean)

    @Query("UPDATE prayers SET deletedAt = :t WHERE id = :id")
    abstract suspend fun setDeleted(id: String, t: Long?)

    @Query("UPDATE prayers SET sortOrder = :o WHERE id = :id")
    abstract suspend fun setOrder(id: String, o: Int)

    @Query("DELETE FROM prayers WHERE id = :id")
    abstract suspend fun purge(id: String)

    @Query("DELETE FROM prayers WHERE deletedAt IS NOT NULL")
    abstract suspend fun purgeAll()

    /** Urutan disimpan berdasarkan ID doa, bukan indeks daftar. Atomik. */
    @Transaction
    open suspend fun reorder(ids: List<String>) {
        ids.forEachIndexed { i, id -> setOrder(id, i) }
    }

    @Transaction
    open suspend fun restore(id: String) {
        setOrder(id, maxOrder() + 1)
        setDeleted(id, null)
    }
}

// Migration strategy: tambahkan Migration(1, 2) di sini saat skema berubah.
// Jangan gunakan fallbackToDestructiveMigration agar data doa tidak hilang.
@Database(entities = [PrayerEntity::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun prayerDao(): PrayerDao
}

@Module
@InstallIn(SingletonComponent::class)
object DbModule {
    @Provides @Singleton
    fun db(@ApplicationContext c: Context): AppDb =
        Room.databaseBuilder(c, AppDb::class.java, "doa.db").build()

    @Provides
    fun dao(db: AppDb): PrayerDao = db.prayerDao()
}

private val Context.store by preferencesDataStore("settings")

data class UiSettings(
    val theme: String = "system",
    val arSize: Int = 30, val arLine: Float = 2.1f, val arAlign: String = "right",
    val trSize: Int = 16, val trLine: Float = 1.6f, val trAlign: String = "left"
)

@Singleton
class SettingsRepo @Inject constructor(@ApplicationContext private val ctx: Context) {
    private object K {
        val theme = stringPreferencesKey("theme")
        val arSize = intPreferencesKey("arSize")
        val arLine = floatPreferencesKey("arLine")
        val arAlign = stringPreferencesKey("arAlign")
        val trSize = intPreferencesKey("trSize")
        val trLine = floatPreferencesKey("trLine")
        val trAlign = stringPreferencesKey("trAlign")
    }

    val flow: Flow<UiSettings> = ctx.store.data.map { p ->
        val d = UiSettings()
        UiSettings(
            p[K.theme] ?: d.theme,
            p[K.arSize] ?: d.arSize, p[K.arLine] ?: d.arLine, p[K.arAlign] ?: d.arAlign,
            p[K.trSize] ?: d.trSize, p[K.trLine] ?: d.trLine, p[K.trAlign] ?: d.trAlign
        )
    }

    suspend fun save(s: UiSettings) {
        ctx.store.edit { p ->
            p[K.theme] = s.theme
            p[K.arSize] = s.arSize; p[K.arLine] = s.arLine; p[K.arAlign] = s.arAlign
            p[K.trSize] = s.trSize; p[K.trLine] = s.trLine; p[K.trAlign] = s.trAlign
        }
    }
}
