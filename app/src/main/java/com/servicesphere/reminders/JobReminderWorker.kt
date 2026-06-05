package com.servicesphere.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.servicesphere.MainActivity
import com.servicesphere.R
import com.servicesphere.data.ServiceLocator
import java.text.DateFormat
import java.util.Date

class JobReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ServiceLocator.init(context.applicationContext)
        val reminderId = inputData.getString(KEY_REMINDER_ID) ?: return Result.failure()
        val reminder = ServiceLocator.jobReminderRepository.getReminderByIdOnce(reminderId) ?: return Result.success()
        if (!reminder.isEnabled || reminder.hasFired) return Result.success()
        val job = ServiceLocator.jobRepository.getJobByIdOnce(reminder.jobId) ?: return Result.success()
        val client = job.clientId?.let { ServiceLocator.clientRepository.getClientByIdOnce(it) }
        val time = job.scheduledAt?.let { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it)) } ?: "scheduled time"
        val body = if (client != null) "${client.name} - $time" else "Scheduled for $time"

        if (canPostNotifications(context)) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_JOB_ID, job.id)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                reminder.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, JOB_REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.servicesphere_icon)
                .setContentTitle("Upcoming job: ${job.title}")
                .setContentText(body)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(reminder.id.hashCode(), notification)
            } catch (_: SecurityException) {
                // Permission can be revoked between scheduling and delivery.
            }
        }

        ServiceLocator.jobReminderRepository.updateReminder(
            reminder.copy(hasFired = true, updatedAt = System.currentTimeMillis())
        )
        return Result.success()
    }

    companion object {
        const val KEY_REMINDER_ID = "reminder_id"
    }
}

private fun canPostNotifications(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
