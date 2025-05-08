package com.example.wristreminder.presentation

import android.app.Activity
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.example.wristreminder.AlarmReceiver
import com.example.wristreminder.R
import com.example.wristreminder.Util
import com.example.wristreminder.application.MainApplication
import com.example.wristreminder.presentation.ui.theme.WristReminderTheme
import com.example.wristreminder.room.Reminder
import com.example.wristreminder.room.ReminderDatabase
import com.example.wristreminder.room.ReminderSettings
import com.example.wristreminder.room.ReminderViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

class EditActivity : ComponentActivity() {
    private val orange = 0xB7FFC107
    private val blue = 0xFF2175F3
    private val green = 0xFF9DB4AB
    private lateinit var viewModel: ReminderViewModel
    
    // 替换旧的 SETTINGS_REQUEST_CODE 常量
    // 使用 registerForActivityResult 创建一个活动结果启动器
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            data?.let {
                val priority = it.getIntExtra("priority", 1)
                val advanceMinutes = it.getIntExtra("advanceMinutes", 0)
                val repeatInterval = if (it.hasExtra("repeatInterval")) 
                    it.getIntExtra("repeatInterval", 15) else null
                val maxReminders = it.getIntExtra("maxReminders", 1)
                val soundUri = it.getStringExtra("soundUri") ?: ""
                
                Log.d("EditActivity", "Received settings from picker - Priority: $priority, " +
                        "Sound URI: $soundUri")
                
                val settings = ReminderSettings(
                    priority = priority,
                    advanceMinutes = advanceMinutes,
                    repeatInterval = repeatInterval,
                    maxReminders = maxReminders,
                    soundUri = soundUri
                )
                
                viewModel.updateReminderSettings(settings)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 初始化 viewModel
        viewModel = ReminderViewModel(ReminderDatabase.getInstance(this).reminderDao())

        setContent {
            val reminders by viewModel.reminders.observeAsState(initial = emptyList())

            WristReminderTheme {
                val context = LocalContext.current
                // Get Data from Intent

                var id = intent.getIntExtra("id", -1)
                // State variables for each section
                var initialReminder:Reminder? =  null
                var selectedTime by remember { mutableStateOf("") }
                var ifCompleted by remember { mutableStateOf(false) }
                var selectedDate by remember { mutableStateOf("") }
                var selectedPriority by remember { mutableIntStateOf(1) }
                var playSound by remember { mutableStateOf(false) }
                var inputText by remember { mutableStateOf("") }



                LaunchedEffect(reminders) {
                    if(id>=0 && reminders.isNotEmpty()){
                        val r = reminders.find { it.id == id }
                        r?.run {
                            selectedTime = time
                            selectedDate = date
                            inputText = title
                            playSound = sound
                            ifCompleted = completed
                            selectedPriority = priority
                        }
                        initialReminder = r
                    }
                }

                // Dialog display state
                var showTimeDialog by remember { mutableStateOf(false) }

                // Function to collect all reminder data into a list
                fun getReminder(): Reminder {
                    return Reminder(time = selectedTime, date = selectedDate, title =  inputText, priority = selectedPriority, completed = ifCompleted, sound = playSound, googleEventId = initialReminder?.googleEventId ?: "", updatedAt = System.currentTimeMillis())
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF58788A))
                )
                {
                    Scaffold(
                        timeText = { TimeText() },
                        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
                        positionIndicator = {
                            PositionIndicator(scalingLazyListState = rememberScalingLazyListState())
                        }
                    ) {
                        ScalingLazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            state = rememberScalingLazyListState(),
                            contentPadding = PaddingValues(
                                top = 28.dp,
                                start = 10.dp,
                                end = 10.dp,
                                bottom = 40.dp
                            )
                        ) {
                            item {
                                Text(
                                    "Events Reminder",
                                    textAlign = TextAlign.Center,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            // Time selection Chip
                            item {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Chip(
                                        onClick = { showTimeDialog = true },
                                        label = { Text(selectedTime.ifBlank { "Select Time" }) },
                                        icon = {
                                            Image(
                                                painter = painterResource(id = R.drawable.baseline_alarm_24),
                                                contentDescription = "Alarm"
                                            )
                                        },
                                        colors = ChipDefaults.primaryChipColors(
                                            backgroundColor = Color(0xCDB1D39F)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (showTimeDialog) {
                                    TimePickerComp(
                                        showDialog = showTimeDialog,
                                        onDismiss = { showTimeDialog = false },
                                        onTimeSelected = { hour, minute ->
                                            selectedTime = String.format("%02d:%02d", hour, minute)
                                            showTimeDialog = false
                                        },
                                        selectedTime = selectedTime
                                    )
                                }
                            }
                            // Date selection Chip
                            item {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Chip(
                                        onClick = {
                                            // Open date picker dialog to return a date in mm/dd/yy format
                                            showDatePicker(context, { date ->
                                                selectedDate = date
                                            }, selectedDate)
                                        },
                                        label = { Text(selectedDate.ifBlank { "Select Date" }) },
                                        icon = {
                                            Image(
                                                painter = painterResource(id = R.drawable.baseline_calendar_today_24),
                                                contentDescription = "Calendar"
                                            )
                                        },
                                        colors = ChipDefaults.primaryChipColors(
                                            backgroundColor = Color(0xDAB4A57B)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            // Sound selection Chip
                            item {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    ToggleChip(
                                        label = {
                                            Text("Sound")
                                        },
                                        checked = playSound,
                                        colors = ToggleChipDefaults.toggleChipColors(
                                            uncheckedToggleControlColor = ToggleChipDefaults.SwitchUncheckedIconColor,
                                            uncheckedStartBackgroundColor = Color(0xFF658A87),
                                            uncheckedEndBackgroundColor = Color(0xABFFC107),
                                            checkedStartBackgroundColor = Color(0xE1009688),
                                            checkedEndBackgroundColor = Color(0xFF658A87),
                                        ),
                                        appIcon = {
                                            Image(
                                                painter = painterResource(id = R.drawable.baseline_volume_up_24),
                                                contentDescription = "Sound"
                                            )
                                        },
                                        toggleControl = {
                                            Column {
                                                Switch(
                                                    checked = playSound
                                                )
                                            }
                                        },
                                        onCheckedChange = {
                                            playSound = !playSound
                                        },
                                        enabled = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                            }
                            // Priority Chip
                            item {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Chip(
                                        onClick = {
                                            selectedPriority = when (selectedPriority) {
                                                1 -> 2
                                                2 -> 0
                                                else -> 1
                                            }
                                        },
                                        label = {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally

                                            ) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = Util.getPriorityDesc(selectedPriority),
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        },
                                        icon = {
                                            when (selectedPriority) {
                                                0 -> Image(
                                                    painter = painterResource(id = R.drawable.baseline_low_priority_24),
                                                    contentDescription = Util.getPriorityDesc(
                                                        selectedPriority
                                                    )
                                                )

                                                2 -> Image(
                                                    painter = painterResource(id = R.drawable.baseline_priority_high_24),
                                                    contentDescription = Util.getPriorityDesc(
                                                        selectedPriority
                                                    )
                                                )

                                                else -> Image(
                                                    painter = painterResource(id = R.drawable.baseline_assignment_24),
                                                    contentDescription = Util.getPriorityDesc(
                                                        selectedPriority
                                                    )
                                                )
                                            }
                                        },
                                        colors = ChipDefaults.primaryChipColors(
                                            backgroundColor = Color(0xB597C58E)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            // Reminder Settings Chip
                            item {
                                var showSettingsDialog by remember { mutableStateOf(false) }
                                val settings by viewModel.getReminderSettings(selectedPriority).observeAsState()

                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Chip(
                                        onClick = { 
                                            // Use the nullable settings object safely
                                            val intent = Intent(context, ReminderSettingsActivity::class.java).apply {
                                                settings?.let {
                                                    putExtra("priority", it.priority)
                                                    putExtra("advanceMinutes", it.advanceMinutes)
                                                    if (it.repeatInterval != null) {
                                                        putExtra("repeatInterval", it.repeatInterval)
                                                    }
                                                    putExtra("maxReminders", it.maxReminders)
                                                    putExtra("soundUri", it.soundUri)
                                                } ?: run {
                                                    putExtra("priority", selectedPriority)
                                                }
                                            }
                                            settingsLauncher.launch(intent)
                                        },
                                        label = {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = buildReminderSettingsDescription(settings),
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        },
                                        icon = {
                                            Image(
                                                painter = painterResource(id = R.drawable.baseline_alarm_24),
                                                contentDescription = "Reminder Settings"
                                            )
                                        },
                                        colors = ChipDefaults.primaryChipColors(
                                            backgroundColor = Color(0xB597C58E)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            // Note input Chip
                            item {
                                var text by remember { mutableStateOf(inputText) }
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Chip(
                                        onClick = {
                                            (context as EditActivity).showInputMethod(
                                                context,
                                                text
                                            ) { newText ->
                                                text = newText
                                                inputText = newText
                                            }
                                        },
                                        label = {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally

                                            ) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text.ifBlank { "Enter Note" },
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        },
                                        colors = ChipDefaults.primaryChipColors(
                                            backgroundColor = Color(
                                                green
                                            )
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            // Button section
                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (id >= 0) {
                                        Button(
                                            onClick = {
                                                viewModel.deleteReminder(getReminder().copy(id = id))
                                                cancelAlarm(
                                                    MainApplication.context,
                                                    initialReminder!!
                                                )
                                                finish()
                                            },
                                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray)
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.baseline_delete_24),
                                                contentDescription = "Delete"
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    Button(
                                        onClick = {
                                            // Collect all data for saving
                                            if (selectedTime.isBlank()) {
                                                Toast.makeText(
                                                    context,
                                                    "Please select a time",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else if (selectedDate.isBlank()) {
                                                Toast.makeText(
                                                    context,
                                                    "Please select a date",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else if (inputText.isBlank()) {
                                                Toast.makeText(
                                                    context,
                                                    "Please enter a note",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                val reminder = getReminder()
                                                if (id < 0) {
                                                    // 新增提醒
                                                    viewModel.addReminder(reminder)
                                                    // 观察 lastId 变化
                                                    viewModel.observeLastId().observe(this@EditActivity) { newId ->
                                                        if (newId > 0) {
                                                            scheduleAlarm(reminder.copy(id = newId))
                                                            finish()
                                                        }
                                                    }
                                                } else {
                                                    // 更新提醒
                                                    viewModel.updateReminder(reminder.copy(id = id))
                                                    cancelAlarm(MainApplication.context, initialReminder!!)
                                                    if (!reminder.completed) {
                                                        scheduleAlarm(reminder.copy(id = id))
                                                    }
                                                    finish()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = Color(
                                                blue
                                            )
                                        )
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.baseline_check_24),
                                            contentDescription = "Confirm"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setAlarm(context: Context, calendar: Calendar, reminder: Reminder) {
        // 使用 lifecycleScope 而不是 viewModelScope
        lifecycleScope.launch {
            try {
                // 在协程中调用 suspend 函数
                val settings = viewModel.getReminderSettingsSync(reminder.priority)
                
                // 现在可以安全地使用 withContext
                withContext(Dispatchers.Main) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("title", reminder.title)
                        putExtra("sound", reminder.sound)
                        putExtra("time", reminder.time)
                        putExtra("reminderId", reminder.id)
                        putExtra("priority", reminder.priority)
                        putExtra("repeatInterval", settings?.repeatInterval)
                        putExtra("maxReminders", settings?.maxReminders)
                        putExtra("currentReminderCount", 1)
                        putExtra("soundUri", settings?.soundUri ?: "")
                    }

                    // 创建日历实例的副本，以避免修改原始实例
                    val adjustedCalendar = Calendar.getInstance()
                    adjustedCalendar.timeInMillis = calendar.timeInMillis
                    
                    // 如果设置了提前提醒，调整提醒时间
                    if (settings?.advanceMinutes != null && settings.advanceMinutes > 0) {
                        Log.d("Alarm", "Setting advance reminder: ${settings.advanceMinutes} minutes")
                        adjustedCalendar.add(Calendar.MINUTE, -settings.advanceMinutes)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context, reminder.id, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        Toast.makeText(context, "Please allow the app to schedule exact alarms in settings.", Toast.LENGTH_SHORT).show()
                        return@withContext
                    }

                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        adjustedCalendar.timeInMillis,
                        pendingIntent
                    )
                    
                    // 添加日志以便调试
                    Log.d("Alarm", "Alarm set for ${reminder.title} at ${adjustedCalendar.time}, advance: ${settings?.advanceMinutes ?: 0} min")
                }
            } catch (e: Exception) {
                Log.e("Alarm", "Error setting alarm: ${e.message}", e)
                // 在出错时显示一个 Toast
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Failed to set alarm: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    private fun showInputMethod(context: Context, init:String, onInputComplete: (String) -> Unit) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val editText = android.widget.EditText(context).apply {
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onInputComplete(text.toString())
                    imm.hideSoftInputFromWindow(windowToken, 0)
                    (v.parent as android.view.ViewGroup).removeView(v)
                    true
                } else {
                    false
                }
            }
        }
        editText.setText(init)
        addContentView(
            editText,
            android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        editText.requestFocus()
        imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED)
    }
    fun showDatePicker(context: Context, onDateSelected: (String) -> Unit, selectedDate: String?) {
        val calendar = Calendar.getInstance()

        if (!selectedDate.isNullOrBlank()) {
            val dateParts = selectedDate.split("-")
            if (dateParts.size == 3) {
                calendar.set(Calendar.YEAR, dateParts[0].toInt())
                calendar.set(Calendar.MONTH, dateParts[1].toInt() - 1)
                calendar.set(Calendar.DAY_OF_MONTH, dateParts[2].toInt())
            }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                onDateSelected(formattedDate)
            },
            year, month, day
        ).show()
    }

    fun cancelAlarm(context: Context, reminder: Reminder) {
        if (reminder.id >= 0) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            intent.putExtra("title", reminder.title)
            intent.putExtra("sound", reminder.sound)
            intent.putExtra("time", reminder.time)
            val pendingIntent = PendingIntent.getBroadcast(
                context, reminder.id, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            } else {
                Log.d("CancelAlarm", "No pending intent found for reminder id: ${reminder.id}")
            }
        }
    }

    @Composable
    fun TimePickerComp(
        showDialog: Boolean,
        onDismiss: () -> Unit,
        onTimeSelected: (Int, Int) -> Unit,
        selectedTime: String?
    ) {
        val context = LocalContext.current

        if (showDialog) {
            val calendar = Calendar.getInstance()

            if (!selectedTime.isNullOrBlank()) {
                val timeParts = selectedTime.split(":")
                if (timeParts.size == 2) {
                    calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    calendar.set(Calendar.MINUTE, timeParts[1].toInt())
                }
            }

            val initialHour = calendar.get(Calendar.HOUR_OF_DAY)
            val initialMinute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = remember {
                TimePickerDialog(
                    context,
                    { _: TimePicker, hour: Int, minute: Int ->
                        onTimeSelected(hour, minute)
                        onDismiss()
                    },
                    initialHour,
                    initialMinute,
                    true
                ).apply {
                    setOnCancelListener { onDismiss() }
                }
            }

            timePickerDialog.show()
        }
    }

    @Composable
    fun ReminderSettingsDialog(
        currentSettings: ReminderSettings?,
        onDismiss: () -> Unit,
        onConfirm: (ReminderSettings) -> Unit
    ) {
        var advanceMinutes by remember { mutableStateOf(currentSettings?.advanceMinutes ?: 0) }
        var repeatInterval by remember { mutableStateOf(currentSettings?.repeatInterval) }
        var maxReminders by remember { mutableStateOf(currentSettings?.maxReminders ?: 1) }

        // 使用全新的对话框布局
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
                // 标题
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
                
                // 提前提醒设置
                item {
                    ToggleChip(
                        checked = advanceMinutes > 0,
                        onCheckedChange = { checked ->
                            advanceMinutes = if (checked) 5 else 0
                        },
                        label = { Text("Advance reminder") },
                        toggleControl = {
                            Switch(checked = advanceMinutes > 0)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (advanceMinutes > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Chip(
                            onClick = {
                                advanceMinutes = when (advanceMinutes) {
                                    5 -> 10
                                    10 -> 15
                                    15 -> 30
                                    30 -> 5
                                    else -> 5
                                }
                            },
                            label = { Text("${advanceMinutes}min before") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // 重复提醒设置
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    ToggleChip(
                        checked = repeatInterval != null,
                        onCheckedChange = { checked ->
                            repeatInterval = if (checked) 15 else null
                            if (!checked) maxReminders = 1
                        },
                        label = { Text("Repeat reminder") },
                        toggleControl = {
                            Switch(checked = repeatInterval != null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (repeatInterval != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Chip(
                            onClick = {
                                repeatInterval = when (repeatInterval) {
                                    5 -> 10
                                    10 -> 15
                                    15 -> 30
                                    30 -> 5
                                    else -> 15
                                }
                            },
                            label = { Text("Every ${repeatInterval}min") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Chip(
                            onClick = {
                                maxReminders = when (maxReminders) {
                                    1 -> 2
                                    2 -> 3
                                    3 -> 5
                                    5 -> 1
                                    else -> 2
                                }
                            },
                            label = { Text("Repeat $maxReminders times") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // 按钮
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray)
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                onConfirm(
                                    ReminderSettings(
                                        priority = currentSettings?.priority ?: 1,
                                        advanceMinutes = advanceMinutes,
                                        repeatInterval = repeatInterval,
                                        maxReminders = maxReminders
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(blue))
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }

    private fun buildReminderSettingsDescription(settings: ReminderSettings?): String {
        if (settings == null) return "Configure Reminder"
        
        val parts = mutableListOf<String>()
        
        if (settings.advanceMinutes > 0) {
            parts.add("${settings.advanceMinutes}min before")
        }
        
        if (settings.repeatInterval != null) {
            parts.add("Repeat every ${settings.repeatInterval}min")
            if (settings.maxReminders > 1) {
                parts.add("${settings.maxReminders} times")
            }
        }
        
        if (settings.soundUri.isNotEmpty()) {
            parts.add("Custom sound")
        }
        
        return if (parts.isEmpty()) {
            "On time"
        } else {
            parts.joinToString(" | ")
        }
    }

    // 辅助方法来安排闹钟
    private fun scheduleAlarm(reminder: Reminder) {
        if (!reminder.completed) {
            val dateEntity = LocalDate.parse(reminder.date)
            val timeEntity = LocalTime.parse(reminder.time)
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, dateEntity.year)
                set(Calendar.MONTH, dateEntity.monthValue - 1)
                set(Calendar.DAY_OF_MONTH, dateEntity.dayOfMonth)
                set(Calendar.HOUR_OF_DAY, timeEntity.hour)
                set(Calendar.MINUTE, timeEntity.minute)
                set(Calendar.SECOND, timeEntity.second)
            }
            setAlarm(MainApplication.context, calendar, reminder)
        }
    }

}



