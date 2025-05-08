package com.example.wristreminder.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.example.wristreminder.Util
import com.example.wristreminder.presentation.ui.theme.WristReminderTheme


class AlarmActivity : ComponentActivity() {
    val green = Color(0xB5ACC78B)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WristReminderTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF58788A))
                ) {
                    Scaffold(timeText = {
                        Column {
                            Spacer(modifier = Modifier.height(15.dp))
                            Text(
                                Util.humanizeTime(intent.getStringExtra("time")!!),
                                textAlign = TextAlign.Center,
                                color = green,
                                fontSize = 15.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                    }, vignette = {
                        Vignette(vignettePosition = VignettePosition.TopAndBottom)
                    }, positionIndicator = {
                        PositionIndicator(
                            scalingLazyListState = rememberScalingLazyListState()
                        )
                    }

                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(60.dp))
                            Chip(
                                onClick = {
                                    finish()
                                    startActivity(
                                        Intent(
                                            this@AlarmActivity,
                                            MainActivity::class.java
                                        )
                                    )
                                },
                                label = {
                                    Text(
                                        intent.getStringExtra("time")!!,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                },
                                colors = ChipDefaults.primaryChipColors(
                                    backgroundColor = green
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 10.dp, end = 10.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                intent.getStringExtra("title")!!,
                                fontSize = 25.sp,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = Color.Black,
                                        blurRadius = 8f,
                                        offset = Offset(4f, 4f)
                                    ), textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                        }
                    }
                }
            }
        }
    }
}
