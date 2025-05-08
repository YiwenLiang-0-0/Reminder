package com.example.wristreminder.presentation

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.example.wristreminder.presentation.ui.theme.WristReminderTheme
import com.example.wristreminder.room.ReminderSettings

class ReminderSettingsActivity : ComponentActivity() {
    private val blue = 0xFF2175F3
    
    // Instead of using lazy initialization, use regular properties
    // and initialize them in onCreate
    private var initialPriority = 1
    private var initialAdvanceMinutes = 0
    private var initialRepeatInterval: Int? = null
    private var initialMaxReminders = 1
    private var initialSoundUri = ""
    
    // Mutable state for the current values
    private val selectedSoundUri = mutableStateOf("")
    private val selectedSoundName = mutableStateOf("Default")
    private val advanceMinutes = mutableStateOf(0)
    private val repeatInterval = mutableStateOf<Int?>(null)
    private val maxReminders = mutableStateOf(1)
    
    // Sound picker launcher
    private val soundPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            uri?.let {
                selectedSoundUri.value = it.toString()
                // Get the sound name from RingtoneManager
                val ringtone = RingtoneManager.getRingtone(this, uri)
                selectedSoundName.value = ringtone.getTitle(this) ?: "Custom Sound"
                Log.d("ReminderSettings", "Selected sound: ${selectedSoundName.value}, URI: ${selectedSoundUri.value}")
            } ?: run {
                // Handle null URI (user selected "None")
                selectedSoundUri.value = ""
                selectedSoundName.value = "None"
                Log.d("ReminderSettings", "Sound selection cleared")
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize these properties here, where intent is guaranteed to exist
        initialPriority = intent.getIntExtra("priority", 1)
        initialAdvanceMinutes = intent.getIntExtra("advanceMinutes", 0)
        initialRepeatInterval = if (intent.hasExtra("repeatInterval")) 
            intent.getIntExtra("repeatInterval", 15) else null
        initialMaxReminders = intent.getIntExtra("maxReminders", 1)
        initialSoundUri = intent.getStringExtra("soundUri") ?: ""
        
        // Initialize state values
        selectedSoundUri.value = initialSoundUri
        advanceMinutes.value = initialAdvanceMinutes
        repeatInterval.value = initialRepeatInterval
        maxReminders.value = initialMaxReminders
        
        Log.d("ReminderSettings", "Received settings - Priority: $initialPriority, Sound URI: $initialSoundUri")
        
        // Initialize sound name if we have a URI
        if (initialSoundUri.isNotEmpty()) {
            try {
                val uri = Uri.parse(initialSoundUri)
                val ringtone = RingtoneManager.getRingtone(this, uri)
                selectedSoundName.value = ringtone.getTitle(this) ?: "Custom Sound"
                Log.d("ReminderSettings", "Loaded sound: ${selectedSoundName.value}")
            } catch (e: Exception) {
                Log.e("ReminderSettings", "Error loading ringtone: ${e.message}")
                selectedSoundName.value = "Default"
            }
        } else {
            // Try to get the default notification sound name
            try {
                val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(this, defaultUri)
                selectedSoundName.value = "Default (${ringtone.getTitle(this)})"
            } catch (e: Exception) {
                selectedSoundName.value = "Default"
            }
        }
        
        setContent {
            WristReminderTheme {
                SettingsScreen(
                    initialSettings = ReminderSettings(
                        priority = initialPriority,
                        advanceMinutes = initialAdvanceMinutes,
                        repeatInterval = initialRepeatInterval,
                        maxReminders = initialMaxReminders,
                        soundUri = initialSoundUri
                    ),
                    soundName = selectedSoundName.value,
                    currentAdvanceMinutes = advanceMinutes.value,
                    currentRepeatInterval = repeatInterval.value,
                    currentMaxReminders = maxReminders.value,
                    onAdvanceMinutesChanged = { advanceMinutes.value = it },
                    onRepeatIntervalChanged = { repeatInterval.value = it },
                    onMaxRemindersChanged = { maxReminders.value = it },
                    onSelectSound = { openSoundPicker() },
                    onSave = {
                        // Collect all current values
                        val resultIntent = Intent().apply {
                            putExtra("priority", initialPriority)
                            putExtra("advanceMinutes", advanceMinutes.value)
                            if (repeatInterval.value != null) {
                                putExtra("repeatInterval", repeatInterval.value)
                            }
                            putExtra("maxReminders", maxReminders.value)
                            putExtra("soundUri", selectedSoundUri.value)
                        }
                        
                        Log.d("ReminderSettings", "Saving settings - Priority: $initialPriority, " +
                              "Sound URI: ${selectedSoundUri.value}")
                        
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    },
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }
    
    private fun openSoundPicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Reminder Sound")
            if (selectedSoundUri.value.isNotEmpty()) {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(selectedSoundUri.value))
            }
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        }
        soundPickerLauncher.launch(intent)
    }
    
    @Composable
    fun SettingsScreen(
        initialSettings: ReminderSettings,
        soundName: String,
        currentAdvanceMinutes: Int,
        currentRepeatInterval: Int?,
        currentMaxReminders: Int,
        onAdvanceMinutesChanged: (Int) -> Unit,
        onRepeatIntervalChanged: (Int?) -> Unit,
        onMaxRemindersChanged: (Int) -> Unit,
        onSelectSound: () -> Unit,
        onSave: () -> Unit,
        onCancel: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF58788A))
        ) {
            Scaffold(
                timeText = { TimeText() },
                vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
                positionIndicator = {
                    PositionIndicator(scalingLazyListState = rememberScalingLazyListState())
                }
            ) {
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = rememberScalingLazyListState(),
                    contentPadding = PaddingValues(
                        top = 24.dp,
                        start = 10.dp,
                        end = 10.dp,
                        bottom = 24.dp
                    )
                ) {
                    // Title
                    item {
                        Text(
                            "Reminder Settings",
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )
                    }
                    
                    // Sound selection
                    item {
                        Chip(
                            onClick = onSelectSound,
                            label = { Text("Sound: $soundName") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // Advanced reminder settings
                    item {
                        ToggleChip(
                            checked = currentAdvanceMinutes > 0,
                            onCheckedChange = { checked ->
                                onAdvanceMinutesChanged(if (checked) 5 else 0)
                            },
                            label = { Text("Advance reminder") },
                            toggleControl = {
                                Switch(checked = currentAdvanceMinutes > 0)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    if (currentAdvanceMinutes > 0) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Chip(
                                onClick = {
                                    val newValue = when (currentAdvanceMinutes) {
                                        5 -> 10
                                        10 -> 15
                                        15 -> 30
                                        30 -> 5
                                        else -> 5
                                    }
                                    onAdvanceMinutesChanged(newValue)
                                },
                                label = { Text("${currentAdvanceMinutes}min before") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    // Repeat reminder settings
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        ToggleChip(
                            checked = currentRepeatInterval != null,
                            onCheckedChange = { checked ->
                                onRepeatIntervalChanged(if (checked) 15 else null)
                                if (!checked) onMaxRemindersChanged(1)
                            },
                            label = { Text("Repeat reminder") },
                            toggleControl = {
                                Switch(checked = currentRepeatInterval != null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    if (currentRepeatInterval != null) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Chip(
                                onClick = {
                                    val newValue = when (currentRepeatInterval) {
                                        5 -> 10
                                        10 -> 15
                                        15 -> 30
                                        30 -> 5
                                        else -> 15
                                    }
                                    onRepeatIntervalChanged(newValue)
                                },
                                label = { Text("Every ${currentRepeatInterval}min") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Chip(
                                onClick = {
                                    val newValue = when (currentMaxReminders) {
                                        1 -> 2
                                        2 -> 3
                                        3 -> 5
                                        5 -> 1
                                        else -> 2
                                    }
                                    onMaxRemindersChanged(newValue)
                                },
                                label = { Text("Repeat $currentMaxReminders times") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    // Buttons
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = onCancel,
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray)
                            ) {
                                Text("Cancel")
                            }
                            
                            Button(
                                onClick = onSave,
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(blue))
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }
} 