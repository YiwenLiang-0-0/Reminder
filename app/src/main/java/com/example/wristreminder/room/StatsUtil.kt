package com.example.wristreminder.room

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object StatsUtil {
    // 日期格式
    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    private val weekdayFormat = SimpleDateFormat("EEE", Locale.getDefault())

    // 获取当前日期
    fun getTodayDate(): String {
        return dbDateFormat.format(Calendar.getInstance().time)
    }

    // 获取过去N天的日期列表
    fun getPastDays(days: Int): List<String> {
        val result = mutableListOf<String>()
        val calendar = Calendar.getInstance()

        for (i in 0 until days) {
            result.add(dbDateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }

        return result
    }

    // 将数据库日期格式转为显示格式
    fun formatDateForDisplay(dbDate: String): String {
        return try {
            val date = dbDateFormat.parse(dbDate)
            displayDateFormat.format(date ?: Date())
        } catch (e: Exception) {
            dbDate
        }
    }

    // 获取日期对应的星期几
    fun getWeekday(dbDate: String): String {
        return try {
            val date = dbDateFormat.parse(dbDate)
            weekdayFormat.format(date ?: Date())
        } catch (e: Exception) {
            ""
        }
    }

    // 将优先级数字转为文本
    fun priorityToText(priority: Int): String {
        return when (priority) {
            0 -> "低"
            1 -> "中"
            2 -> "高"
            else -> "未知"
        }
    }

    // 将优先级数字转为颜色
    fun priorityToColor(priority: Int): androidx.compose.ui.graphics.Color {
        return when (priority) {
            0 -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // 绿色
            1 -> androidx.compose.ui.graphics.Color(0xFFFF9800) // 橙色
            2 -> androidx.compose.ui.graphics.Color(0xFFF44336) // 红色
            else -> androidx.compose.ui.graphics.Color.Gray
        }
    }

    // 将完成率转为百分比文本
    fun formatCompletionRate(rate: Float): String {
        return String.format("%.0f%%", rate * 100)
    }
}