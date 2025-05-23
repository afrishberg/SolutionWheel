package org.ayala.wheel

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.random.Random

interface SpeechRecognizer {
    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    )
    fun stopListening()
    fun isAvailable(): Boolean
    
    @Composable
    fun Initialize()

    companion object {
        fun create(): SpeechRecognizer = createSpeechRecognizer()
    }
}

expect fun createSpeechRecognizer(): SpeechRecognizer

@Composable
fun rememberSpeechRecognizer(): SpeechRecognizer {
    val speechRecognizer = remember { SpeechRecognizer.create() }
    speechRecognizer.Initialize()
    return speechRecognizer
}

data class SolutionOption(
    val emoji: String,
    val description: String,
    val color: Color
)

val solutionOptions = listOf(
    SolutionOption("👃🏼", "לינשום עמוק", Color(0xFFFFC107)),
    SolutionOption("🗣️", "לדבר עם מישהו", Color(0xFF4CAF50)),
    SolutionOption("🎨", "לצייר את הרגשות", Color(0xFF2196F3)),
    SolutionOption("🎵", "לשמוע מוזיקה", Color(0xFF9C27B0)),
    SolutionOption("🧸", "לחבק בובה", Color(0xFFE91E63)),
    SolutionOption("👐🏼", "לספור עד 10", Color(0xFF795548)),
    SolutionOption("🚶", "לעשות הליכה", Color(0xFF607D8B)),
    SolutionOption("📚", "לקרוא ספר", Color(0xFFFF5722))
)

@Composable
fun App() {
    val textMeasurer = rememberTextMeasurer()
    var textInput by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    val speechRecognizer = rememberSpeechRecognizer()
    
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "גלגל הפתרונות",
                style = TextStyle(
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Text field
                TextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier
                        .weight(1f),
                    placeholder = {
                        Text(
                            "תוכל לספר מה קרה? למה אתה מרגיש צורך בפתרון?",
                            style = TextStyle(fontSize = 16.sp)
                        )
                    },
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions.Default,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        textAlign = TextAlign.Right
                    )
                )

                // Microphone button
                if (speechRecognizer.isAvailable()) {
                    IconButton(
                        onClick = {
                            if (!isListening) {
                                isListening = true
                                speechRecognizer.startListening(
                                    onResult = { result ->
                                        textInput = result
                                    },
                                    onError = { error ->
                                        isListening = false
                                    }
                                )
                            } else {
                                isListening = false
                                speechRecognizer.stopListening()
                            }
                        },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "הקלטה",
                            tint = if (isListening) Color.Red else Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Pointer arrow
            Canvas(modifier = Modifier.size(30.dp, 40.dp)) {
                // Draw arrow head
                val trianglePath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width / 2f, size.height)
                    lineTo(0f, 0f)
                    lineTo(size.width, 0f)
                    close()
                }
                drawPath(path = trianglePath, color = Color.Black)
                
                // Draw arrow tail
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(size.width / 2f - 2f, 0f),
                    size = Size(4f, size.height * 0.4f)
                )
            }

            var rotationState by remember { mutableStateOf(0f) }
            var isSpinning by remember { mutableStateOf(false) }
            var selectedOption by remember { mutableStateOf<SolutionOption?>(null) }
            
            val rotation = remember { Animatable(rotationState) }

            LaunchedEffect(isSpinning) {
                if (isSpinning) {
                    val sectionSize = 360f / solutionOptions.size
                    // Calculate a random section to land on
                    val targetSection = Random.nextInt(solutionOptions.size)
                    // Since the wheel rotates clockwise but sections are numbered counterclockwise,
                    // we need to reverse the section index for rotation
                    val reversedSection = (solutionOptions.size - targetSection - 1) % solutionOptions.size
                    
                    // Normalize current rotation to be within one full rotation
                    val normalizedCurrentRotation = rotationState % 360f
                    // Calculate how much we need to rotate to reach the target section's center
                    val rotationToTarget = (reversedSection * sectionSize + sectionSize / 2) - normalizedCurrentRotation
                    // Add full rotations to make it spin more
                    val fullRotations = Random.nextInt(3, 6) * 360f
                    
                    // Calculate final target rotation
                    val targetRotation = rotationState + fullRotations + rotationToTarget
                    
                    rotation.animateTo(
                        targetValue = targetRotation,
                        animationSpec = tween(
                            durationMillis = 3000,
                            easing = FastOutSlowInEasing
                        )
                    )
                    
                    rotationState = targetRotation
                    isSpinning = false
                    // Use the original targetSection for selection to match visual position
                    selectedOption = solutionOptions[targetSection]
                }
            }

            // Wheel
            Canvas(
                modifier = Modifier
                    .size(300.dp)
                    .padding(16.dp)
            ) {
                val sectionAngle = 360f / solutionOptions.size
                rotate(rotation.value) {
                    // First draw all the colored sections
                    solutionOptions.forEachIndexed { index, option ->
                        rotate(index * sectionAngle) {
                            drawArc(
                                color = option.color,
                                startAngle = 0f,
                                sweepAngle = sectionAngle,
                                useCenter = true,
                                size = Size(size.width, size.height)
                            )
                        }
                    }
                    
                    // Then draw all the emojis
                    solutionOptions.forEachIndexed { index, option ->
                        rotate(index * sectionAngle + sectionAngle / 2) {
                            val textStyle = TextStyle(fontSize = 24.sp)
                            val textLayoutResult = textMeasurer.measure(option.emoji, textStyle)
                            drawText(
                                textLayoutResult,
                                topLeft = Offset(
                                    x = size.width / 2f - textLayoutResult.size.width / 2f,
                                    y = size.height / 6f - textLayoutResult.size.height / 2f
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { isSpinning = true },
                enabled = !isSpinning,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(if (isSpinning) "מסתובב..." else "סובב את הגלגל!")
            }

            Spacer(modifier = Modifier.height(16.dp))

            selectedOption?.let { option ->
                Text(
                    "${option.emoji} ${option.description}",
                    style = TextStyle(fontSize = 20.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}