package com.servicesphere.reminders

object ReminderTypes {
    const val NONE = "NONE"
    const val AT_TIME = "AT_TIME"
    const val FIFTEEN_MINUTES_BEFORE = "15_MINUTES_BEFORE"
    const val THIRTY_MINUTES_BEFORE = "30_MINUTES_BEFORE"
    const val ONE_HOUR_BEFORE = "1_HOUR_BEFORE"
    const val ONE_DAY_BEFORE = "1_DAY_BEFORE"
    const val CUSTOM = "CUSTOM"

    val formOptions = listOf(
        NONE,
        AT_TIME,
        FIFTEEN_MINUTES_BEFORE,
        THIRTY_MINUTES_BEFORE,
        ONE_HOUR_BEFORE,
        ONE_DAY_BEFORE
    )

    fun label(type: String): String = when (type) {
        NONE -> "No reminder"
        AT_TIME -> "At scheduled time"
        FIFTEEN_MINUTES_BEFORE -> "15 minutes before"
        THIRTY_MINUTES_BEFORE -> "30 minutes before"
        ONE_HOUR_BEFORE -> "1 hour before"
        ONE_DAY_BEFORE -> "1 day before"
        CUSTOM -> "Custom"
        else -> "No reminder"
    }
}
