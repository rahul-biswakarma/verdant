package com.verdant.work.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HabitTimerService : Service() {
    private var timerJob: Job? = null
    private var elapsedSeconds = 0
    private var habitId: String = ""
    private var habitName: String = ""
    private var isPaused = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return START_NOT_STICKY
                habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: "Timer"
                startTimer()
            }
            ACTION_PAUSE -> {
                isPaused = !isPaused
                updateNotification()
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer() {
        val channelId = "verdant_timer"
        val channel = NotificationChannel(
            channelId,
            "Habit Timer",
            NotificationManager.IMPORTANCE_LOW,
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)

        startForeground(NOTIFICATION_ID, buildNotification(channelId))

        timerJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(1000)
                if (!isPaused) {
                    elapsedSeconds++
                    updateNotification()
                }
            }
        }
    }

    private fun buildNotification(channelId: String): Notification {
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)
        val statusText = if (isPaused) "Paused" else "Running"

        val pauseIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, HabitTimerService::class.java).apply { action = ACTION_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, HabitTimerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("$habitName — $timeText")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .addAction(0, if (isPaused) "Resume" else "Pause", pauseIntent)
            .addAction(0, "Stop & Log", stopIntent)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification("verdant_timer"))
    }

    override fun onDestroy() {
        timerJob?.cancel()
        // Broadcast the result so the app can log it
        sendBroadcast(
            Intent(ACTION_TIMER_RESULT).apply {
                putExtra(EXTRA_HABIT_ID, habitId)
                putExtra(EXTRA_ELAPSED_SECONDS, elapsedSeconds)
                setPackage(packageName)
            },
        )
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.verdant.timer.START"
        const val ACTION_PAUSE = "com.verdant.timer.PAUSE"
        const val ACTION_STOP = "com.verdant.timer.STOP"
        const val ACTION_TIMER_RESULT = "com.verdant.timer.RESULT"
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_HABIT_NAME = "habit_name"
        const val EXTRA_ELAPSED_SECONDS = "elapsed_seconds"
        const val NOTIFICATION_ID = 9001

        fun startIntent(context: Context, habitId: String, habitName: String): Intent =
            Intent(context, HabitTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_HABIT_ID, habitId)
                putExtra(EXTRA_HABIT_NAME, habitName)
            }
    }
}
