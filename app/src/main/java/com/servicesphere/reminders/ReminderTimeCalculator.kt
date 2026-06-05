package com.servicesphere.reminders

object ReminderTimeCalculator {
    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    fun calculate(scheduledAt: Long, reminderType: String, customTimeMillis: Long? = null): Long = when (reminderType) {
        ReminderTypes.AT_TIME -> scheduledAt
        ReminderTypes.FIFTEEN_MINUTES_BEFORE -> scheduledAt - 15 * MINUTE
        ReminderTypes.THIRTY_MINUTES_BEFORE -> scheduledAt - 30 * MINUTE
        ReminderTypes.ONE_HOUR_BEFORE -> scheduledAt - HOUR
        ReminderTypes.ONE_DAY_BEFORE -> scheduledAt - DAY
        ReminderTypes.CUSTOM -> customTimeMillis ?: scheduledAt
        else -> scheduledAt
    }
}
