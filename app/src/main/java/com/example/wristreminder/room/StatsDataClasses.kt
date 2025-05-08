package com.example.wristreminder.room

import androidx.room.ColumnInfo

// 日期计数类 - 用于按日期查询统计结果
data class DateCount(
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "count") val count: Int
)

// 优先级计数类 - 用于按优先级查询统计结果
data class PriorityCount(
    @ColumnInfo(name = "priority") val priority: Int,
    @ColumnInfo(name = "count") val count: Int
)

// 用于UI显示的统计摘要数据类
data class StatsSummary(
    val totalTasks: Int,
    val completedTasks: Int,
    val pendingTasks: Int,
    val completionRate: Float,
    val dailyStats: List<DateCount>,
    val priorityStats: List<PriorityCount>
)