package com.example.colorwordmatching

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.random.Random

enum class GameState { START, COUNTDOWN, RUNNING, GAME_OVER }
enum class Mode { COLOR, TEXT }
enum class Choice { INK, WORD }
enum class ColorName(val label: String, val color: Color) {
    RED("RED", Color(0xFFF44336)),
    BLUE("BLUE", Color(0xFF2196F3)),
    GREEN("GREEN", Color(0xFF4CAF50)),
    ORANGE("ORANGE", Color(0xFFFF9800)),
    PURPLE("PURPLE", Color(0xFF9C27B0))
}

data class Question(
    val id: Int,
    val mode: Mode,
    val word: ColorName,      // teks yang ditulis
    val ink: ColorName,       // warna tinta
    val inkOnLeft: Boolean    // flag acak posisi tombol
)

const val TIME_LIMIT_MS = 5000L
const val MAX_MISTAKES = 3

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { ColorWordApp() }
        }
    }
}

@Composable
fun ColorWordApp() {
    var state by remember { mutableStateOf(GameState.START) }
    var countdownText by remember { mutableStateOf("3") }
    var score by remember { mutableIntStateOf(0) }
    var mistakes by remember { mutableIntStateOf(0) }
    var bestScore by remember { mutableIntStateOf(0) }
    var question by remember { mutableStateOf<Question?>(null) }
    var remainingMs by remember { mutableLongStateOf(TIME_LIMIT_MS) }
    var qCounter by remember { mutableIntStateOf(0) }

    // Reset state per-soal
    fun newQuestion() {
        qCounter++
        val word = ColorName.values().random()
        val ink = ColorName.values().filter { it != word }.random()
        question = Question(qCounter, Mode.values().random(), word, ink, Random.nextBoolean())
        remainingMs = TIME_LIMIT_MS
    }

    fun submit(correct: Boolean) {
        if (correct) score++ else mistakes++
        if (mistakes >= MAX_MISTAKES) {
            if (score > bestScore) bestScore = score
            state = GameState.GAME_OVER
        } else {
            newQuestion()
        }
    }

    fun startGame() {
        score = 0
        mistakes = 0
        question = null
        countdownText = "3"
        state = GameState.COUNTDOWN
    }

    // Countdown 3 - 2 - 1 - Start! (pakai delay)
    LaunchedEffect(state) {
        if (state == GameState.COUNTDOWN) {
            for (t in listOf("3", "2", "1")) {
                countdownText = t
                delay(1000)
            }
            countdownText = "Start!"
            delay(700)
            newQuestion()
            state = GameState.RUNNING
        }
    }

    // Timer per soal; otomatis berhenti & reset saat soal berganti
    LaunchedEffect(question?.id, state) {
        val q = question
        if (state == GameState.RUNNING && q != null) {
            val begin = SystemClock.elapsedRealtime()
            remainingMs = TIME_LIMIT_MS
            while (remainingMs > 0) {
                delay(50)
                remainingMs = (TIME_LIMIT_MS - (SystemClock.elapsedRealtime() - begin)).coerceAtLeast(0)
            }
            submit(false)   // timeout = salah
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .systemBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            GameState.START -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Welcome\nto\nColor Word Matching", fontSize = 26.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(32.dp))
                    Button(onClick = { startGame() }) { Text("Start Game") }
                }
            }

            GameState.COUNTDOWN -> {
                Text(countdownText, fontSize = 40.sp)
            }

            GameState.RUNNING -> {
                val q = question
                if (q != null) {
                    val secondsLeft = ceil(remainingMs / 1000.0).toInt()
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Mode: ${q.mode.name}", fontSize = 16.sp)
                            Text("✅ $score   ❌ $mistakes/$MAX_MISTAKES", fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(12.dp))

                        // Banner clue
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (q.mode == Mode.COLOR) "Pilih WARNA TINTA" else "Pilih NAMA KATA",
                                fontSize = 16.sp
                            )
                        }

                        Spacer(Modifier.height(40.dp))
                        Text("$secondsLeft s", fontSize = 22.sp, color = Color.DarkGray)
                        Spacer(Modifier.height(8.dp))
                        Text(q.word.label, fontSize = 52.sp, color = q.ink.color)
                        Spacer(Modifier.height(60.dp))

                        val inkBtn: @Composable () -> Unit = {
                            Button(onClick = {
                                submit(q.mode == Mode.COLOR)   // INK benar hanya di mode COLOR
                            }) { Text(q.ink.label) }
                        }
                        val wordBtn: @Composable () -> Unit = {
                            Button(onClick = {
                                submit(q.mode == Mode.TEXT)    // WORD benar hanya di mode TEXT
                            }) { Text(q.word.label) }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            if (q.inkOnLeft) { inkBtn(); wordBtn() } else { wordBtn(); inkBtn() }
                        }
                    }
                }
            }

            GameState.GAME_OVER -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Game Over!", fontSize = 30.sp)
                    Spacer(Modifier.height(24.dp))
                    Text("Your Score", fontSize = 18.sp)
                    Text("$score", fontSize = 28.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Best Score", fontSize = 14.sp)
                    Text("$bestScore", fontSize = 20.sp)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { startGame() }) { Text("Restart Game") }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { state = GameState.START }) { Text("Exit") }
                }
            }
        }
    }
}