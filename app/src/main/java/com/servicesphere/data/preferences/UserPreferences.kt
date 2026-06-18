package com.servicesphere.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.servicesphere.reminders.ReminderTypes

private val Context.dataStore by preferencesDataStore("servicesphere_preferences")

class UserPreferences(context: Context) {
    private val dataStore = context.dataStore
    private val onboardingCompleteKey = booleanPreferencesKey("onboarding_complete")
    private val businessSetupCompleteKey = booleanPreferencesKey("business_setup_complete")
    private val selectedTradeTypeKey = stringPreferencesKey("selected_trade_type")
    private val onboardingCompletedAtKey = longPreferencesKey("onboarding_completed_at")
    private val businessSetupCompletedAtKey = longPreferencesKey("business_setup_completed_at")
    private val walkthroughSeenKey = booleanPreferencesKey("walkthrough_seen")
    private val debugProEnabledKey = booleanPreferencesKey("debug_is_pro_enabled")
    private val defaultJobReminderTypeKey = stringPreferencesKey("default_job_reminder_type")
    private val autoDisableCompletedJobRemindersKey = booleanPreferencesKey("auto_disable_completed_job_reminders")

    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[onboardingCompleteKey] ?: false
    }

    val onboardingComplete: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[onboardingCompleteKey] ?: false
    }

    val hasCompletedBusinessSetup: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[businessSetupCompleteKey] ?: false
    }

    val selectedTradeType: Flow<String?> = dataStore.data.map { preferences ->
        preferences[selectedTradeTypeKey]
    }

    val onboardingCompletedAt: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[onboardingCompletedAtKey]
    }

    val businessSetupCompletedAt: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[businessSetupCompletedAtKey]
    }

    val hasSeenWalkthrough: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[walkthroughSeenKey] ?: false
    }

    val debugProEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[debugProEnabledKey] ?: false
    }

    val defaultJobReminderType: Flow<String> = dataStore.data.map { preferences ->
        preferences[defaultJobReminderTypeKey] ?: ReminderTypes.NONE
    }

    val autoDisableCompletedJobReminders: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[autoDisableCompletedJobRemindersKey] ?: true
    }

    suspend fun setOnboardingComplete(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[onboardingCompleteKey] = value
            if (value && preferences[onboardingCompletedAtKey] == null) {
                preferences[onboardingCompletedAtKey] = System.currentTimeMillis()
            }
        }
    }

    suspend fun setBusinessSetupComplete(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[businessSetupCompleteKey] = value
            if (value && preferences[businessSetupCompletedAtKey] == null) {
                preferences[businessSetupCompletedAtKey] = System.currentTimeMillis()
            }
        }
    }

    suspend fun setSelectedTradeType(value: String?) {
        dataStore.edit { preferences ->
            val trimmed = value?.trim().orEmpty()
            if (trimmed.isBlank()) preferences.remove(selectedTradeTypeKey) else preferences[selectedTradeTypeKey] = trimmed
        }
    }

    suspend fun setWalkthroughSeen(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[walkthroughSeenKey] = value
        }
    }

    suspend fun markBusinessSetupComplete(tradeType: String?) {
        dataStore.edit { preferences ->
            val now = System.currentTimeMillis()
            preferences[onboardingCompleteKey] = true
            preferences[businessSetupCompleteKey] = true
            preferences[onboardingCompletedAtKey] = preferences[onboardingCompletedAtKey] ?: now
            preferences[businessSetupCompletedAtKey] = now
            val trimmed = tradeType?.trim().orEmpty()
            if (trimmed.isBlank()) preferences.remove(selectedTradeTypeKey) else preferences[selectedTradeTypeKey] = trimmed
        }
    }

    suspend fun setDebugProEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[debugProEnabledKey] = value
        }
    }

    suspend fun setDefaultJobReminderType(value: String) {
        dataStore.edit { preferences ->
            preferences[defaultJobReminderTypeKey] = value
        }
    }

    suspend fun setAutoDisableCompletedJobReminders(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[autoDisableCompletedJobRemindersKey] = value
        }
    }

    suspend fun resetSetupState() {
        dataStore.edit { preferences ->
            preferences[onboardingCompleteKey] = false
            preferences[businessSetupCompleteKey] = false
            preferences.remove(selectedTradeTypeKey)
            preferences.remove(onboardingCompletedAtKey)
            preferences.remove(businessSetupCompletedAtKey)
            preferences.remove(walkthroughSeenKey)
        }
    }
}
