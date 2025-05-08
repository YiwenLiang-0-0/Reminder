package com.example.wristreminder.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "time") val time: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "priority") val priority: Int, // 0: l, 1: m, 2: h
    @ColumnInfo(name = "completed") val completed: Boolean,
    @ColumnInfo(name = "sound") val sound: Boolean,
    @ColumnInfo(name = "googleEventId") val googleEventId: String = "",
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis()
)
