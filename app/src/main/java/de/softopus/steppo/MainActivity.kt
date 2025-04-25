package de.softopus.steppo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request activity recognition permission for step counting
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* Handle permission result if needed */ }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        setContent {
            SteppoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SteppoApp()
                }
            }
        }
    }
}

class StepCounterViewModel : ViewModel() {
    var stepCount by mutableStateOf(0)
    var distance by mutableStateOf(0.0f)
    var isTracking by mutableStateOf(false)

    // Default stride length in meters
    private val strideLength = 0.76f

    fun startTracking() {
        isTracking = true
    }

    fun stopTracking() {
        isTracking = false
    }

    fun updateSteps(steps: Int) {
        if (isTracking) {
            stepCount = steps
            // Calculate distance in meters based on steps and stride length
            distance = steps * strideLength
        }
    }

    fun resetCounters() {
        stepCount = 0
        distance = 0.0f
    }
}

@Composable
fun SteppoApp(viewModel: StepCounterViewModel = viewModel()) {
    val context = LocalContext.current
    var sensorManager: SensorManager? = null
    var stepSensor: Sensor? = null

    DisposableEffect(key1 = viewModel.isTracking) {
        if (viewModel.isTracking) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

            val sensorEventListener = object : SensorEventListener {
                private var initialSteps = -1

                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                        val totalSteps = event.values[0].toInt()

                        if (initialSteps == -1) {
                            initialSteps = totalSteps
                        }

                        val currentSteps = totalSteps - initialSteps
                        viewModel.updateSteps(currentSteps)
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    // Not needed for this implementation
                }
            }

            stepSensor?.let {
                sensorManager?.registerListener(
                    sensorEventListener,
                    it,
                    SensorManager.SENSOR_DELAY_UI
                )
            }

            onDispose {
                sensorManager?.unregisterListener(sensorEventListener)
            }
        } else {
            onDispose { }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "STEPPO",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Steps display
        CounterCard(
            title = "Schritte",
            value = viewModel.stepCount.toString()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Distance display
        CounterCard(
            title = "Meter",
            value = String.format("%.2f", viewModel.distance)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    if (!viewModel.isTracking) {
                        viewModel.resetCounters()
                        viewModel.startTracking()
                    }
                }
            ) {
                Text(text = "START")
            }

            Button(
                onClick = { viewModel.stopTracking() }
            ) {
                Text(text = "STOP")
            }
        }
    }
}

@Composable
fun CounterCard(title: String, value: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SteppoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}