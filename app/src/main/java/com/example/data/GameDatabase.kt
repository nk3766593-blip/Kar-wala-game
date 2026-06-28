package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "high_scores")
data class HighScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val score: Int,
    val coins: Int,
    val playerName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "garage_state")
data class GarageEntity(
    @PrimaryKey val id: Int = 1, // Single row for player state
    val coins: Int = 100, // Starts with some free coins
    val unlockedCarIds: String = "neon_cruiser", // Comma-separated list
    val activeCarId: String = "neon_cruiser",
    val speedUpgradeLevel: Int = 1,
    val fuelUpgradeLevel: Int = 1,
    val magnetUpgradeLevel: Int = 1
)

@Dao
interface GameDao {
    @Query("SELECT * FROM high_scores ORDER BY score DESC LIMIT 10")
    fun getTopHighScores(): Flow<List<HighScoreEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighScore(highScore: HighScoreEntity)

    @Query("SELECT * FROM garage_state WHERE id = 1 LIMIT 1")
    fun getGarageState(): Flow<GarageEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGarageState(garage: GarageEntity)
}

@Database(entities = [HighScoreEntity::class, GarageEntity::class], version = 1, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "neon_nitro_racing_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
