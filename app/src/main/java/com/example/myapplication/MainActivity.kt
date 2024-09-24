package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.Icon



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                        .padding(innerPadding)
                    ) {
                        TopBar()
                        AppInfo(
                            app_name = "Planten Monitor",
                            app_version = "0.1"
                        )
                        ConnectDialog()
                        //Slider()

                    }
                }
            }
        }
    }
}

// https://developer.android.com/develop/ui/compose/components/slider Geraadpleegd op 24-09-2024, door S. Helmantel.
@Preview
@Composable
fun Slider() {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    Column {
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it }
        )
        Text(text = sliderPosition.toString())
    }
}

// https://developer.android.com/develop/ui/compose/components/app-bars Geraadpleegd op 24-09-2024, door S. Helmantel.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar() {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("Planten Monitor")
                }
            )
}

@Composable
fun ConnectDialog() {
    val openAlertDialog = remember { mutableStateOf(false) }

    // Your logic to open the dialog (e.g., button click) would go here
    // Button to open the AlertDialog
    Button(onClick = { openAlertDialog.value = true }) {
        Text("Connect")
    }
    if (openAlertDialog.value) {
        AlertDialog(
            onDismissRequest = {
                openAlertDialog.value = false
            },
            confirmButton = {
                Button(onClick = {
                    openAlertDialog.value = false
                    println("Confirmation registered")
                    // Add more logic for confirmation here
                }) {
                    Text("Connect")
                }
            },
            dismissButton = {
                Button(onClick = { openAlertDialog.value = false }) {
                    Text("Cancel")
                }
            },
            title = {
                Text("Connect to sensor")
            },
            text = {
                Text("Press the connect button to show connections.")
            },
            icon = {
                Icon(imageVector = Icons.Default.Info, contentDescription = null)
            }
        )
    }
}

@Composable
fun AppInfo(app_name: String, app_version: String, modifier: Modifier = Modifier) {
    Surface(color = Color.LightGray) {
        Text(
            text = "App : $app_name" +
            "\nVersion : $app_version",
            //modifier = modifier
            //modifier = Modifier.padding(8.dp) // Add padding.
            //modifier = Modifier.androidx.compose.ui.Modifier.size(30.dp) // Not working.
        )
    }
}