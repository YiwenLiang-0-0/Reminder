package com.example.wristreminder.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_settings")
data class ReminderSettings(
    @PrimaryKey val priority: Int,
    val advanceMinutes: Int = 0,
    val repeatInterval: Int? = null,
    val maxReminders: Int = 1,
    val soundUri: String = ""
) 