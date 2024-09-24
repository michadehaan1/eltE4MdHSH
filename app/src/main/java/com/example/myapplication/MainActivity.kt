package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


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
                        First(
                            name_one = "Planten Monitor"
                        )
                        Second(
                            name_two = "0.1"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun First(name_one: String, modifier: Modifier = Modifier) {
    Surface(color = Color.LightGray) {
        Text(
            text = "App : $name_one",
            //modifier = modifier
            //modifier = Modifier.padding(8.dp) // Add padding.
            //modifier = Modifier.androidx.compose.ui.Modifier.size(30.dp) // Not working.
        )
    }
}
@Composable
fun Second(name_two: String, modifier: Modifier = Modifier) {
    Surface(color = Color.LightGray) {
        Text(
            text = "Version : $name_two",
            //modifier = modifier
            //modifier = Modifier.padding(8.dp) // Add padding.
            //modifier = Modifier.androidx.compose.ui.Modifier.size(30.dp) // Not working.
        )
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        First("Planten monitor")
        Second("V0.1")
    }
}