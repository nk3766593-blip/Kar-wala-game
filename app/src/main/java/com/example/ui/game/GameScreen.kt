package com.example.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.sound.RetroSoundPlayer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Shared Cyberpunk neon color palette
val DarkSpaceBackground = Color(0xFF070514)
val NeonCyan = Color(0xFF00E5FF)
val NeonMagenta = Color(0xFFFF007F)
val NeonGreen = Color(0xFF39FF14)
val NeonYellow = Color(0xFFFFD700)
val DarkGreyGlass = Color(0xBE1E1B2D)

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val topScores by viewModel.topScoresFlow.collectAsStateWithLifecycle()
    val garageState by viewModel.garageFlow.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkSpaceBackground, Color(0xFF0D0A24), Color(0xFF05030F))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        when (viewModel.currentScreen) {
            "home" -> MainHomeScreen(viewModel, garageState)
            "play" -> ActiveGameScreen(viewModel)
            "garage" -> GarageScreen(viewModel)
            "leaderboards" -> LeaderboardsScreen(viewModel, topScores)
        }
    }
}

// 1. HOME SCREEN
@Composable
fun MainHomeScreen(viewModel: GameViewModel, garage: com.example.data.GarageEntity?) {
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper stats banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(DarkGreyGlass, RoundedCornerShape(12.dp))
                    .border(1.dp, NeonYellow.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Coins",
                    tint = NeonYellow,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${viewModel.playerCoins}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            }

            Text(
                text = "V1.2 ARCADE",
                color = NeonCyan.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Title and decorative illustration
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Background glow behind title
                Box(
                    modifier = Modifier
                        .size(240.dp, 80.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(NeonMagenta.copy(alpha = 0.25f), Color.Transparent)
                            )
                        )
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NEON NITRO",
                        color = NeonCyan,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("home_title")
                    )
                    Text(
                        text = "R A C I N G",
                        color = NeonMagenta,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 8.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Glowing Wireframe Car silhouette preview
            Box(
                modifier = Modifier
                    .size(280.dp, 130.dp)
                    .background(DarkGreyGlass, RoundedCornerShape(16.dp))
                    .border(2.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val carDef = viewModel.currentCar
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawHomeCarPreview(carDef)
                }
                Text(
                    text = carDef.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(DarkSpaceBackground.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        // Menu buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // PLAY BUTTON
            Button(
                onClick = { viewModel.setScreen("play"); viewModel.startGame() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                    .testTag("play_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = DarkSpaceBackground,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START RACE",
                        color = DarkSpaceBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // GARAGE
                MenuSecondaryButton(
                    text = "GARAGE",
                    icon = Icons.Default.ShoppingCart,
                    color = NeonMagenta,
                    modifier = Modifier.weight(1f).testTag("garage_button")
                ) {
                    viewModel.setScreen("garage")
                }

                // LEADERBOARD
                MenuSecondaryButton(
                    text = "RECORDS",
                    icon = Icons.Default.List,
                    color = NeonGreen,
                    modifier = Modifier.weight(1f).testTag("records_button")
                ) {
                    viewModel.setScreen("leaderboards")
                }
            }
        }
    }
}

@Composable
fun MenuSecondaryButton(
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        border = BorderStroke(2.dp, color),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// 2. ACTIVE PLAY SCREEN
@Composable
fun ActiveGameScreen(viewModel: GameViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main visual scrolling highway (Weight occupies most screen)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
            ) {
                GameCanvas(viewModel)
                GameHudOverlay(viewModel)
            }

            // High-precision ergonomic controller layout at bottom
            InteractiveControls(viewModel)
        }

        // Overlay game over dialog state
        AnimatedVisibility(
            visible = viewModel.isGameOver,
            enter = fadeIn(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            GameOverOverlay(viewModel)
        }
    }
}

@Composable
fun GameCanvas(viewModel: GameViewModel) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { /* Eat clicks */ }
            }
    ) {
        val w = size.width
        val h = size.height

        // 1. Draw glowing space vertical background stars
        drawRect(color = Color(0xFF03020A), size = size)

        // 2. Draw central racing highway lanes (Normalized road: 0.18f to 0.82f)
        val roadLeft = 0.18f * w
        val roadRight = 0.82f * w
        val roadWidth = roadRight - roadLeft

        // Draw asphalt base with futuristic dark mesh grid
        drawRect(
            color = Color(0xFF0C091A),
            topLeft = Offset(roadLeft, 0f),
            size = Size(roadWidth, h)
        )

        // Draw grid horizontal lines to amplify speed perception
        val gridLinesCount = 15
        for (i in 0..gridLinesCount) {
            val ratio = (i.toFloat() / gridLinesCount + viewModel.roadScrollOffset) % 1f
            val gridY = ratio * h
            drawLine(
                color = Color(0xFF1E143A).copy(alpha = 0.4f),
                start = Offset(roadLeft, gridY),
                end = Offset(roadRight, gridY),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw side neon barriers (L & R borders)
        drawLine(
            brush = Brush.verticalGradient(listOf(NeonMagenta, NeonCyan)),
            start = Offset(roadLeft, 0f),
            end = Offset(roadLeft, h),
            strokeWidth = 4.dp.toPx()
        )
        drawLine(
            brush = Brush.verticalGradient(listOf(NeonCyan, NeonMagenta)),
            start = Offset(roadRight, 0f),
            end = Offset(roadRight, h),
            strokeWidth = 4.dp.toPx()
        )

        // Draw scrolling lane dividers (4 lanes, 3 divider lines)
        val dividersCount = 3
        val laneWidth = roadWidth / 4f
        for (d in 1..dividersCount) {
            val divX = roadLeft + d * laneWidth
            // Draw dotted line that scrolls
            val dashLength = 40.dp.toPx()
            val dashGap = 30.dp.toPx()
            var currentY = -dashLength + (viewModel.roadScrollOffset * (dashLength + dashGap))

            while (currentY < h) {
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(divX, currentY),
                    end = Offset(divX, currentY + dashLength),
                    strokeWidth = 2.dp.toPx()
                )
                currentY += dashLength + dashGap
            }
        }

        // 3. Draw Collectibles & Obstacles
        viewModel.activeEntities.value.forEach { entity ->
            val ex = entity.x * w
            val ey = entity.y * h

            when (entity.type) {
                EntityType.COIN -> {
                    // Glowing rotating coin
                    drawCircle(color = NeonYellow, radius = 10.dp.toPx(), center = Offset(ex, ey))
                    drawCircle(
                        color = Color.White,
                        radius = 6.dp.toPx(),
                        center = Offset(ex, ey),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                EntityType.FUEL -> {
                    // Fuel pack (green canister with a white lightning bolt / plus)
                    val rw = 18.dp.toPx()
                    val rh = 24.dp.toPx()
                    drawRoundRect(
                        color = NeonGreen,
                        topLeft = Offset(ex - rw/2, ey - rh/2),
                        size = Size(rw, rh),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    // Inner lightning icon representation
                    drawLine(
                        color = Color.White,
                        start = Offset(ex, ey - 6.dp.toPx()),
                        end = Offset(ex, ey + 6.dp.toPx()),
                        strokeWidth = 3.dp.toPx()
                    )
                }
                EntityType.SHIELD -> {
                    // Blue shield powerup orb
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.3f),
                        radius = 14.dp.toPx(),
                        center = Offset(ex, ey)
                    )
                    drawCircle(
                        color = NeonCyan,
                        radius = 12.dp.toPx(),
                        center = Offset(ex, ey),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                EntityType.MAGNET -> {
                    // Purple magnet powerup
                    val mw = 16.dp.toPx()
                    val mh = 20.dp.toPx()
                    drawRoundRect(
                        color = NeonMagenta,
                        topLeft = Offset(ex - mw/2, ey - mh/2),
                        size = Size(mw, mh),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                    drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(ex, ey))
                }
                EntityType.OBSTACLE_BLUE -> {
                    // Oncoming standard cyber-car (blue)
                    drawObstacleCar(ex, ey, Color(0xFF0D47A1), NeonCyan, size)
                }
                EntityType.OBSTACLE_RED -> {
                    // Fast sports car (red)
                    drawObstacleCar(ex, ey, Color(0xFF880E4F), NeonMagenta, size)
                }
                EntityType.OBSTACLE_TRUCK -> {
                    // Large obstacle truck (yellow/orange)
                    drawObstacleTruck(ex, ey, size)
                }
            }
        }

        // 4. Draw Particles (Debris & Exhaust Sparks)
        viewModel.activeParticles.value.forEach { particle ->
            val px = particle.x * w
            val py = particle.y * h
            drawCircle(
                color = particle.color.copy(alpha = particle.alpha),
                radius = particle.size,
                center = Offset(px, py)
            )
        }

        // 5. Draw Player Car (at Y = 0.85f normalized)
        if (!viewModel.isGameOver) {
            val px = viewModel.playerX * w
            val py = 0.80f * h
            drawPlayerCar(px, py, viewModel.currentCar, viewModel.shieldActive, size)
        }
    }
}

// Helpers for Drawing Vehicles
fun DrawScope.drawPlayerCar(
    cx: Float,
    cy: Float,
    car: CarDefinition,
    hasShield: Boolean,
    canvasSize: Size
) {
    val w = car.width * canvasSize.width
    val h = car.height * canvasSize.height

    // Wheel tracks
    val wr = w * 0.15f
    val wh = h * 0.25f

    // Draw 4 glowing wheels
    val wheelColor = Color(0xFF15122B)
    drawRoundRect(wheelColor, Offset(cx - w/2 - wr/2, cy + h*0.1f), Size(wr, wh), CornerRadius(2.dp.toPx()))
    drawRoundRect(wheelColor, Offset(cx + w/2 - wr/2, cy + h*0.1f), Size(wr, wh), CornerRadius(2.dp.toPx()))
    drawRoundRect(wheelColor, Offset(cx - w/2 - wr/2, cy + h*0.65f), Size(wr, wh), CornerRadius(2.dp.toPx()))
    drawRoundRect(wheelColor, Offset(cx + w/2 - wr/2, cy + h*0.65f), Size(wr, wh), CornerRadius(2.dp.toPx()))

    // Main Chassis Body
    val path = Path().apply {
        moveTo(cx, cy) // nose
        lineTo(cx + w/2, cy + h*0.3f)
        lineTo(cx + w/2, cy + h*0.85f)
        lineTo(cx + w/3, cy + h)
        lineTo(cx - w/3, cy + h)
        lineTo(cx - w/2, cy + h*0.85f)
        lineTo(cx - w/2, cy + h*0.3f)
        close()
    }

    drawPath(
        path = path,
        color = car.primaryColor
    )

    // Inner Cabin glass
    val cabinPath = Path().apply {
        moveTo(cx, cy + h*0.35f)
        lineTo(cx + w/4, cy + h*0.5f)
        lineTo(cx + w/5, cy + h*0.75f)
        lineTo(cx - w/5, cy + h*0.75f)
        lineTo(cx - w/4, cy + h*0.5f)
        close()
    }
    drawPath(cabinPath, Color.White.copy(alpha = 0.6f))

    // Headlight glowing beams
    drawArc(
        color = NeonYellow.copy(alpha = 0.3f),
        startAngle = 240f,
        sweepAngle = 60f,
        useCenter = true,
        topLeft = Offset(cx - w, cy - h*0.3f),
        size = Size(w * 2, h * 0.5f)
    )

    // Draw neon underglow outline
    drawPath(
        path = path,
        color = car.glowColor,
        style = Stroke(width = 3.dp.toPx())
    )

    // Shield protective bubble
    if (hasShield) {
        drawCircle(
            color = NeonCyan.copy(alpha = 0.2f),
            radius = h * 0.75f,
            center = Offset(cx, cy + h/2)
        )
        drawCircle(
            color = NeonCyan,
            radius = h * 0.72f,
            center = Offset(cx, cy + h/2),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

fun DrawScope.drawObstacleCar(
    cx: Float,
    cy: Float,
    bodyColor: Color,
    neonGlow: Color,
    canvasSize: Size
) {
    val ow = 0.11f * canvasSize.width
    val oh = 0.15f * canvasSize.height

    // Draw wheels
    val wheelColor = Color(0xFF15122B)
    drawRect(wheelColor, Offset(cx - ow/2 - 4.dp.toPx(), cy - oh/3), Size(6.dp.toPx(), 12.dp.toPx()))
    drawRect(wheelColor, Offset(cx + ow/2 - 2.dp.toPx(), cy - oh/3), Size(6.dp.toPx(), 12.dp.toPx()))
    drawRect(wheelColor, Offset(cx - ow/2 - 4.dp.toPx(), cy + oh/4), Size(6.dp.toPx(), 12.dp.toPx()))
    drawRect(wheelColor, Offset(cx + ow/2 - 2.dp.toPx(), cy + oh/4), Size(6.dp.toPx(), 12.dp.toPx()))

    // Main body
    drawRoundRect(
        color = bodyColor,
        topLeft = Offset(cx - ow/2, cy - oh/2),
        size = Size(ow, oh),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
    )

    // Neon trim lines
    drawRoundRect(
        color = neonGlow,
        topLeft = Offset(cx - ow/2, cy - oh/2),
        size = Size(ow, oh),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
        style = Stroke(width = 2.dp.toPx())
    )

    // Red brake lights
    drawRect(Color.Red, Offset(cx - ow/3, cy - oh/2 + 2.dp.toPx()), Size(4.dp.toPx(), 2.dp.toPx()))
    drawRect(Color.Red, Offset(cx + ow/3 - 4.dp.toPx(), cy - oh/2 + 2.dp.toPx()), Size(4.dp.toPx(), 2.dp.toPx()))
}

fun DrawScope.drawObstacleTruck(
    cx: Float,
    cy: Float,
    canvasSize: Size
) {
    val tw = 0.14f * canvasSize.width
    val th = 0.21f * canvasSize.height

    // Cargo body (metallic yellow/orange)
    drawRoundRect(
        color = Color(0xFFD84315), // Rust Orange
        topLeft = Offset(cx - tw/2, cy - th/2),
        size = Size(tw, th * 0.7f),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )

    // Driver's cabin at the bottom of the oncoming truck
    drawRoundRect(
        color = Color(0xFFFFB300), // Yellow
        topLeft = Offset(cx - tw/2 + 2.dp.toPx(), cy + th*0.2f),
        size = Size(tw - 4.dp.toPx(), th * 0.3f),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )

    // Windshield
    drawRect(
        color = Color.Black.copy(alpha = 0.8f),
        topLeft = Offset(cx - tw/3, cy + th*0.35f),
        size = Size(tw * 0.66f, th * 0.1f)
    )

    // Glowing hazard stripes
    drawRoundRect(
        color = NeonYellow,
        topLeft = Offset(cx - tw/2, cy - th/2),
        size = Size(tw, th * 0.7f),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
        style = Stroke(width = 1.5.dp.toPx())
    )
}

fun DrawScope.drawHomeCarPreview(car: CarDefinition) {
    val cx = size.width / 2
    val cy = size.height / 2 - 15.dp.toPx()
    val w = 60.dp.toPx()
    val h = 80.dp.toPx()

    // Preview drawing coordinates
    val path = Path().apply {
        moveTo(cx, cy)
        lineTo(cx + w/2, cy + h*0.3f)
        lineTo(cx + w/2, cy + h*0.85f)
        lineTo(cx + w/3, cy + h)
        lineTo(cx - w/3, cy + h)
        lineTo(cx - w/2, cy + h*0.85f)
        lineTo(cx - w/2, cy + h*0.3f)
        close()
    }
    drawPath(path, car.primaryColor)
    drawPath(path, car.glowColor, style = Stroke(width = 3.dp.toPx()))

    // Inner cabin outline
    drawCircle(color = Color.White.copy(alpha = 0.5f), radius = 10.dp.toPx(), center = Offset(cx, cy + h*0.55f))
}

// 3. GAME PLAY HUD OVERLAY
@Composable
fun GameHudOverlay(viewModel: GameViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // TOP Row: Distance & Score
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(DarkGreyGlass, RoundedCornerShape(10.dp))
                .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "SCORE: ${viewModel.currentScore}",
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                modifier = Modifier.testTag("score_hud")
            )
            Text(
                text = "DIST: ${viewModel.currentDistance.toInt()}m",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // TOP Row Right: Coins & Active status
        Column(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(DarkGreyGlass, RoundedCornerShape(10.dp))
                    .border(1.dp, NeonYellow.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = NeonYellow,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${viewModel.currentCoinsCollected}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    modifier = Modifier.testTag("coins_hud")
                )
            }

            // Powerup indicator badges
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (viewModel.shieldActive) {
                    PowerupBadge(text = "SHIELD", color = NeonCyan)
                }
                if (viewModel.magnetActive) {
                    PowerupBadge(text = "MAGNET", color = NeonMagenta)
                }
            }
        }

        // BOTTOM HUD Row: FUEL Meter and boost alert
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(DarkGreyGlass, RoundedCornerShape(10.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "FUEL LEVEL",
                        color = if (viewModel.fuelLevel < 0.25f) NeonMagenta else NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${(viewModel.fuelLevel * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { viewModel.fuelLevel },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .testTag("fuel_bar"),
                    color = if (viewModel.fuelLevel < 0.25f) NeonMagenta else NeonGreen,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
            }
        }
    }
}

@Composable
fun PowerupBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .border(1.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// 4. ERGONOMIC CONTROLLER LAYOUT
@Composable
fun InteractiveControls(viewModel: GameViewModel) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        color = DarkSpaceBackground,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Directional steer wheel (Left / Right buttons)
            Row(
                modifier = Modifier.weight(1.2f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SteerButton(
                    icon = Icons.Default.KeyboardArrowLeft,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("steer_left_btn")
                ) {
                    viewModel.moveLeft()
                }

                SteerButton(
                    icon = Icons.Default.KeyboardArrowRight,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("steer_right_btn")
                ) {
                    viewModel.moveRight()
                }
            }

            Spacer(modifier = Modifier.width(32.dp))

            // BOOST Nitro Button (Hold down to execute nitro, release to decelerate)
            val boostInteractionSource = remember { MutableInteractionSource() }
            val isPressed by boostInteractionSource.collectIsPressedAsState()

            LaunchedEffect(isPressed) {
                viewModel.toggleBoost(isPressed)
            }

            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
                    .background(
                        if (isPressed) NeonMagenta else NeonMagenta.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    )
                    .border(2.dp, NeonMagenta, RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                val press = try {
                                    viewModel.toggleBoost(true)
                                    tryAwaitRelease()
                                } finally {
                                    viewModel.toggleBoost(false)
                                }
                            }
                        )
                    }
                    .testTag("nitro_boost_btn"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isPressed) Color.White else NeonMagenta,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "NITRO BOOST",
                        color = if (isPressed) Color.White else NeonMagenta,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun SteerButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 12.dp)
            .background(NeonCyan.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .border(2.dp, NeonCyan.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .clickable {
                RetroSoundPlayer.playClickSound()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(44.dp)
        )
    }
}

// 5. GAME OVER DIALOG
@Composable
fun GameOverOverlay(viewModel: GameViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .background(DarkGreyGlass, RoundedCornerShape(18.dp))
                .border(2.dp, NeonMagenta, RoundedCornerShape(18.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "GAME OVER",
                color = NeonMagenta,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.sp,
                modifier = Modifier.testTag("game_over_title")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Score details
            StatRow(label = "FINAL SCORE", value = "${viewModel.currentScore}", color = NeonCyan)
            StatRow(label = "DISTANCE ROAD", value = "${viewModel.currentDistance.toInt()}m", color = Color.White)
            StatRow(label = "COINS EARNED", value = "+${viewModel.currentCoinsCollected}", color = NeonYellow)

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Button(
                onClick = { viewModel.startGame() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("restart_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = DarkSpaceBackground)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RETRY RACE",
                        color = DarkSpaceBackground,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { viewModel.setScreen("home") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("back_to_menu_btn"),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MAIN MENU",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// 6. GARAGE & UPGRADES SCREEN
@Composable
fun GarageScreen(viewModel: GameViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Upper stats & navigation bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.setScreen("home") },
                modifier = Modifier
                    .background(DarkGreyGlass, CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .testTag("garage_back_btn")
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Text(
                text = "NEON GARAGE",
                color = NeonCyan,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            // Current Coin balance
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(DarkGreyGlass, RoundedCornerShape(10.dp))
                    .border(1.dp, NeonYellow.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${viewModel.playerCoins}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Core Upgrades levels
        Text(
            text = "PERFORMANCE TUNING",
            color = NeonMagenta,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkGreyGlass, RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            UpgradeRow(
                name = "ENGINE TOP SPEED",
                level = viewModel.speedUpgradeLevel,
                cost = viewModel.getUpgradeCost("speed"),
                color = NeonCyan,
                onUpgrade = { viewModel.upgradeStat("speed") }
            )
            UpgradeRow(
                name = "SOLAR FUEL CELLS",
                level = viewModel.fuelUpgradeLevel,
                cost = viewModel.getUpgradeCost("fuel"),
                color = NeonGreen,
                onUpgrade = { viewModel.upgradeStat("fuel") }
            )
            UpgradeRow(
                name = "MAGNETIC DEFLECTOR",
                level = viewModel.magnetUpgradeLevel,
                cost = viewModel.getUpgradeCost("magnet"),
                color = NeonMagenta,
                onUpgrade = { viewModel.upgradeStat("magnet") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Available vehicle roster
        Text(
            text = "CHOOSE VEHICLE",
            color = NeonCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(AVAILABLE_CARS) { index, carDef ->
                val isUnlocked = viewModel.unlockedCarsList.contains(carDef.id)
                val isSelected = viewModel.selectedCarId == carDef.id

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) carDef.primaryColor.copy(alpha = 0.08f) else DarkGreyGlass,
                            RoundedCornerShape(14.dp)
                        )
                        .border(
                            1.5.dp,
                            if (isSelected) carDef.primaryColor else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            if (isUnlocked) {
                                viewModel.selectCar(carDef.id)
                            }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small color silhouette
                    Box(
                        modifier = Modifier
                            .size(50.dp, 60.dp)
                            .background(carDef.glowColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .border(1.dp, carDef.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawHomeCarPreview(carDef)
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Details
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = carDef.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = carDef.description,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Buy or Select status Action
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .background(carDef.primaryColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = DarkSpaceBackground,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else if (isUnlocked) {
                        OutlinedButton(
                            onClick = { viewModel.selectCar(carDef.id) },
                            border = BorderStroke(1.dp, Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("SELECT", fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.purchaseCar(carDef) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonYellow),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = DarkSpaceBackground, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${carDef.cost}",
                                    color = DarkSpaceBackground,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpgradeRow(
    name: String,
    level: Int,
    cost: Int,
    color: Color,
    onUpgrade: () -> Unit
) {
    val isMaxed = level >= 5

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            if (isMaxed) {
                Text(
                    text = "MAX LEVEL",
                    color = NeonYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Row(
                    modifier = Modifier
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, color, RoundedCornerShape(6.dp))
                        .clickable { onUpgrade() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$cost",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Level blocks visualization (1 to 5 dots)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 1..5) {
                val isActive = i <= level
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(
                            if (isActive) color else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

// 7. LEADERBOARDS SCREEN
@Composable
fun LeaderboardsScreen(viewModel: GameViewModel, topScores: List<com.example.data.HighScoreEntity>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Navigation bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.setScreen("home") },
                modifier = Modifier
                    .background(DarkGreyGlass, CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .testTag("records_back_btn")
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "SPEED RECORD HOLDERS",
                color = NeonCyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (topScores.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "NO RECORDS YET",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Complete a race to record stats",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(topScores) { index, scoreEntity ->
                    val rank = index + 1
                    val rankColor = when (rank) {
                        1 -> NeonYellow
                        2 -> Color(0xFFC0C0C0) // Silver
                        3 -> Color(0xFFCD7F32) // Bronze
                        else -> NeonCyan.copy(alpha = 0.5f)
                    }

                    val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(scoreEntity.timestamp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkGreyGlass, RoundedCornerShape(12.dp))
                            .border(1.dp, if (rank == 1) NeonYellow.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#$rank",
                                color = rankColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(36.dp)
                            )

                            Column {
                                Text(
                                    text = scoreEntity.playerName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = dateFormatted,
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${scoreEntity.score} PTS",
                                color = NeonCyan,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "+${scoreEntity.coins}",
                                    color = NeonYellow,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
