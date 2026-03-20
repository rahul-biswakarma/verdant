package com.verdant.work.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.verdant.core.database.dao.HabitDao
import com.verdant.core.database.entity.toDomain
import com.verdant.work.scheduler.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Listens for [Intent.ACTION_BOOT_COMPLETED] (and `QUICKBOOT_POWERON` for some OEMs)
 * and re-registers all habit reminder alarms that were cleared by the device reboot.
 *
 * AlarmManager alarms do not survive a device reboot, so this receiver is required
 * to restore the full reminder schedule when the device starts back up.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var habitDao: HabitDao
    @Inject lateinit var scheduler: ReminderScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != ACTION_QUICKBOOT) return

        scope.launch {
            runCatching {
                val habits = habitDao.getAll()
                    .filter { !it.isArchived }
                    .map { it.toDomain() }
                scheduler.rescheduleAll(habits)
            }
        }
    }

    companion object {
        /** Some HTC / OnePlus devices fire this instead of ACTION_BOOT_COMPLETED. */
        private const val ACTION_QUICKBOOT = "android.intent.action.QUICKBOOT_POWERON"
    }
}
