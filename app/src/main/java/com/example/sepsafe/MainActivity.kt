package com.example.sepsafe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import org.json.JSONObject
import androidx.compose.ui.layout.ContentScale


class MainActivity : ComponentActivity() {

    private lateinit var model: SepsisModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = SepsisModel(this)
        setContent { SepsisScreen(model) }
    }
}

// Función para leer mean y scale desde JSON
fun loadScalerParams(context: android.content.Context): Pair<FloatArray, FloatArray> {
    val jsonStr = context.assets.open("scaler_params.json").bufferedReader().use { it.readText() }
    val json = JSONObject(jsonStr)
    val meanJson = json.getJSONArray("mean")
    val scaleJson = json.getJSONArray("scale")
    val n = meanJson.length()
    val mean = FloatArray(n) { i -> meanJson.getDouble(i).toFloat() }
    val scale = FloatArray(n) { i -> scaleJson.getDouble(i).toFloat() }
    return Pair(mean, scale)
}

@Composable
fun SepsisScreen(model: SepsisModel) {
    var rawInput by remember { mutableStateOf("") }
    var resultProb by remember { mutableStateOf(0f) }
    var resultMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val timesteps = 12
    val nFeatures = 8

    val history = remember { mutableStateListOf<Float>() }

    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {

            // Encabezado con icono
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.sepsafe),
                    contentDescription = "Ícono de la app",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentScale = ContentScale.FillWidth
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Sepsafe: Early prediction of sepsis",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Variables in orden: HR,O2Sat,MAP,Resp,WBC,Lactate,Temp,Platelets")
            Spacer(modifier = Modifier.height(4.dp))

            TextField(
                value = rawInput,
                onValueChange = { rawInput = it },
                label = { Text("Enter 8 values separated by commas") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val rawValues = rawInput.split(",").map { it.trim() }
                        if (rawValues.size != nFeatures) {
                            withContext(Dispatchers.Main) {
                                resultMessage = "ERROR: You must enter exactly 8 values"
                                resultProb = 0f
                            }
                            return@launch
                        }

                        val raw = rawValues.map { it.toFloat() }.toFloatArray()

                        // Validar rangos básicos
                        val validRanges = listOf(
                            30f..220f, 50f..100f, 40f..200f, 10f..50f,
                            1f..50f, 0f..20f, 30f..42f, 50f..500f
                        )
                        for (i in 0 until nFeatures) {
                            if (raw[i] !in validRanges[i]) {
                                withContext(Dispatchers.Main) {
                                    resultMessage = "ERROR: Value ${raw[i]} out of range for variable ${i + 1}"
                                    resultProb = 0f
                                }
                                return@launch
                            }
                        }

                        val (mean, scale) = loadScalerParams(context)
                        val normalized = FloatArray(nFeatures) { i -> (raw[i] - mean[i]) / scale[i] }

                        val window = Array(1) { Array(timesteps) { FloatArray(nFeatures) } }
                        for (t in 0 until timesteps) {
                            for (f in 0 until nFeatures) window[0][t][f] = normalized[f]
                        }

                        val probSepsis = model.predict(window)

                        withContext(Dispatchers.Main) {
                            resultProb = probSepsis
                            resultMessage = ""
                            history.add(probSepsis)
                            if (history.size > 12) history.removeAt(0)
                        }

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            resultMessage = "ERROR: ${e.message}"
                            resultProb = 0f
                        }
                    }
                }
            }) {
                Text("Predict")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (resultMessage.isNotEmpty()) {
                Text(resultMessage, color = Color.Red)
            }

            if (resultMessage.isEmpty() && rawInput.isNotEmpty()) {
                val (color, actionText) = when {
                    resultProb < 0.5f -> Pair(Color.Green, "Monitor the patient routinely.")
                    resultProb < 0.7f -> Pair(Color(0xFFFFA500), "Reassess the patient.")
                    else -> Pair(Color.Red, "Act immediately.")
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(color.copy(alpha = 0.6f))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Sepsis probability: %.2f".format(resultProb), color = color)
                Text(actionText, color = color)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (history.isNotEmpty()) {
                Text("Probability history")
                Canvas(modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)) {

                    val widthPerPoint = size.width / (history.size - 1).coerceAtLeast(1)
                    val maxProb = 1f

                    for (i in 0 until history.size - 1) {
                        val x1 = i * widthPerPoint
                        val y1 = size.height * (1 - history[i] / maxProb)
                        val x2 = (i + 1) * widthPerPoint
                        val y2 = size.height * (1 - history[i + 1] / maxProb)
                        drawLine(Color.Red, Offset(x1, y1), Offset(x2, y2), strokeWidth = 4f)
                    }

                    for (i in history.indices) {
                        val x = i * widthPerPoint
                        val y = size.height * (1 - history[i] / maxProb)
                        val pointColor = when {
                            history[i] < 0.5f -> Color.Green
                            history[i] < 0.7f -> Color(0xFFFFA500)
                            else -> Color.Red
                        }
                        drawCircle(pointColor, 8f, Offset(x, y))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (p in history) {
                        Text("%.2f".format(p), color = Color.Black)
                    }
                }
            }
        }
    }
}
