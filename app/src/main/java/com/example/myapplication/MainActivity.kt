
package com.example.myapplication

import kotlinx.coroutines.delay
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var pitch = 0f  // De hoek bepaald door de accelerometer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sta netwerkverkeer toe op de hoofdthread (alleen voor development)
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())

        // Initialiseer SensorManager en haal de accelerometer op
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            var isDarkMode by remember { mutableStateOf(false) } // Toggle voor dark mode
            var lastErrorMessage by remember { mutableStateOf("") } // Verplaats lastErrorMessage hierheen

            MyApplicationTheme(darkTheme = isDarkMode) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { ErrorBar(lastErrorMessage) } // Geef lastErrorMessage door aan ErrorBar
                ) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        TopBar(isDarkMode) { isDarkMode = it }  // Dark mode toggle in de top bar
                        ServoControl(lastErrorMessage = lastErrorMessage, onError = { lastErrorMessage = it })  // Geef lastErrorMessage door aan ServoControl
                    }
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0] // Links-rechts kanteling
            val z = event.values[2] // Gravitatie vector
            pitch = Math.toDegrees(Math.atan2(x.toDouble(), z.toDouble())).toFloat()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TopBar(isDarkMode: Boolean, onToggleDarkMode: (Boolean) -> Unit) {
        TopAppBar(
            title = { Text("Servo Controller") },
            actions = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isDarkMode, onCheckedChange = onToggleDarkMode)
                    Text(text = if (isDarkMode) "Dark" else "Light", fontSize = 14.sp)
                }
            }
        )
    }

    @Composable
    fun ServoControl(lastErrorMessage: String, onError: (String) -> Unit) {
        var sliderPosition by remember { mutableFloatStateOf(90f) }
        var feedbackMessage by remember { mutableStateOf("") }
        var currentServoAngle by remember { mutableStateOf(-1) }
        var isAutoMode by remember { mutableStateOf(false) } // Automatische modus aan/uit
        var offsetValue by remember { mutableStateOf(TextFieldValue("0")) } // Invoerveld voor offset
        var offset by remember { mutableStateOf(0f) } // Offset waarde
        var isOffsetConfirmed by remember { mutableStateOf(false) } // Controleer of OK is gedrukt

        LaunchedEffect(isAutoMode) {
            while (true) {
                if (isAutoMode) {
                    if (isOffsetConfirmed) {
                        // Draai 0 en 180 om (90 - pitch), en voeg de offset toe
                        val calculatedAngle = (90 - pitch + offset).coerceIn(0f, 180f)
                        sliderPosition = calculatedAngle

                        // Stuur de berekende hoek naar de ESP32
                        val result = sendServoPosition(calculatedAngle.toInt())
                        if (result.isSuccess) {
                            feedbackMessage = "Servo angle (sensor): ${calculatedAngle.toInt()}°"
                        } else {
                            feedbackMessage = "Error: Could not send servo angle"
                            onError(result.errorMessage ?: "Unknown error")
                        }
                    }
                }
                delay(500L)
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Schakelaar om tussen automatische en handmatige modus te schakelen
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = isAutoMode, onCheckedChange = { isAutoMode = it })
                Text("Automatic Mode")
            }

            // Invoerveld voor de offset, alleen in automatische modus zichtbaar
            if (isAutoMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = offsetValue,
                        onValueChange = {
                            // Controleer dat alleen numerieke invoer is toegestaan
                            if (it.text.all { char -> char.isDigit() || char == '.' }) {
                                offsetValue = it
                                isOffsetConfirmed = false // Offset is nog niet bevestigd
                            }
                        },
                        label = { Text("Offset") }
                    )
                    Button(onClick = {
                        offset = offsetValue.text.toFloatOrNull() ?: 0f
                        isOffsetConfirmed = true
                    }) {
                        Text("OK")
                    }
                }
            }

            // Tekst die links/rechts kanteling en sterkte weergeeft
            if (isAutoMode) {
                val tiltDirection = if (pitch > 0) "Left" else "Right"
                val tiltStrength = ((Math.abs(pitch) / 9).coerceIn(1f, 10f)).toInt()
                Text("Tilt: $tiltDirection, Strength: $tiltStrength")

                // Toon de servohoeken inclusief en exclusief offset
                val servoAngleWithoutOffset = (90 - pitch).coerceIn(0f, 180f)
                val servoAngleWithOffset = (90 - pitch + offset).coerceIn(0f, 180f)
                Text("Servo Angle (without offset): ${servoAngleWithoutOffset.toInt()}°")
                Text("Servo Angle (with offset): ${servoAngleWithOffset.toInt()}°")
            }

            if (!isAutoMode) {
                Slider(
                    value = sliderPosition,
                    valueRange = 0f..180f,
                    onValueChange = { sliderPosition = it }
                )
                Text(text = "Angle: ${sliderPosition.toInt()}°", style = MaterialTheme.typography.bodyLarge)

                Button(onClick = {
                    val result = sendServoPosition(sliderPosition.toInt())
                    if (result.isSuccess) {
                        feedbackMessage = "Servo angle sent: ${sliderPosition.toInt()}°"
                        onError("") // Clear errors
                    } else {
                        feedbackMessage = "Error: Could not send servo angle"
                        onError(result.errorMessage ?: "Unknown error")
                    }
                }, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Send")
                }
            }

            Text(text = feedbackMessage, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
        }
    }

    @Composable
    fun ErrorBar(lastErrorMessage: String) {
        // Venster onderaan voor foutmeldingen
        if (lastErrorMessage.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Red)
                    .padding(8.dp)
            ) {
                Text(
                    text = "Error: $lastErrorMessage",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    fun sendServoPosition(angle: Int): SendResult {
        val urlString = "http://192.168.4.1/setServo?angle=$angle"
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 750
            connection.readTimeout = 750

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d("HTTP", "Request success. Response code: $responseCode")
                SendResult(true)
            } else {
                Log.e("HTTP", "Failed to connect. Response code: $responseCode")
                SendResult(false, "Failed to connect. Response code: $responseCode")
            }
        } catch (e: Exception) {
            Log.e("HTTP", "Error: ${e.message}")
            SendResult(false, e.message)
        }
    }

    data class SendResult(val isSuccess: Boolean, val errorMessage: String? = null)
}

//package com.example.myapplication
//
//import kotlinx.coroutines.delay
//import android.os.Bundle
//import android.os.StrictMode
//import android.util.Log
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import com.example.myapplication.ui.theme.MyApplicationTheme
//import java.net.HttpURLConnection
//import java.net.URL
//
//// De MainActivity is de hoofdactiviteit van de Android-app, gebaseerd op de ComponentActivity.
//class MainActivity : ComponentActivity() {
//
//    // De onCreate-methode wordt aangeroepen wanneer de activiteit wordt gemaakt.
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // Sta netwerkverkeer toe op de hoofdthread (alleen voor development). Normaal gesproken mag netwerkverkeer
//        // niet op de hoofdthread plaatsvinden om vertragingen of bevriezing van de UI te voorkomen.
//        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
//
//        // Zet de content van de activiteit, dit is waar de interface van de app wordt opgebouwd.
//        setContent {
//            // Verwijst naar het MyApplicationTheme, dat zorgt voor thematisering van de interface (kleuren, lettertypes, etc.)
//            MyApplicationTheme {
//                // Scaffold biedt een standaard lay-outstructuur voor de app met een top bar, bottom bar, floating button, enz.
//                Scaffold(
//                    // Vul de beschikbare ruimte met deze lay-out
//                    modifier = Modifier.fillMaxSize()
//                ) { innerPadding ->
//                    // Kolom bevat items verticaal geordend
//                    Column(
//                        // Voeg padding toe binnen de opgegeven grenzen (bijvoorbeeld onder de top bar)
//                        modifier = Modifier.padding(innerPadding)
//                    ) {
//                        // Roep de functie aan om de top bar te renderen
//                        TopBar()
//                        // Roep de functie aan om de schuifregelaar (slider) weer te geven
//                        ServoSlider()
//                    }
//                }
//            }
//        }
//    }
//
//    // TopBar is een Composable functie die de top bar in de app weergeeft. Composable functies kunnen UI-elementen creëren.
//    @OptIn(ExperimentalMaterial3Api::class) // Geeft aan dat een experimentele API wordt gebruikt.
//    @Composable
//    fun TopBar() {
//        // TopAppBar is een standaard Material3 component dat een bovenste app-balk creëert met een titel.
//        TopAppBar(
//            title = { Text("Controller") } // Geeft de tekst "Controller" weer in de top bar.
//        )
//    }
//
//    // ServoSlider is een Composable functie die de slider en bijbehorende knoppen en tekst weergeeft.
//    @Composable
//    fun ServoSlider() {
//        var sliderPosition by remember { mutableFloatStateOf(90f) }  // De servo start op 90 graden.
//        var feedbackMessage by remember { mutableStateOf("") }
//        var lastSentPosition by remember { mutableStateOf(-1) }
//        var lastErrorMessage by remember { mutableStateOf("") }
//        var currentServoAngle by remember { mutableStateOf(-1) }  // Voeg deze variabele toe om de huidige servohoek op te slaan.
//
//         //Automatically refresh the current servo angle every 5 seconds
//        LaunchedEffect(Unit) {
//            while (true) {
//                val result = getCurrentServoAngle()
//                if (result.isSuccess) {
//                    currentServoAngle = result.angle ?: -1  // Update de huidige hoek
//                    feedbackMessage = "Current Servo angle: $currentServoAngle°"
//                } else {
//                    feedbackMessage = "Error: Could not get current servo angle"
//                    lastErrorMessage = result.errorMessage ?: "Unknown error"
//                }
//                delay(500L)  // Refresh every 500 ms.
//            }
//        }
//
//        Column(modifier = Modifier.padding(16.dp)) {
//            Slider(
//                value = sliderPosition,
//                valueRange = 0f..180f,
//                onValueChange = { sliderPosition = it }
//            )
//            Text(text = "Angle: ${sliderPosition.toInt()}°", style = MaterialTheme.typography.bodyLarge)
//
//            Button(onClick = {
//                val result = sendServoPosition(sliderPosition.toInt())
//                if (result.isSuccess) {
//                    lastSentPosition = sliderPosition.toInt()
//                    feedbackMessage = "Servo angle sent: ${sliderPosition.toInt()}°"
//                    lastErrorMessage = ""
//                } else {
//                    feedbackMessage = "Error: Could not send servo angle"
//                    lastErrorMessage = result.errorMessage ?: "Unknown error"
//                }
//            }, modifier = Modifier.padding(top = 16.dp)) {
//                Text("Send")
//            }
//
//            Text(text = feedbackMessage, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
//
//            if (lastErrorMessage.isNotEmpty()) {
//                Text(text = "Last Error: $lastErrorMessage", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
//            }
//
//
//        }
//    }
//
//
//    // Functie om de huidige servohoek van de ESP32 op te halen
//    fun getCurrentServoAngle(): GetAngleResult {
//        val urlString = "http://192.168.4.1/getServo"
//        return try {
//            val url = URL(urlString)
//            val connection = url.openConnection() as HttpURLConnection
//            connection.requestMethod = "GET"
//            connection.connectTimeout = 250
//            connection.readTimeout = 250
//
//            val responseCode = connection.responseCode
//            if (responseCode == HttpURLConnection.HTTP_OK) {
//                val response = connection.inputStream.bufferedReader().use { it.readText() }
//                // Extract the angle from the plain text response
//                val angle = Regex("Current Servo Angle:\\s*(\\d+)").find(response)?.groupValues?.get(1)?.toIntOrNull()
//                if (angle != null) {
//                    GetAngleResult(true, angle)
//                } else {
//                    GetAngleResult(false, errorMessage = "Invalid response format")
//                }
//            } else {
//                GetAngleResult(false, errorMessage = "Failed to connect. Response code: $responseCode")
//            }
//        } catch (e: Exception) {
//            GetAngleResult(false, errorMessage = e.message)
//        }
//    }
//
//
//
//    // Data class om het resultaat van het verkrijgen van de hoek op te slaan
//    data class GetAngleResult(val isSuccess: Boolean, val angle: Int? = null, val errorMessage: String? = null)
//
//
//
//    // Functie om de servo-hoek naar de ESP32 te sturen via een HTTP-request. Dit gebeurt buiten de composable functies.
//    fun sendServoPosition(angle: Int): SendResult {
//        // De URL van het ESP32-apparaat met de specifieke hoekwaarde in de query parameters.
//        val urlString = "http://192.168.4.1/setServo?angle=$angle"
//        return try {
//            // Maak een URL-object aan en open een HTTP-verbinding.
//            val url = URL(urlString)
//            val connection = url.openConnection() as HttpURLConnection
//            connection.requestMethod = "GET"  // Verzend een GET-verzoek naar de ESP32.
//            connection.connectTimeout = 750  // Stel een timeout van 250ms in voor het verbinden.
//            connection.readTimeout = 750  // Stel een timeout van 250ms in voor het lezen van de respons.
//
//            // Haal de HTTP-responscode op.
//            val responseCode = connection.responseCode
//            if (responseCode == HttpURLConnection.HTTP_OK) {
//                // Als de respons OK (200) is, log het en geef aan dat het verzoek succesvol was.
//                Log.d("HTTP", "Request success. Response code: $responseCode")
//                SendResult(true)
//            } else {
//                // Als de respons niet OK is, log het en geef een foutmelding.
//                Log.e("HTTP", "Failed to connect. Response code: $responseCode")
//                SendResult(false, "Failed to connect. Response code: $responseCode")
//            }
//        } catch (e: Exception) {
//            // Als er een uitzondering optreedt (bijv. netwerkfout), log de fout en geef een foutmelding terug.
//            Log.e("HTTP", "Error: ${e.message}")
//            SendResult(false, e.message)
//        }
//    }
//
//    // Data class die het resultaat van het versturen van de hoek beschrijft.
//    // isSuccess geeft aan of het versturen gelukt is, errorMessage bevat een eventuele foutmelding.
//    data class SendResult(val isSuccess: Boolean, val errorMessage: String? = null)
//}
