package com.example.wristreminder.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wristreminder.room.DateCount
import com.example.wristreminder.room.PriorityCount
import com.example.wristreminder.room.ReminderDatabase
import com.example.wristreminder.room.ReminderViewModel
import com.example.wristreminder.room.StatsSummary
import com.example.wristreminder.room.StatsUtil
import com.example.wristreminder.presentation.ui.theme.WristReminderTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StatsActivity : ComponentActivity() {
    private lateinit var viewModel: ReminderViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化 ViewModel
        val dao = ReminderDatabase.getInstance(this).reminderDao()
        viewModel = ReminderViewModel(dao)

        setContent {
            WristReminderTheme {
                StatsScreen(viewModel) {
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: ReminderViewModel, onBackPressed: () -> Unit) {
    val statsSummary by viewModel.statsSummary.observeAsState(
        StatsSummary(0, 0, 0, 0f, emptyList(), emptyList())
    )
    val todayCompleted by viewModel.todayCompletedCount.observeAsState(0)
    val todayTotal by viewModel.todayTotalCount.observeAsState(0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stats of Event") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Return"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = Color(0xFF9DB4AB)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            state = rememberScrollState(),
                            enabled = true
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 今日完成情况
                    TodaySummaryCard(
                        completed = todayCompleted,
                        total = todayTotal
                    )

                    // 总体完成情况
                    OverallCompletionCard(
                        completed = statsSummary.completedTasks,
                        total = statsSummary.totalTasks,
                        rate = statsSummary.completionRate
                    )

                    // 过去7天完成情况
                    WeeklyStatsCard(statsSummary.dailyStats)

                    // 按优先级统计
                    PriorityStatsCard(statsSummary.priorityStats)
                }
            }
        }
    }
}

@Composable
fun TodaySummaryCard(completed: Int, total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题部分
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Today's event",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 数据显示部分
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 完成数量
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = completed.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Completed",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // 总数
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = total.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // 完成率
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    val rate = if (total > 0) completed.toFloat() / total else 0f
                    Text(
                        text = StatsUtil.formatCompletionRate(rate),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (rate >= 0.8f) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rate",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun OverallCompletionCard(completed: Int, total: Int, rate: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Overall",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 进度圆环
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = rate,
                    modifier = Modifier.size(120.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 12.dp
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Complete",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = StatsUtil.formatCompletionRate(rate),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 统计信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = completed.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Completed",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = total.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyStatsCard(dailyStats: List<DateCount>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "Past 7 days",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 生成过去7天的日期列表
            val last7Days = (0..6).map { daysAgo ->
                LocalDate.now().minusDays(daysAgo.toLong())
            }.reversed()

            // 日期行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                last7Days.forEach { date ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(18.dp)  // 进一步减小宽度
                    ) {
                        // 日期
                        Text(
                            text = date.dayOfMonth.toString(),
                            fontSize = 10.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        
                        // 星期 - 只显示首字母
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("E")).first().toString(),
                            fontSize = 10.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 获取当天的统计数据
                        val dayStats = dailyStats.find { 
                            LocalDate.parse(it.date).isEqual(date)
                        }
                        val count = dayStats?.count ?: 0

                        // 柱状图
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height((count * 6).coerceAtLeast(3).dp)
                                .background(
                                    when {
                                        count >= 5 -> Color(0xFF4CAF50)
                                        count >= 3 -> Color(0xFFFF9800)
                                        count > 0 -> Color(0xFFF44336)
                                        else -> Color.LightGray
                                    },
                                    RoundedCornerShape(2.dp)
                                )
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // 数量
                        Text(
                            text = count.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityStatsCard(priorityStats: List<PriorityCount>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Statistics by priority",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 处理优先级数据
            val priorityMap = priorityStats.associate { it.priority to it.count }
            val highCount = priorityMap[2] ?: 0
            val mediumCount = priorityMap[1] ?: 0
            val lowCount = priorityMap[0] ?: 0
            val totalCount = highCount + mediumCount + lowCount

            // 优先级分布柱状图
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                PriorityBar(
                    label = "Urgent",
                    count = highCount,
                    totalCount = totalCount,
                    color = Color(0xC1FFBF00)
                )

                Spacer(modifier = Modifier.height(8.dp))

                PriorityBar(
                    label = "Normal",
                    count = mediumCount,
                    totalCount = totalCount,
                    color = Color(0xB721C2F3)
                )

                Spacer(modifier = Modifier.height(8.dp))

                PriorityBar(
                    label = "Low",
                    count = lowCount,
                    totalCount = totalCount,
                    color = Color(0xC46EC34A)
                )
            }
        }
    }
}

@Composable
fun PriorityBar(label: String, count: Int, totalCount: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = label,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = count.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 进度条，确保权重始终大于0
        val progress = if (totalCount > 0) count.toFloat() / totalCount else 0f
        // 安全的权重值，避免为0
        val safeProgress = if (progress <= 0f) 0.01f else progress
        val remainingWeight = if (progress >= 1f) 0.01f else (1f - progress)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(safeProgress)
                    .height(8.dp)
                    .background(color, RoundedCornerShape(4.dp))
            )

            Box(
                modifier = Modifier
                    .weight(remainingWeight)
                    .height(8.dp)
                    .background(Color.LightGray, RoundedCornerShape(4.dp))
            )
        }
    }
}

// 修改 StatsUtil 类，添加日期处理方法
object StatsUtil {
    // ... 其他方法

    fun formatDateForDisplay(dateStr: String): String {
        return try {
            val date = LocalDate.parse(dateStr)
            date.format(DateTimeFormatter.ofPattern("MM/dd"))
        } catch (e: Exception) {
            ""
        }
    }

    fun getWeekday(dateStr: String): String {
        return try {
            val date = LocalDate.parse(dateStr)
            date.format(DateTimeFormatter.ofPattern("EEE"))
        } catch (e: Exception) {
            ""
        }
    }
}