package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchFaceDao {
    @Query("SELECT * FROM watchfaces ORDER BY lastModified DESC")
    fun getAllWatchFaces(): Flow<List<WatchFaceEntity>>

    @Query("SELECT * FROM watchfaces WHERE id = :id")
    suspend fun getWatchFaceById(id: Int): WatchFaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchFace(watchFace: WatchFaceEntity): Long

    @Query("DELETE FROM watchfaces WHERE id = :id")
    suspend fun deleteWatchFace(id: Int)
}

@Database(entities = [WatchFaceEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchFaceDao(): WatchFaceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "watchface_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class WatchFaceRepository(private val dao: WatchFaceDao) {
    val allWatchFaces: Flow<List<WatchFaceEntity>> = dao.getAllWatchFaces()

    suspend fun getById(id: Int): WatchFaceEntity? = dao.getWatchFaceById(id)

    suspend fun save(watchFace: WatchFaceEntity): Long = dao.insertWatchFace(watchFace)

    suspend fun delete(id: Int) = dao.deleteWatchFace(id)
}
