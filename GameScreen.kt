package com.example.appconingresoycontrasea.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Item Types
enum class ItemType {
    STAR,  // +10 points
    GEM,   // +25 points
    BOMB,  // -1 Life & -15 points
    HEART  // +1 Life
}

data class FallingItem(
    val id: Long,
    var x: Float,
    var y: Float,
    val speed: Float,
    val size: Float,
    val type: ItemType
)

data class ScorePopup(
    val id: Long,
    val text: String,
    val x: Float,
    var y: Float,
    val color: Color,
    var alpha: Float = 1.0f
)

@Composable
fun GameScreen(
    username: String,
    onLogout: () -> Unit
) {
    // Game States
    var score by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var isGameOver by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    // Stats
    var starsCaught by remember { mutableIntStateOf(0) }
    var gemsCaught by remember { mutableIntStateOf(0) }

    // Dimensions
    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }

    // Player position
    var playerX by remember { mutableFloatStateOf(0f) }
    val paddleWidth = 180f
    val paddleHeight = 36f

    // Falling items list & floating popups
    val items = remember { mutableStateListOf<FallingItem>() }
    val popups = remember { mutableStateListOf<ScorePopup>() }

    var lastSpawnTime by remember { mutableLongStateOf(0L) }
    var nextItemId by remember { mutableLongStateOf(0L) }

    val textMeasurer = rememberTextMeasurer()

    fun resetGame() {
        score = 0
        lives = 3
        isGameOver = false
        isPaused = false
        starsCaught = 0
        gemsCaught = 0
        items.clear()
        popups.clear()
        if (canvasWidth > 0f) {
            playerX = canvasWidth / 2f
        }
    }

    // Main Game Loop
    LaunchedEffect(isGameOver, isPaused) {
        if (isGameOver || isPaused) return@LaunchedEffect

        var lastFrameTime = System.nanoTime()

        while (!isGameOver && !isPaused) {
            withFrameNanos { frameTime ->
                val rawDt = (frameTime - lastFrameTime) / 1e9f
                lastFrameTime = frameTime

                // Cap delta time to prevent large physics jumps on frame drops
                val dt = rawDt.coerceIn(0.0001f, 0.05f)

                if (canvasWidth <= 0f || canvasHeight <= 0f) return@withFrameNanos

                // Difficulty factor increases with score
                val speedMultiplier = 1.0f + (score / 150f).coerceAtMost(2.5f)
                val spawnIntervalMs = (800 / speedMultiplier).toLong().coerceAtLeast(350L)

                // Spawn items
                val currentTimeMs = System.currentTimeMillis()
                if (currentTimeMs - lastSpawnTime > spawnIntervalMs) {
                    lastSpawnTime = currentTimeMs
                    val itemType = when (Random.nextFloat()) {
                        in 0.0f..0.55f -> ItemType.STAR
                        in 0.55f..0.75f -> ItemType.GEM
                        in 0.75f..0.93f -> ItemType.BOMB
                        else -> ItemType.HEART
                    }

                    val margin = 50f
                    val spawnX = Random.nextFloat() * (canvasWidth - 2 * margin) + margin
                    val baseSpeed = when (itemType) {
                        ItemType.STAR -> 320f
                        ItemType.GEM -> 420f
                        ItemType.BOMB -> 360f
                        ItemType.HEART -> 380f
                    }

                    items.add(
                        FallingItem(
                            id = nextItemId++,
                            x = spawnX,
                            y = -40f,
                            speed = baseSpeed * speedMultiplier,
                            size = if (itemType == ItemType.GEM) 36f else 40f,
                            type = itemType
                        )
                    )
                }

                // Update Popups
                val popupIterator = popups.iterator()
                while (popupIterator.hasNext()) {
                    val p = popupIterator.next()
                    p.y -= 70f * dt
                    p.alpha -= 1.2f * dt
                    if (p.alpha <= 0f) {
                        popupIterator.remove()
                    }
                }

                // Update Item positions & check collisions
                val playerY = canvasHeight - 110f
                val paddleLeft = playerX - paddleWidth / 2f
                val paddleRight = playerX + paddleWidth / 2f

                val itemIterator = items.iterator()
                while (itemIterator.hasNext()) {
                    val item = itemIterator.next()
                    item.y += item.speed * dt

                    // Collision check with Paddle
                    if (item.y >= playerY - 20f && item.y <= playerY + paddleHeight) {
                        if (item.x in paddleLeft..paddleRight) {
                            // Collision occurred!
                            when (item.type) {
                                ItemType.STAR -> {
                                    score += 10
                                    starsCaught++
                                    popups.add(ScorePopup(nextItemId++, "+10", item.x, item.y, Color(0xFFFFD700)))
                                }
                                ItemType.GEM -> {
                                    score += 25
                                    gemsCaught++
                                    popups.add(ScorePopup(nextItemId++, "+25", item.x, item.y, Color(0xFF00EEEE)))
                                }
                                ItemType.BOMB -> {
                                    score = (score - 15).coerceAtLeast(0)
                                    lives -= 1
                                    popups.add(ScorePopup(nextItemId++, "-1 ❤️", item.x, item.y, Color(0xFFFF4444)))
                                    if (lives <= 0) {
                                        isGameOver = true
                                        if (score > highScore) {
                                            highScore = score
                                        }
                                    }
                                }
                                ItemType.HEART -> {
                                    if (lives < 5) lives++
                                    popups.add(ScorePopup(nextItemId++, "+1 ❤️", item.x, item.y, Color(0xFFFF69B4)))
                                }
                            }
                            itemIterator.remove()
                            continue
                        }
                    }

                    // Remove if passed screen bottom
                    if (item.y > canvasHeight + 50f) {
                        itemIterator.remove()
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Game Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            playerX = offset.x.coerceIn(paddleWidth / 2f, canvasWidth - paddleWidth / 2f)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            playerX = (playerX + dragAmount.x).coerceIn(paddleWidth / 2f, canvasWidth - paddleWidth / 2f)
                        }
                    )
                }
        ) {
            canvasWidth = size.width
            canvasHeight = size.height

            if (playerX == 0f) {
                playerX = canvasWidth / 2f
            }

            // Draw Background Grid
            drawGridBackground(size)

            // Draw Player Paddle
            val playerY = canvasHeight - 110f
            drawPaddle(playerX, playerY, paddleWidth, paddleHeight)

            // Draw Falling Items
            items.forEach { item ->
                when (item.type) {
                    ItemType.STAR -> drawStar(item.x, item.y, item.size)
                    ItemType.GEM -> drawGem(item.x, item.y, item.size)
                    ItemType.BOMB -> drawBomb(item.x, item.y, item.size)
                    ItemType.HEART -> drawHeart(item.x, item.y, item.size)
                }
            }

            // Draw Floating Score Popups
            popups.forEach { popup ->
                val textLayoutResult = textMeasurer.measure(
                    text = popup.text,
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = popup.color.copy(alpha = popup.alpha.coerceIn(0f, 1f))
                    )
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        popup.x - textLayoutResult.size.width / 2f,
                        popup.y - textLayoutResult.size.height / 2f
                    )
                )
            }
        }

        // Top HUD Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B).copy(alpha = 0.88f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Player info & Score
                    Column {
                        Text(
                            text = "Piloto: $username",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Score: ",
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = "$score",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Récord: $highScore",
                                fontSize = 12.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    // Lives & Pause
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(lives) {
                            Text(text = "❤️", fontSize = 16.sp)
                        }
                        repeat((5 - lives).coerceAtLeast(0)) {
                            Text(text = "🖤", fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = { isPaused = !isPaused },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Close,
                                contentDescription = "Pausa",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Control Hint
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Color(0xFF1E293B).copy(alpha = 0.75f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "👈 Desliza o toca la pantalla para mover la nave 👉",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Pause Overlay
        AnimatedVisibility(
            visible = isPaused && !isGameOver,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PAUSA",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { isPaused = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Text("REANUDAR")
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onLogout() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                        ) {
                            Text("SALIR AL LOGIN")
                        }
                    }
                }
            }
        }

        // Game Over Overlay
        AnimatedVisibility(
            visible = isGameOver,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "💥", fontSize = 32.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "¡JUEGO TERMINADO!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "PUNTUACIÓN FINAL",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "$score",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Text(
                                        text = "⭐ $starsCaught",
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "💎 $gemsCaught",
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "🏆 Récord: $highScore",
                                        fontSize = 14.sp,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { resetGame() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("JUGAR DE NUEVO", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onLogout() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                        ) {
                            Text("CERRAR SESIÓN", color = Color(0xFFCBD5E1))
                        }
                    }
                }
            }
        }
    }
}

// Drawing Helper Functions
private fun DrawScope.drawGridBackground(size: Size) {
    val step = 80f
    var x = 0f
    while (x < size.width) {
        drawLine(
            color = Color(0xFF1E293B).copy(alpha = 0.3f),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawLine(
            color = Color(0xFF1E293B).copy(alpha = 0.3f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += step
    }
}

private fun DrawScope.drawPaddle(x: Float, y: Float, width: Float, height: Float) {
    val left = x - width / 2f

    // Outer glow
    drawRoundRect(
        color = Color(0xFF818CF8).copy(alpha = 0.3f),
        topLeft = Offset(left - 6f, y - 6f),
        size = Size(width + 12f, height + 12f),
        cornerRadius = CornerRadius(16f, 16f)
    )

    // Paddle body
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFF6366F1), Color(0xFF818CF8), Color(0xFF38BDF8))
        ),
        topLeft = Offset(left, y),
        size = Size(width, height),
        cornerRadius = CornerRadius(12f, 12f)
    )

    // Inner detail line
    drawLine(
        color = Color.White.copy(alpha = 0.8f),
        start = Offset(left + 20f, y + height / 2f),
        end = Offset(left + width - 20f, y + height / 2f),
        strokeWidth = 3f
    )
}

private fun DrawScope.drawStar(x: Float, y: Float, radius: Float) {
    val path = Path()
    val numPoints = 5
    val innerRadius = radius * 0.45f

    for (i in 0 until numPoints * 2) {
        val r = if (i % 2 == 0) radius else innerRadius
        val angle = i * PI / numPoints - PI / 2
        val px = x + (r * cos(angle)).toFloat()
        val py = y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()

    // Outer Glow
    drawCircle(
        color = Color(0xFFFFD700).copy(alpha = 0.25f),
        radius = radius * 1.4f,
        center = Offset(x, y)
    )

    drawPath(path = path, color = Color(0xFFFFD700), style = Fill)
}

private fun DrawScope.drawGem(x: Float, y: Float, radius: Float) {
    val path = Path()
    path.moveTo(x, y - radius)
    path.lineTo(x + radius, y)
    path.lineTo(x, y + radius)
    path.lineTo(x - radius, y)
    path.close()

    // Outer Glow
    drawCircle(
        color = Color(0xFF00EEEE).copy(alpha = 0.3f),
        radius = radius * 1.3f,
        center = Offset(x, y)
    )

    drawPath(
        path = path,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF00FFFF), Color(0xFF0099FF))
        ),
        style = Fill
    )
}

private fun DrawScope.drawBomb(x: Float, y: Float, radius: Float) {
    // Bomb body
    drawCircle(
        color = Color(0xFF334155),
        radius = radius,
        center = Offset(x, y)
    )
    drawCircle(
        color = Color(0xFFEF4444),
        radius = radius * 0.7f,
        center = Offset(x, y)
    )

    // Fuse spark
    drawLine(
        color = Color(0xFFF59E0B),
        start = Offset(x, y - radius),
        end = Offset(x + 10f, y - radius - 12f),
        strokeWidth = 4f
    )
    drawCircle(
        color = Color(0xFFFACC15),
        radius = 5f,
        center = Offset(x + 10f, y - radius - 12f)
    )
}

private fun DrawScope.drawHeart(x: Float, y: Float, radius: Float) {
    val path = Path()
    val width = radius * 2
    val height = radius * 2

    // Outer Glow
    drawCircle(
        color = Color(0xFFEC4899).copy(alpha = 0.3f),
        radius = radius * 1.3f,
        center = Offset(x, y)
    )

    path.moveTo(x, y + height * 0.35f)
    path.cubicTo(
        x - width * 0.5f, y,
        x - width * 0.5f, y - height * 0.5f,
        x, y - height * 0.2f
    )
    path.cubicTo(
        x + width * 0.5f, y - height * 0.5f,
        x + width * 0.5f, y,
        x, y + height * 0.35f
    )
    path.close()

    drawPath(path = path, color = Color(0xFFEC4899), style = Fill)
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, showSystemUi = true)
@Composable
fun GameScreenPreview() {
    GameScreen(username = "admin", onLogout = {})
}
