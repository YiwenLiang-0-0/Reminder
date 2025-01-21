/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.example.reminder.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/// MainActivity.kt
import androidx.activity.compose.setContent
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.material.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearAppTheme {
                ReminderApp()
            }
        }
    }
}

@Composable
fun WearAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            background = Color(0xFF58788A),
            primary = Color(0xFF1B1B1B),
            secondary = Color(0xFF03DAC6),
            surface = Color(0xFF9DB4AB)
        ),
        content = content
    )
}

data class Event(
    val title: String,
    val datetime: LocalDateTime,
    val isUrgent: Boolean = false
)

@Composable
fun ReminderApp() {
    // Sample events
    val events by remember {
        mutableStateOf(
            listOf(
                Event(
                    title = "Lecture1",
                    datetime = LocalDateTime.now(),
                    isUrgent = true
                ),
                Event(
                    title = "Coursework1",
                    datetime = LocalDateTime.now().plusHours(3),
                    isUrgent = true
                ),
                Event(
                    title = "Meeting1",
                    datetime = LocalDateTime.now().plusDays(1)
                )
            )
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {

        ScalingLazyColumn {
            item {
                TimeText()
            }
            item {
                Text(
                    "Upcoming events",
                    style = MaterialTheme.typography.title2,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Group events by date
            val groupedEvents = events.groupBy {
                it.datetime.toLocalDate()
            }

            groupedEvents.forEach { (date, dateEvents) ->
                item {
                    Text(
                        text = if (date == LocalDateTime.now().toLocalDate())
                            "Today, ${date.format(DateTimeFormatter.ofPattern("dd MMM"))}"
                        else
                            date.format(DateTimeFormatter.ofPattern("dd MMM")),
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.secondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(dateEvents) { event ->
                    EventCard(event)
                }
            }
        }
    }
}

@Composable
fun EventCard(event: Event) {
    androidx.wear.compose.material.Card(
        onClick = { /* Handle click */ },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = Color(0xFFA6E9C5).copy(alpha = 0.5f),
            endBackgroundColor = Color(0xFF8E8E8E).copy(alpha = 0.5f)
        )
    )  {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.title3,
                    fontWeight = FontWeight.Bold
                )

                // Display relative time
                Text(
                    text = when {
                        event.datetime.toLocalDate() == LocalDateTime.now().toLocalDate() -> {
                            val hours = LocalDateTime.now().until(event.datetime, java.time.temporal.ChronoUnit.HOURS)
                            if (hours == 0L) "Now" else "${hours}h"
                        }
                        else -> "1d"
                    },
                    style = MaterialTheme.typography.caption2
                )
            }

            if (event.isUrgent) {
                Text(
                    text = "urgent",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.secondary
                )
            }
        }
    }
}


