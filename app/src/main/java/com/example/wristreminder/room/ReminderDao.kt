package com.example.wristreminder.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(reminder: Reminder)

    @Update
    fun update(reminder: Reminder)

    @Delete
    fun delete(reminder: Reminder)

    @Query("SELECT * FROM reminders ORDER BY date ASC, time ASC")
    fun getAllReminders(): LiveData<List<Reminder>>

    @Query("SELECT * FROM reminders ORDER BY date ASC, time ASC")
    suspend fun getAllRemindersSync(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE completed = 0 ORDER BY priority DESC, date, time")
    fun getPendingReminders(): LiveData<List<Reminder>>

    @Query("UPDATE reminders SET completed = :isCompleted WHERE id = :reminderId")
    fun updateReminderCompletion(reminderId: Int, isCompleted: Boolean)

    @Query("SELECT id FROM reminders ORDER BY id DESC LIMIT 1")
    fun getLastId(): Int

    @Query("UPDATE reminders SET googleEventId = :eventId WHERE id = :reminderId")
    fun updateGoogleEventId(reminderId: Int, eventId: String)

    @Query("UPDATE reminders SET completed = :isCompleted WHERE id = :reminderId")
    suspend fun updateCompletion(reminderId: Int, isCompleted: Boolean)

// 新增统计查询
@Query("SELECT COUNT(*) FROM reminders WHERE completed = 1")
fun getCompletedCount(): LiveData<Int>

@Query("SELECT COUNT(*) FROM reminders WHERE completed = 0")
fun getPendingCount(): LiveData<Int>

@Query("SELECT COUNT(*) FROM reminders WHERE completed = 1 AND date = :date")
fun getCompletedCountByDate(date: String): LiveData<Int>

@Query("SELECT COUNT(*) FROM reminders WHERE date = :date")
fun getTotalCountByDate(date: String): LiveData<Int>

@Query("""
    SELECT date, COUNT(*) as count 
    FROM reminders 
    WHERE completed = 1 
    AND date >= date('now', '-6 days') 
    AND date <= date('now')
    GROUP BY date
""")
fun getCompletedCountLast7Days(): LiveData<List<DateCount>>

@Query("SELECT priority, COUNT(*) as count FROM reminders WHERE completed = 1 GROUP BY priority")
fun getCompletedCountByPriority(): LiveData<List<PriorityCount>>

@Query("SELECT * FROM reminder_settings WHERE priority = :priority LIMIT 1")
fun getReminderSettings(priority: Int): LiveData<ReminderSettings?>

@Query("SELECT * FROM reminder_settings WHERE priority = :priority LIMIT 1")
suspend fun getReminderSettingsByPriority(priority: Int): ReminderSettings?

@Insert(onConflict = OnConflictStrategy.REPLACE)
fun insertReminderSettings(reminderSettings: ReminderSettings)

@Update
suspend fun updateReminderSettings(settings: ReminderSettings)

@Query("SELECT * FROM reminders WHERE id = :id")
suspend fun getReminderById(id: Int): Reminder?

@Query("SELECT * FROM reminder_settings WHERE priority = :priority")
suspend fun getReminderSettingsSync(priority: Int): ReminderSettings?
}