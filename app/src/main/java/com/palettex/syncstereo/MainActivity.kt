package com.palettex.syncstereo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import com.palettex.syncstereo.ui.theme.SyncStereoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SyncStereoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val test = innerPadding
//                    YouTubePlayerScreen(
//                        modifier = Modifier.padding(innerPadding)
//                    )
                    YouTubePlayerScreen()
                }
            }
        }
    }
}

@Composable
fun YouTubePlayerScreen() {
    // YouTube video ID (replace with your desired video)
//    val videoId = "dQw4w9WgXcQ"
    val videoId ="5Dn-_UzLmbM"

    // State for scheduling inputs
    var scheduleHour by remember { mutableStateOf("") }
    var scheduleMinute by remember { mutableStateOf("") }
    var scheduleSecond by remember { mutableStateOf("") }

    // State for seek inputs
    var seekMinute by remember { mutableStateOf("1") }
    var seekSecond by remember { mutableStateOf("32") }

    // YouTube player reference
    var youTubePlayer by remember { mutableStateOf<YouTubePlayer?>(null) }

    // Obtain the current lifecycle owner
    val lifecycleOwner = LocalLifecycleOwner.current
    // Live UTC time state
    var currentUTCTime by remember { mutableStateOf(Instant.now()) }


    // Live clock update
    LaunchedEffect(Unit) {
        while(true) {
            currentUTCTime = Instant.now()
            delay(1000) // Update every second
        }
    }


    // Scheduled play function
    fun scheduleVideoPlay(hour: Int, minute: Int, second: Int) {
        lifecycleOwner.lifecycleScope.launch {
            // Create the target time in UTC
            val now = Instant.now()
            val targetTime = now.atOffset(ZoneOffset.UTC)
                .withHour(hour)
                .withMinute(minute)
                .withSecond(second)
                .withNano(0)
                .toInstant()

            // Calculate delay
            val delayMillis = targetTime.toEpochMilli() - now.toEpochMilli()

            if (delayMillis > 0) {
                Log.d("ScheduledPlay", "Waiting to play video at: ${
                    targetTime.atOffset(ZoneOffset.UTC).format(
                        DateTimeFormatter.ISO_LOCAL_TIME
                    )
                }")
                delay(delayMillis)
                youTubePlayer?.play()
                Log.d("ScheduledPlay", "Video started at scheduled time")
            } else {
                Log.d("ScheduledPlay", "Scheduled time is in the past")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // YouTube Player View
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            factory = { context ->
                YouTubePlayerView(context).apply {
                    // Attach the lifecycle observer
                    lifecycleOwner.lifecycle.addObserver(this)
                    addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                        override fun onReady(player: YouTubePlayer) {
                            youTubePlayer = player
                            youTubePlayer?.addListener(CustomYouTubePlayerListener())
                            player.loadVideo(videoId, 0f)
                        }
                    })
                }
            },
            // Clean up the YouTubePlayerView when the composable is disposed
            update = { youTubePlayerView ->
                lifecycleOwner.lifecycle.addObserver(youTubePlayerView)
            }
        )

        // Seek Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Minute Input
            OutlinedTextField(
                value = seekMinute,
                onValueChange = {
                    // Ensure only numeric input
                    seekMinute = it.filter { char -> char.isDigit() }
                },
                label = { Text("Minutes") },
                modifier = Modifier.width(100.dp)
            )

            // Second Input
            OutlinedTextField(
                value = seekSecond,
                onValueChange = {
                    // Ensure only numeric input
                    seekSecond = it.filter { char -> char.isDigit() }
                },
                label = { Text("Seconds") },
                modifier = Modifier.width(100.dp)
            )

            // Seek Button
            Button(
                onClick = {
                    // Convert inputs to seconds
                    val totalSeconds =
                        (seekMinute.toIntOrNull() ?: 0) * 60 +
                                (seekSecond.toIntOrNull() ?: 0)

                    // Seek to the specified time
                    youTubePlayer?.seekTo(totalSeconds.toFloat())
                }
            ) {
                Text("Seek")
            }


        }
        // Seek Button
        Button(
            onClick = {
                // Convert inputs to total seconds
                val totalSeconds = (seekMinute.toIntOrNull() ?: 0) * 60 + (seekSecond.toIntOrNull() ?: 0)
                // Seek to the specified time and pause
                youTubePlayer?.seekTo(totalSeconds.toFloat())
                youTubePlayer?.pause()
            }
        ) {
            Text("Seek & Pause")
        }

        // Play and Pause Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play Button
            Button(
                onClick = {
                    youTubePlayer?.play()
                }
            ) {
                Text("Play")
            }

            // Pause Button
            Button(
                onClick = {
                    youTubePlayer?.pause()
                }
            ) {
                Text("Pause")
            }
        }

        // Format for displaying time
        val utcTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC)
        // Live UTC Time Display
        Text(
            text = "Current UTC Time: ${utcTimeFormatter.format(currentUTCTime)}",
            modifier = Modifier.padding(bottom = 16.dp)
        )


        // Scheduling Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hour Input
            OutlinedTextField(
                value = scheduleHour,
                onValueChange = {
                    scheduleHour = it.filter { char -> char.isDigit() }.take(2)
                },
                label = { Text("Hour (UTC)") },
                modifier = Modifier.width(100.dp)
            )

            // Minute Input
            OutlinedTextField(
                value = scheduleMinute,
                onValueChange = {
                    scheduleMinute = it.filter { char -> char.isDigit() }.take(2)
                },
                label = { Text("Minute") },
                modifier = Modifier.width(100.dp)
            )

            // Second Input
            OutlinedTextField(
                value = scheduleSecond,
                onValueChange = {
                    scheduleSecond = it.filter { char -> char.isDigit() }.take(2)
                },
                label = { Text("Second") },
                modifier = Modifier.width(100.dp)
            )
        }

        // Schedule Play Button
        Button(
            onClick = {
                val hour = scheduleHour.toIntOrNull() ?: 0
                val minute = scheduleMinute.toIntOrNull() ?: 0
                val second = scheduleSecond.toIntOrNull() ?: 0

                scheduleVideoPlay(hour, minute, second)
            }
        ) {
            Text("Schedule Play")
        }
    }
}

class CustomYouTubePlayerListener : AbstractYouTubePlayerListener() {
    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
        // Log the current playback time in seconds
        Log.d("GDT", "Current playback time: $second seconds")
    }
}