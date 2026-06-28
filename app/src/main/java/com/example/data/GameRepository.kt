package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {
    val topHighScores: Flow<List<HighScoreEntity>> = gameDao.getTopHighScores()
    val garageState: Flow<GarageEntity?> = gameDao.getGarageState()

    suspend fun insertHighScore(highScore: HighScoreEntity) {
        gameDao.insertHighScore(highScore)
    }

    suspend fun saveGarageState(garage: GarageEntity) {
        gameDao.saveGarageState(garage)
    }
}
