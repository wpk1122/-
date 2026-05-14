package com.chrona.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.chrona.ai.data.AppDatabase
import com.chrona.ai.data.ScheduleRepository
import com.chrona.ai.parser.RuleBasedTaskParser
import com.chrona.ai.reminder.WorkManagerReminderScheduler
import com.chrona.ai.ui.ChronaApp
import com.chrona.ai.ui.ChronaTheme
import java.time.Clock

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current.applicationContext
            val parser = remember { RuleBasedTaskParser() }
            val repository = remember {
                val dao = AppDatabase.get(context).scheduleTaskDao()
                ScheduleRepository(
                    dao = dao,
                    reminderScheduler = WorkManagerReminderScheduler(context),
                    clock = Clock.systemDefaultZone()
                )
            }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { }
            )

            LaunchedEffect(Unit) {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            ChronaTheme {
                ChronaApp(
                    parser = parser,
                    repository = repository
                )
            }
        }
    }
}
