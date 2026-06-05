package com.servicesphere.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.servicesphere.data.local.JobReminderEntity
import java.util.concurrent.TimeUnit

const val JOB_REMINDER_CHANNEL_ID = "job_reminders"

class JobReminderScheduler(private val context: Context) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                JOB_REMINDER_CHANNEL_ID,
                "Job Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for scheduled ServiceSphere jobs"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun schedule(reminder: JobReminderEntity) {
        if (!reminder.isEnabled || reminder.hasFired) return
        ensureNotificationChannel()
        val delay = (reminder.reminderTimeMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<JobReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(JobReminderWorker.KEY_REMINDER_ID to reminder.id))
            .addTag(JOB_REMINDER_WORK_TAG)
            .build()
        workManager.enqueueUniqueWork(workName(reminder.id), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(reminderId: String) {
        workManager.cancelUniqueWork(workName(reminderId))
    }

    fun cancelAllReminders() {
        workManager.cancelAllWorkByTag(JOB_REMINDER_WORK_TAG)
    }

    private fun workName(reminderId: String): String = "job_reminder_$reminderId"
}

const val JOB_REMINDER_WORK_TAG = "job_reminder_work"
