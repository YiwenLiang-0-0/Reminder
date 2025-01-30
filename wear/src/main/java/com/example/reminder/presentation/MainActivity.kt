package com.example.reminder.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.wear.compose.material.MaterialTheme
import androidx.compose.material.AlertDialog
import androidx.compose.material.TextField
import androidx.compose.material.TextButton
import androidx.compose.ui.text.style.TextDecoration


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
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val datetime: LocalDateTime,
    val isUrgent: Boolean = false,
    val isCompleted: Boolean = false
)

@Composable
fun ReminderApp() {
    var events by remember { mutableStateOf(
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
    )}

    var showAddEventDialog by remember { mutableStateOf(false) }
    var newEventDateTime by remember { mutableStateOf(LocalDateTime.now()) }
    var newEventTitle by remember { mutableStateOf("") }
    var newEventIsUrgent by remember { mutableStateOf(false) }
    var showAddEventScreen by remember { mutableStateOf(false) }


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
            val groupedEvents = events
                .filterNot { it.isCompleted }
                .groupBy { it.datetime.toLocalDate() }
                .toSortedMap()

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

                items(dateEvents.sortedBy { it.datetime }) { event ->
                    EventCard(
                        event = event,
                        onToggleComplete = {
                            events = events.map {
                                if (it.id == event.id) it.copy(isCompleted = !it.isCompleted)
                                else it
                            }
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = { showAddEventDialog = true },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add event"
                )
            }
        }
        if (showAddEventDialog) {
            AlertDialog(
                onDismissRequest = { showAddEventDialog = false },
                title = { Text("Add New Event") },
                text = {
                    Column {
                        TextField(
                            value = newEventTitle,
                            onValueChange = { newEventTitle = it },
                            label = { Text("Event Title") }
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = newEventIsUrgent,
                                onCheckedChange = { newEventIsUrgent = it }
                            )
                            Text("Urgent")
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newEventTitle.isNotBlank()) {
                                events = events + Event(
                                    title = newEventTitle,
                                    datetime = newEventDateTime,
                                    isUrgent = newEventIsUrgent
                                )
                                showAddEventDialog = false
                                newEventTitle = ""
                                newEventIsUrgent = false
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAddEventDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    onToggleComplete: () -> Unit
) {
    Card(
        onClick = onToggleComplete,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = Color(0xFF9DB4AB),
            endBackgroundColor = Color(0xFF9DB4AB)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.title3,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (event.isCompleted) TextDecoration.LineThrough else null
                )

                if (event.isUrgent) {
                    Text(
                        text = "Urgent",
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.secondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = when {
                        event.datetime.toLocalDate() == LocalDateTime.now().toLocalDate() -> {
                            val hours = LocalDateTime.now().until(event.datetime, ChronoUnit.HOURS)
                            if (hours == 0L) "Now" else "${hours}h"
                        }
                        event.datetime.isBefore(LocalDateTime.now()) -> "Overdue"
                        else -> "${ChronoUnit.DAYS.between(LocalDateTime.now(), event.datetime)}d"
                    },
                    style = MaterialTheme.typography.caption2
                )

                Icon(
                    imageVector = if (event.isCompleted) Icons.Filled.Check else Icons.Filled.CheckCircle,
                    contentDescription = if (event.isCompleted) "Completed" else "Mark as completed",
                    tint = Color(0xFFFFEB3B)
                )
            }
        }
    }
}
