package com.example.wristreminder.presentation


import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Checkbox
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
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
import com.example.wristreminder.presentation.theme.WristReminderTheme
import com.example.wristreminder.room.Reminder
import com.example.wristreminder.room.ReminderDatabase
import com.example.wristreminder.room.ReminderViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(CalendarScopes.CALENDAR))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        signIn()
        setContent {
            WearApp(ReminderViewModel(ReminderDatabase.getInstance(this).reminderDao()))
        }
    }
    private val RC_SIGN_IN = 9001

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    suspend fun syncWithGoogleCalendar(context: Context) {
        withContext(Dispatchers.IO) {
            val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (googleAccount == null) {
                Log.e("Sync", "Google Account not signed in")
                return@withContext
            }

            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(CalendarScopes.CALENDAR_READONLY)
            )
            credential.selectedAccount = googleAccount.account

            val service = com.google.api.services.calendar.Calendar.Builder(
                NetHttpTransport(), GsonFactory.getDefaultInstance(), credential
            )
                .setApplicationName("Reminder App")
                .build()

            val calendarId = "primary"
            val calendar = Calendar.getInstance()


            calendar.add(Calendar.YEAR, -1)
            val oneYearBefore = DateTime(calendar.time)


            try {
                val events = service.events().list(calendarId)
                    .setTimeMin(oneYearBefore)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute()

                val cloudReminders = events.items.map { event ->
                    val dateTime = event.start.dateTime ?: event.start.date
                    val localDateTime = convertDateTime(dateTime)

                    Reminder(
                        googleEventId = event.id,
                        title = event.summary ?: "No Title",
                        date = localDateTime.first, // yyyy-MM-dd
                        time = localDateTime.second, // HH:mm
                        priority = 1,
                        completed = false,
                        sound = true,
                        updatedAt = event.updated.value
                    )
                }

                val localReminders = ReminderDatabase.getInstance(context).reminderDao().getAllRemindersSync()

                val mergedReminders = mergeReminders(localReminders, cloudReminders)
                mergedReminders.forEach {
                    ReminderDatabase.getInstance(context).reminderDao().insert(it)
                }

                withContext(Dispatchers.Main) {
                    updateAlarmsAfterSync(context, mergedReminders)
                }

            } catch (e: UserRecoverableAuthIOException) {
                Log.e("Sync", "Failed to sync with Google Calendar: ${e.message}")
                withContext(Dispatchers.Main) {
                    startActivityForResult(e.getIntent(), 0)
                }
            }
        }
    }


    private fun convertDateTime(dateTime: DateTime): Pair<String, String> {
        val instant = Instant.ofEpochMilli(dateTime.value)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())

        val date = zonedDateTime.toLocalDate().toString() // yyyy-MM-dd
        val time = if (dateTime.isDateOnly) "00:00" else zonedDateTime.toLocalTime().format(
            DateTimeFormatter.ofPattern("HH:mm"))

        return Pair(date, time)
    }


    private fun mergeReminders(local: List<Reminder>, cloud: List<Reminder>): List<Reminder> {
        val localMap = local.associateBy { it.googleEventId }
        val cloudMap = cloud.associateBy { it.googleEventId }

        val mergedList = mutableListOf<Reminder>()


        for (cloudReminder in cloud) {
            if (!localMap.containsKey(cloudReminder.googleEventId)) {
                mergedList.add(cloudReminder)
            }
        }


        for ((eventId, localReminder) in localMap) {
            val cloudReminder = cloudMap[eventId]
            if (cloudReminder != null) {
                if (cloudReminder.updatedAt > localReminder.updatedAt) {
                    mergedList.add(cloudReminder)
                } else {
                    mergedList.add(localReminder)
                }
            } else {

                mergedList.add(localReminder)
            }
        }

        return mergedList
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    val account = task.getResult(ApiException::class.java)
                    Log.d("GoogleSignIn", "Success: ${account?.email}")
                } catch (e: ApiException) {
                    Log.e("GoogleSignIn", "Failed: ${e.message}")
                }
            }
        }

    }







    private val green = Color(0xFF9DB4AB)
    fun cancelAlarm(context: Context, reminder: Reminder) {
        if (reminder.id >= 0) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            intent.putExtra("title", reminder.title)
            intent.putExtra("sound", reminder.sound)
            intent.putExtra("time", reminder.time)
            val pendingIntent = PendingIntent.getBroadcast(
                context, reminder.id, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            } else {
                Log.d("CancelAlarm", "No pending intent found for reminder id: ${reminder.id}")
            }
        }
    }
    private fun setAlarm(context: Context, calendar: Calendar, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        intent.putExtra("title", reminder.title)
        intent.putExtra("sound", reminder.sound)
        intent.putExtra("time", reminder.time)
        val pendingIntent = PendingIntent.getBroadcast(
            context, reminder.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(context, "Please allow the app to schedule exact alarms in settings.", Toast.LENGTH_SHORT).show()
            return
        }
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
    @Composable
    fun ReminderItem(reminder: Reminder, updateCompletion: (Int, Boolean) -> Unit) {
        var checked by remember { mutableStateOf(reminder.completed) }
        val context = LocalContext.current
        ToggleChip(
            label = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = Util.getPriorityDesc(reminder.priority)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = if(Util.humanizeDate(reminder.date)=="Today") Util.humanizeTime(reminder.time) else reminder.time
                        )
                    }
                    Text(
                        text = reminder.title,
                        textDecoration = if (checked) TextDecoration.LineThrough else null
                    )
                }
            },
            checked = checked,
            colors = ToggleChipDefaults.toggleChipColors(
                uncheckedToggleControlColor = ToggleChipDefaults.SwitchUncheckedIconColor,
                uncheckedStartBackgroundColor = green,
                uncheckedEndBackgroundColor = Color(0xFF658A87),
                checkedStartBackgroundColor = green,
                checkedEndBackgroundColor = green,
            ),
            toggleControl = {
                Column {
                    Text("")
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            checked = !checked
                            if(checked){
                                cancelAlarm(MainApplication.context,reminder)
                            }
                            else{
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
                                setAlarm(MainApplication.context,calendar, reminder)
                            }
                            updateCompletion(reminder.id, checked)
                        }
                    )
                }
            },
            onCheckedChange = {
                val intent = Intent(context, EditActivity::class.java)
                intent.putExtra("id", reminder.id)
                context.startActivity(intent)
            },
            enabled = true,
            modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    fun WearApp(viewModel: ReminderViewModel) {
        val reminders by viewModel.reminders.observeAsState(initial = emptyList())
        var r by remember { mutableStateOf(listOf<Reminder>()) }
        val context = LocalContext.current
        LaunchedEffect(reminders) {
            var previousDate = ""
            r = listOf()
            for(i in reminders.indices){
                if(previousDate!=reminders[i].date){
                    r = r.plus(Reminder(id = -2, date = reminders[i].date, time = "00:00", title = "Blank", priority = 0, completed = false, sound = false))
                    previousDate = reminders[i].date
                }
                r = r.plus(reminders[i])

            }
        }
        WristReminderTheme {
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
                        PositionIndicator(
                            scalingLazyListState = rememberScalingLazyListState()
                        )
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
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        content = {
                                            Image(
                                                painter = painterResource(id = R.drawable.baseline_add_24),
                                                contentDescription = "Add"
                                            )
                                        },
                                        onClick = {
                                            val intent = Intent(context, EditActivity::class.java)
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(backgroundColor = green)
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Button(
                                        content = {
                                            Icon(Icons.Default.Send, null, tint = Color.White)

                                        },
                                        onClick = {
                                            lifecycleScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    syncTasksManually(this@MainActivity)
                                                }
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Sync",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = Color(
                                                0xFF008B8B
                                            )
                                        )
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Button(
                                        content = {
                                            Icon(Icons.Default.Info, contentDescription = "Stats", tint = Color.White)
                                        },
                                        onClick = {
                                            val intent = Intent(context, StatsActivity::class.java)
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF8B658B))
                                    )
                                }


                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    "Upcoming Event",
                                    textAlign = TextAlign.Center,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        if (reminders.isEmpty()) {
                            item {
                                Column {
                                    Text(
                                        "No Upcoming Event",
                                        textAlign = TextAlign.Center,
                                        color = Color.Gray,
                                        fontSize = 15.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {

                            items(
                                items = r
                            ) { reminder ->
                                Column {
                                    if (reminder.id < 0) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            Util.humanizeDate(reminder.date),
                                            textAlign = TextAlign.Center,
                                            color = Color.LightGray,
                                            fontSize = 15.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ReminderItem(
                                            reminder,
                                            updateCompletion = { reminderId, isCompleted ->
                                                viewModel.updateCompletion(reminderId, isCompleted)

                                            })
                                    }

                                }
                            }
                        }


                    }
                }
            }
        }
    }
    fun syncTasksManually(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            syncWithGoogleCalendar(context)
        }
    }

    private fun updateAlarmsAfterSync(context: Context, reminders: List<Reminder>) {
        for (reminder in reminders) {
            cancelAlarm(context, reminder)
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
                setAlarm(context, calendar, reminder)
            }
        }
    }


}
