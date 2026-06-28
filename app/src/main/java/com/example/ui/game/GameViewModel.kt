package com.example.ui.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GarageEntity
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.HighScoreEntity
import com.example.ui.sound.RetroSoundPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

// Car data structures
data class CarDefinition(
    val id: String,
    val name: String,
    val cost: Int,
    val baseSpeed: Float,
    val fuelRate: Float,
    val width: Float, // normalized width
    val height: Float, // normalized height
    val primaryColor: Color,
    val glowColor: Color,
    val description: String
)

val AVAILABLE_CARS = listOf(
    CarDefinition(
        id = "neon_cruiser",
        name = "Neon Cruiser",
        cost = 0,
        baseSpeed = 0.012f,
        fuelRate = 0.0012f,
        width = 0.12f,
        height = 0.15f,
        primaryColor = Color(0xFF00E5FF), // Cyan
        glowColor = Color(0xFF00838F),
        description = "Standard arcade cruiser. Balanced stats."
    ),
    CarDefinition(
        id = "cyber_wedge",
        name = "Cyber Wedge",
        cost = 150,
        baseSpeed = 0.014f,
        fuelRate = 0.0010f,
        width = 0.14f,
        height = 0.16f,
        primaryColor = Color(0xFF39FF14), // Neon Green
        glowColor = Color(0xFF1B5E20),
        description = "Heavy solar engine. Excellent fuel economy."
    ),
    CarDefinition(
        id = "formula_flame",
        name = "Formula Flame",
        cost = 400,
        baseSpeed = 0.017f,
        fuelRate = 0.0016f,
        width = 0.11f,
        height = 0.15f,
        primaryColor = Color(0xFFFF073A), // Neon Red
        glowColor = Color(0xFFB71C1C),
        description = "Ultra high speed open-wheeler. Thirsty engine."
    ),
    CarDefinition(
        id = "galaxy_phantom",
        name = "Galaxy Phantom",
        cost = 800,
        baseSpeed = 0.020f,
        fuelRate = 0.0014f,
        width = 0.13f,
        height = 0.16f,
        primaryColor = Color(0xFFFF00FF), // Neon Purple/Pink
        glowColor = Color(0xFF4A148C),
        description = "Hyperdrive elite hypercar. Peak speed performance."
    )
)

// Game items
data class GameEntity(
    val id: Long,
    val x: Float, // normalized 0.15f to 0.85f
    var y: Float, // normalized 0f to 1f
    val type: EntityType,
    val speedMultiplier: Float = 1.0f
)

enum class EntityType {
    COIN, FUEL, OBSTACLE_BLUE, OBSTACLE_RED, OBSTACLE_TRUCK, MAGNET, SHIELD
}

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    var alpha: Float,
    var life: Int
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: GameRepository

    val topScoresFlow: StateFlow<List<HighScoreEntity>>
    val garageFlow: StateFlow<GarageEntity?>

    // Active playing states
    var isPlaying by mutableStateOf(false)
        private set
    var isGameOver by mutableStateOf(false)
        private set
    var currentScore by mutableStateOf(0)
        private set
    var currentCoinsCollected by mutableStateOf(0)
        private set
    var currentDistance by mutableStateOf(0.0f) // in meters
    var fuelLevel by mutableStateOf(1.0f) // 0.0 to 1.0
    var shieldActive by mutableStateOf(false)
    var magnetActive by mutableStateOf(false)
    var isBoosting by mutableStateOf(false)

    // Upgrade/Garage levels & stats
    var playerCoins by mutableStateOf(100)
        private set
    var unlockedCarsList by mutableStateOf(setOf("neon_cruiser"))
        private set
    var selectedCarId by mutableStateOf("neon_cruiser")
        private set
    var speedUpgradeLevel by mutableStateOf(1)
        private set
    var fuelUpgradeLevel by mutableStateOf(1)
        private set
    var magnetUpgradeLevel by mutableStateOf(1)
        private set

    // Active car details
    val currentCar: CarDefinition
        get() = AVAILABLE_CARS.find { it.id == selectedCarId } ?: AVAILABLE_CARS[0]

    // Screen navigation
    var currentScreen by mutableStateOf("home") // home, play, garage, leaderboards

    // Dynamic gameplay objects
    var playerX by mutableStateOf(0.5f) // centered initially
    var activeEntities = mutableStateOf<List<GameEntity>>(emptyList())
    var activeParticles = mutableStateOf<List<Particle>>(emptyList())
    var roadScrollOffset by mutableStateOf(0f)

    private var gameLoopJob: Job? = null
    private var entityIdCounter = 0L

    init {
        val database = GameDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())

        topScoresFlow = repository.topHighScores.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        garageFlow = repository.garageState.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        // Observe garage updates from DB to keep VM in sync
        viewModelScope.launch {
            garageFlow.collect { garage ->
                if (garage != null) {
                    playerCoins = garage.coins
                    unlockedCarsList = garage.unlockedCarIds.split(",").toSet()
                    selectedCarId = garage.activeCarId
                    speedUpgradeLevel = garage.speedUpgradeLevel
                    fuelUpgradeLevel = garage.fuelUpgradeLevel
                    magnetUpgradeLevel = garage.magnetUpgradeLevel
                } else {
                    // Initialize first-time DB entity if empty
                    repository.saveGarageState(GarageEntity())
                }
            }
        }
    }

    fun setScreen(screen: String) {
        currentScreen = screen
        RetroSoundPlayer.playClickSound()
        if (screen == "play") {
            resetGame()
        } else {
            isPlaying = false
        }
    }

    private fun resetGame() {
        isPlaying = false
        isGameOver = false
        currentScore = 0
        currentCoinsCollected = 0
        currentDistance = 0f
        fuelLevel = 1.0f
        playerX = 0.5f
        shieldActive = false
        magnetActive = false
        isBoosting = false
        activeEntities.value = emptyList()
        activeParticles.value = emptyList()
        roadScrollOffset = 0f
    }

    fun startGame() {
        resetGame()
        isPlaying = true
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            runGameLoop()
        }
    }

    // Horizontal Movement
    fun moveLeft() {
        if (!isPlaying || isGameOver) return
        playerX = (playerX - 0.06f).coerceAtLeast(0.18f)
    }

    fun moveRight() {
        if (!isPlaying || isGameOver) return
        playerX = (playerX + 0.06f).coerceAtMost(0.82f)
    }

    fun toggleBoost(boost: Boolean) {
        if (!isPlaying || isGameOver) return
        if (boost && !isBoosting) {
            RetroSoundPlayer.playBoostSound()
        }
        isBoosting = boost
    }

    private suspend fun runGameLoop() {
        var tickCount = 0L
        while (isPlaying && !isGameOver) {
            tickCount++

            // Calculate speeds based on active car & speed upgrade levels
            val carSpeedFactor = currentCar.baseSpeed * (1f + speedUpgradeLevel * 0.06f)
            val boostMultiplier = if (isBoosting) 1.7f else 1.0f
            val finalTickSpeed = carSpeedFactor * boostMultiplier

            // Update score & distance
            currentDistance += finalTickSpeed * 120f
            val scoreEarned = if (isBoosting) 2 else 1
            currentScore += scoreEarned

            // Deplete Fuel
            val fuelDepletionFactor = currentCar.fuelRate * (1f - (fuelUpgradeLevel - 1) * 0.05f)
            val boostFuelFactor = if (isBoosting) 1.8f else 1.0f
            fuelLevel = (fuelLevel - (fuelDepletionFactor * boostFuelFactor)).coerceAtLeast(0f)

            if (fuelLevel <= 0f) {
                triggerGameOver("Out of Fuel!")
                break
            }

            // Scroll the road background
            roadScrollOffset = (roadScrollOffset + finalTickSpeed * 15f) % 1f

            // Handle Entities Movement & Mechanics
            val currentList = activeEntities.value.toMutableList()
            val iterator = currentList.iterator()
            val magnetRange = 0.15f + (magnetUpgradeLevel * 0.03f)

            while (iterator.hasNext()) {
                val entity = iterator.next()
                // Move item downwards relative to player speed
                entity.y += finalTickSpeed * 1.05f + (if (entity.type == EntityType.OBSTACLE_RED) 0.005f else 0f)

                // Magnet Powerup Effect: pulls coins toward the player
                if (magnetActive && entity.type == EntityType.COIN) {
                    val dx = playerX - entity.x
                    val dy = 0.85f - entity.y
                    val distanceToPlayer = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (distanceToPlayer < magnetRange) {
                        // Magnetic pull towards player car
                        val pullForce = 0.03f
                        entity.y += (0.85f - entity.y) * pullForce
                        val newX = entity.x + (playerX - entity.x) * pullForce
                        // Reflect coordinate update safely
                        val updatedEntity = entity.copy(x = newX)
                        currentList[currentList.indexOf(entity)] = updatedEntity
                    }
                }

                // Check out of bounds (off bottom screen)
                if (entity.y > 1.05f) {
                    iterator.remove()
                    continue
                }

                // Check collision with player car
                if (checkCollision(entity)) {
                    iterator.remove()
                    handleCollision(entity)
                    continue
                }
            }
            activeEntities.value = currentList

            // Spawn mechanics
            if (tickCount % getSpawnInterval() == 0L) {
                spawnRandomEntity()
            }

            // Update and clear particles
            updateParticles()

            // Spawn engine particles
            spawnEngineExhaustParticles()

            delay(20) // approx ~50 updates per second (smooth rendering)
        }
    }

    private fun getSpawnInterval(): Int {
        // Higher speeds and score reduce interval slightly for a high-intensity vibe
        val baseInterval = 28
        val reduction = (currentScore / 600).coerceAtMost(12)
        return (baseInterval - reduction).coerceAtLeast(15)
    }

    private fun spawnRandomEntity() {
        val rand = Random.nextFloat()
        val laneX = when (Random.nextInt(4)) {
            0 -> 0.28f
            1 -> 0.42f
            2 -> 0.58f
            else -> 0.72f
        }

        val type = when {
            rand < 0.35f -> {
                // Obstacles
                val obsType = Random.nextInt(3)
                when (obsType) {
                    0 -> EntityType.OBSTACLE_BLUE
                    1 -> EntityType.OBSTACLE_RED // moves faster
                    else -> EntityType.OBSTACLE_TRUCK // larger obstruction
                }
            }
            rand < 0.70f -> EntityType.COIN
            rand < 0.85f -> EntityType.FUEL
            rand < 0.92f -> EntityType.SHIELD
            else -> EntityType.MAGNET
        }

        val newEntity = GameEntity(
            id = entityIdCounter++,
            x = laneX,
            y = -0.1f,
            type = type
        )
        activeEntities.value = activeEntities.value + newEntity
    }

    private fun checkCollision(entity: GameEntity): Boolean {
        val pCar = currentCar
        // Player bounding box (centered on playerX horizontally, Y is at 0.82f - 0.97f)
        val pLeft = playerX - (pCar.width / 2)
        val pRight = playerX + (pCar.width / 2)
        val pTop = 0.80f
        val pBottom = 0.80f + pCar.height

        // Entity bounding box
        val eWidth = getEntityWidth(entity.type)
        val eHeight = getEntityHeight(entity.type)
        val eLeft = entity.x - (eWidth / 2)
        val eRight = entity.x + (eWidth / 2)
        val eTop = entity.y - (eHeight / 2)
        val eBottom = entity.y + (eHeight / 2)

        // Axis-Aligned Bounding Box (AABB) intersection check
        return pLeft < eRight && pRight > eLeft && pTop < eBottom && pBottom > eTop
    }

    private fun getEntityWidth(type: EntityType): Float {
        return when (type) {
            EntityType.COIN -> 0.08f
            EntityType.FUEL -> 0.09f
            EntityType.MAGNET, EntityType.SHIELD -> 0.09f
            EntityType.OBSTACLE_TRUCK -> 0.14f
            else -> 0.11f // obstacle cars
        }
    }

    private fun getEntityHeight(type: EntityType): Float {
        return when (type) {
            EntityType.COIN -> 0.08f
            EntityType.FUEL -> 0.10f
            EntityType.MAGNET, EntityType.SHIELD -> 0.10f
            EntityType.OBSTACLE_TRUCK -> 0.20f
            else -> 0.15f
        }
    }

    private fun handleCollision(entity: GameEntity) {
        when (entity.type) {
            EntityType.COIN -> {
                currentCoinsCollected += 5
                RetroSoundPlayer.playCoinSound()
                spawnDebris(entity.x, entity.y, Color(0xFFFFD700), count = 6)
            }
            EntityType.FUEL -> {
                fuelLevel = (fuelLevel + 0.30f).coerceAtMost(1.0f)
                RetroSoundPlayer.playPowerupSound()
                spawnDebris(entity.x, entity.y, Color(0xFF00FF00), count = 8)
            }
            EntityType.SHIELD -> {
                shieldActive = true
                RetroSoundPlayer.playPowerupSound()
                spawnDebris(entity.x, entity.y, Color(0xFF00E5FF), count = 10)
            }
            EntityType.MAGNET -> {
                magnetActive = true
                RetroSoundPlayer.playPowerupSound()
                spawnDebris(entity.x, entity.y, Color(0xFFFF00FF), count = 10)
                viewModelScope.launch {
                    delay(10000) // 10 second magnet duration
                    magnetActive = false
                }
            }
            EntityType.OBSTACLE_BLUE, EntityType.OBSTACLE_RED, EntityType.OBSTACLE_TRUCK -> {
                if (shieldActive) {
                    shieldActive = false
                    RetroSoundPlayer.playPowerupSound() // shield burst sound
                    spawnDebris(entity.x, entity.y, Color(0xFF00E5FF), count = 20)
                } else {
                    triggerGameOver("CRASHED!")
                }
            }
        }
    }

    private fun triggerGameOver(reason: String) {
        isGameOver = true
        isPlaying = false
        RetroSoundPlayer.playCrashSound()

        // Create massive explosion debris
        spawnDebris(playerX, 0.85f, currentCar.primaryColor, count = 35)
        spawnDebris(playerX, 0.85f, Color(0xFFFF3D00), count = 15) // Fire orange

        // Persist rewards and high score
        viewModelScope.launch {
            val totalCoinsEarned = currentCoinsCollected
            val currentGarage = garageFlow.value ?: GarageEntity()
            val updatedCoins = currentGarage.coins + totalCoinsEarned

            // Save high score to Room
            repository.insertHighScore(
                HighScoreEntity(
                    score = currentScore,
                    coins = totalCoinsEarned,
                    playerName = "Racer ${Random.nextInt(100, 999)}"
                )
            )

            // Save updated coins balance to Room
            repository.saveGarageState(
                currentGarage.copy(coins = updatedCoins)
            )
        }
    }

    private fun spawnDebris(x: Float, y: Float, color: Color, count: Int) {
        val newParticles = (1..count).map {
            val angle = Random.nextFloat() * 2.0 * Math.PI
            val speed = Random.nextFloat() * 0.015f + 0.005f
            Particle(
                x = x,
                y = y,
                vx = (kotlin.math.cos(angle) * speed).toFloat(),
                vy = (kotlin.math.sin(angle) * speed).toFloat(),
                color = color,
                size = Random.nextFloat() * 8f + 4f,
                alpha = 1f,
                life = Random.nextInt(15, 30)
            )
        }
        activeParticles.value = activeParticles.value + newParticles
    }

    private fun spawnEngineExhaustParticles() {
        if (!isPlaying || isGameOver) return
        val exhaustY = 0.88f + currentCar.height
        val size = if (isBoosting) 12f else 6f
        val color = if (isBoosting) Color(0xFFFF3D00) else currentCar.primaryColor

        val particle = Particle(
            x = playerX,
            y = exhaustY,
            vx = (Random.nextFloat() - 0.5f) * 0.005f,
            vy = Random.nextFloat() * 0.01f + 0.01f, // push down
            color = color,
            size = Random.nextFloat() * size + 2f,
            alpha = 0.8f,
            life = 8
        )
        activeParticles.value = activeParticles.value + particle
    }

    private fun updateParticles() {
        val list = activeParticles.value.toMutableList()
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life--
            if (p.life <= 0) {
                iterator.remove()
                continue
            }
            // Apply drift velocity
            val updatedX = p.x + p.vx
            val updatedY = p.y + p.vy
            val updatedAlpha = (p.life.toFloat() / 25f).coerceAtMost(1f)

            list[list.indexOf(p)] = p.copy(x = updatedX, y = updatedY, alpha = updatedAlpha)
        }
        activeParticles.value = list
    }

    // Garage/Upgrades actions
    fun selectCar(carId: String) {
        if (unlockedCarsList.contains(carId)) {
            selectedCarId = carId
            RetroSoundPlayer.playClickSound()
            saveGarageState()
        }
    }

    fun purchaseCar(carDef: CarDefinition) {
        if (playerCoins >= carDef.cost && !unlockedCarsList.contains(carDef.id)) {
            val updatedCoins = playerCoins - carDef.cost
            val updatedUnlocked = unlockedCarsList + carDef.id
            playerCoins = updatedCoins
            unlockedCarsList = updatedUnlocked
            selectedCarId = carDef.id // auto-select
            RetroSoundPlayer.playPowerupSound()
            saveGarageState()
        } else {
            RetroSoundPlayer.playCrashSound() // fail buzzer
        }
    }

    fun upgradeStat(statType: String) {
        val cost = getUpgradeCost(statType)
        if (playerCoins >= cost) {
            playerCoins -= cost
            when (statType) {
                "speed" -> speedUpgradeLevel++
                "fuel" -> fuelUpgradeLevel++
                "magnet" -> magnetUpgradeLevel++
            }
            RetroSoundPlayer.playPowerupSound()
            saveGarageState()
        } else {
            RetroSoundPlayer.playCrashSound() // fail buzzer
        }
    }

    fun getUpgradeCost(statType: String): Int {
        val currentLevel = when (statType) {
            "speed" -> speedUpgradeLevel
            "fuel" -> fuelUpgradeLevel
            "magnet" -> magnetUpgradeLevel
            else -> 1
        }
        if (currentLevel >= 5) return Int.MAX_VALUE // max level reached
        return currentLevel * 100
    }

    private fun saveGarageState() {
        viewModelScope.launch {
            val garage = GarageEntity(
                coins = playerCoins,
                unlockedCarIds = unlockedCarsList.joinToString(","),
                activeCarId = selectedCarId,
                speedUpgradeLevel = speedUpgradeLevel,
                fuelUpgradeLevel = fuelUpgradeLevel,
                magnetUpgradeLevel = magnetUpgradeLevel
            )
            repository.saveGarageState(garage)
        }
    }
}
