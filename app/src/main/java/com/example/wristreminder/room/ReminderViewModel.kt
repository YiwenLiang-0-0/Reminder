package com.example.wristreminder.room

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.withContext

class ReminderViewModel(private val dao: ReminderDao) : ViewModel() {
    val reminders: LiveData<List<Reminder>> = dao.getAllReminders()
    // 新增统计相关数据
    val completedCount: LiveData<Int> = dao.getCompletedCount()
    val pendingCount: LiveData<Int> = dao.getPendingCount()
    val completedLast7Days: LiveData<List<DateCount>> = dao.getCompletedCountLast7Days()
    val completedByPriority: LiveData<List<PriorityCount>> = dao.getCompletedCountByPriority()
    // 今日完成的任务
    val todayCompletedCount: LiveData<Int> by lazy {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        dao.getCompletedCountByDate(today)
    }

    // 今日总任务
    val todayTotalCount: LiveData<Int> by lazy {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        dao.getTotalCountByDate(today)
    }

    // 完成率计算
    val completionRate = MediatorLiveData<Float>().apply {
        var completed = 0
        var total = 0

        addSource(completedCount) { count ->
            completed = count ?: 0
            value = if (total > 0) completed.toFloat() / total else 0f
        }

        addSource(reminders) { list ->
            total = list?.size ?: 0
            value = if (total > 0) completed.toFloat() / total else 0f
        }
    }

    // 修改获取过去7天数据的方式
    private val _last7DaysStats = MediatorLiveData<List<DateCount>>()
    val last7DaysStats: LiveData<List<DateCount>> = _last7DaysStats

    init {
        // 监听数据库返回的完成数据
        _last7DaysStats.addSource(dao.getCompletedCountLast7Days()) { dbStats ->
            // 生成过去7天的日期列表
            val last7Days = (0..6).map { daysAgo ->
                val date = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -daysAgo)
                }
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date.time)
            }.reversed()

            // 将数据库数据转换为Map，方便查找
            val statsMap = dbStats.associateBy { it.date }

            // 生成完整的7天数据，没有数据的日期计数为0
            val completeStats = last7Days.map { date ->
                statsMap[date] ?: DateCount(date, 0)
            }

            _last7DaysStats.value = completeStats
        }
    }

    // 修改统计摘要中的数据源
    val statsSummary = MediatorLiveData<StatsSummary>().apply {
        var total = 0
        var completed = 0
        var pending = 0
        var dailyStats: List<DateCount> = emptyList()
        var priorityStats: List<PriorityCount> = emptyList()

        fun updateValue() {
            value = StatsSummary(
                totalTasks = total,
                completedTasks = completed,
                pendingTasks = pending,
                completionRate = if (total > 0) completed.toFloat() / total else 0f,
                dailyStats = dailyStats,
                priorityStats = priorityStats
            )
        }

        // 更新数据源，使用新的 last7DaysStats
        addSource(last7DaysStats) { stats ->
            dailyStats = stats
            updateValue()
        }

        addSource(reminders) { list ->
            total = list?.size ?: 0
            updateValue()
        }

        addSource(completedCount) { count ->
            completed = count ?: 0
            updateValue()
        }

        addSource(pendingCount) { count ->
            pending = count ?: 0
            updateValue()
        }

        addSource(completedByPriority) { stats ->
            priorityStats = stats ?: emptyList()
            updateValue()
        }
    }
    fun updateCompletion(reminderId: Int, isCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateCompletion(reminderId, isCompleted)
        }
    }
    fun addReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(reminder)
            _lastId = dao.getLastId()
            _lastIdLiveData.postValue(_lastId)  // 通知 ID 更新
        }
    }
    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(reminder)
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.update(reminder)
        }
    }

    fun getLastId(): Int {
        return _lastId
    }

    // 获取指定日期的任务完成率
    fun getCompletionRateForDate(date: String): LiveData<Float> {
        val result = MediatorLiveData<Float>()
        val completedForDate = dao.getCompletedCountByDate(date)
        val totalForDate = dao.getTotalCountByDate(date)

        var completed = 0
        var total = 0

        result.addSource(completedForDate) { count ->
            completed = count ?: 0
            result.value = if (total > 0) completed.toFloat() / total else 0f
        }

        result.addSource(totalForDate) { count ->
            total = count ?: 0
            result.value = if (total > 0) completed.toFloat() / total else 0f
        }

        return result
    }

    // 获取提醒设置
    fun getReminderSettings(priority: Int): LiveData<ReminderSettings?> {
        return dao.getReminderSettings(priority)
    }

    // 更新提醒设置
    fun updateReminderSettings(settings: ReminderSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 首先检查是否已有此优先级的设置
                val existingSettings = dao.getReminderSettingsSync(settings.priority)
                
                Log.d("ReminderViewModel", "Updating settings: Priority=${settings.priority}, " +
                    "AdvMin=${settings.advanceMinutes}, RepInt=${settings.repeatInterval}, " +
                    "MaxRem=${settings.maxReminders}, Sound=${settings.soundUri}")
                Log.d("ReminderViewModel", "Existing settings: ${existingSettings != null}")
                
                if (existingSettings != null) {
                    // 如果存在设置，则更新
                    Log.d("ReminderViewModel", "Updating existing settings")
                    dao.updateReminderSettings(settings)
                } else {
                    // 如果设置不存在，则插入
                    Log.d("ReminderViewModel", "Inserting new settings")
                    dao.insertReminderSettings(settings)
                }
            } catch (e: Exception) {
                Log.e("ReminderViewModel", "Error updating settings: ${e.message}", e)
            }
        }
    }

    // 获取默认设置
    private fun getDefaultSettings(priority: Int) = ReminderSettings(
        priority = priority,
        advanceMinutes = when (priority) {
            0 -> 0     // Low priority: 准时提醒
            1 -> 5     // Normal priority: 提前5分钟
            2 -> 15    // Urgent priority: 提前15分钟
            else -> 0
        },
        repeatInterval = when (priority) {
            0 -> null  // Low priority: 不重复
            1 -> 15    // Normal priority: 每15分钟
            2 -> 5     // Urgent priority: 每5分钟
            else -> null
        },
        maxReminders = when (priority) {
            0 -> 1     // Low priority: 提醒1次
            1 -> 2     // Normal priority: 提醒2次
            2 -> 3     // Urgent priority: 提醒3次
            else -> 1
        }
    )

    // 添加一个私有变量跟踪最后插入的 ID
    private var _lastId: Int = -1

    // 公开一个 LiveData 以观察最后插入的 ID
    private val _lastIdLiveData = MutableLiveData<Int>()
    fun observeLastId(): LiveData<Int> = _lastIdLiveData

    // 同步获取提醒设置
    suspend fun getReminderSettingsSync(priority: Int): ReminderSettings? {
        return withContext(Dispatchers.IO) {
            try {
                val settings = dao.getReminderSettingsByPriority(priority)
                Log.d("ReminderViewModel", "Got settings for priority $priority: ${settings?.soundUri ?: "null"}")
                return@withContext settings ?: ReminderSettings(
                    priority = priority,
                    advanceMinutes = 0,
                    repeatInterval = null,
                    maxReminders = 1,
                    soundUri = ""
                )
            } catch (e: Exception) {
                Log.e("ReminderViewModel", "Error getting settings: ${e.message}", e)
                return@withContext null
            }
        }
    }
}