package pt.antares.app.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import pt.antares.app.core.model.MealSlot
import pt.antares.app.core.notifications.NotificationRules

const val MEAL_NAME_MAX = 24

private fun mealNameKey(slot: MealSlot) = stringPreferencesKey("meal_name_" + slot.name)

fun createPreferencesDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })

data class StoredAiUsage(
    val used: Int,
    val limit: Int,
    val trial: Boolean,
    val day: String,
) {

    fun remaining(today: String): Int =
        if (!trial && day != today) limit else (limit - used).coerceAtLeast(0)
}

class AppPreferences(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val waterGoalOverrideMl = androidx.datastore.preferences.core.intPreferencesKey("water_goal_override_ml")
        val lastCelebratedStreak = androidx.datastore.preferences.core.intPreferencesKey("last_celebrated_streak")
        val themeMode = androidx.datastore.preferences.core.stringPreferencesKey("theme_mode")
        val heroStyle = androidx.datastore.preferences.core.stringPreferencesKey("hero_style")
        val fastingNotifications = booleanPreferencesKey("fasting_notifications_enabled")
        val patternSuggestions = booleanPreferencesKey("pattern_suggestions")

        val mealRemindersEnabled = booleanPreferencesKey("notif_meal_reminders")
        val weighInReminderEnabled = booleanPreferencesKey("notif_weighin")
        val coachReadyNotifEnabled = booleanPreferencesKey("notif_coach_ready")
        val quietHoursEnabled = booleanPreferencesKey("notif_quiet_enabled")
        val quietStartMin = androidx.datastore.preferences.core.intPreferencesKey("notif_quiet_start")
        val quietEndMin = androidx.datastore.preferences.core.intPreferencesKey("notif_quiet_end")
        val runOemWarningShown = booleanPreferencesKey("run_oem_warning_shown")
        val gamificationEnabled = booleanPreferencesKey("gamification_enabled")
        val adminUnlimited = booleanPreferencesKey("admin_unlimited")
        val adaptiveTargets = booleanPreferencesKey("adaptive_targets_enabled")
        val runGoalType = androidx.datastore.preferences.core.stringPreferencesKey("run_goal_type")
        val runGoalValue = androidx.datastore.preferences.core.intPreferencesKey("run_goal_value")
        val lastSyncAt = androidx.datastore.preferences.core.longPreferencesKey("last_sync_at")
        val lastHealthImportAt =
            androidx.datastore.preferences.core.longPreferencesKey("last_health_import_at")
        val lastHealthPublishAt =
            androidx.datastore.preferences.core.longPreferencesKey("last_health_publish_at")
        val aiUsed = androidx.datastore.preferences.core.intPreferencesKey("ai_used")
        val aiLimit = androidx.datastore.preferences.core.intPreferencesKey("ai_limit")
        val aiTrial = booleanPreferencesKey("ai_trial")
        val aiUsageDay = androidx.datastore.preferences.core.stringPreferencesKey("ai_usage_day")

        val lastSeenVersion = androidx.datastore.preferences.core.stringPreferencesKey("last_seen_version")
        val goalEngineNoticePending = booleanPreferencesKey("goal_engine_notice_pending")
    }

    val lastSeenVersion: Flow<String> =
        dataStore.data.map { it[Keys.lastSeenVersion] ?: "" }

    suspend fun setLastSeenVersion(version: String) {
        dataStore.edit { it[Keys.lastSeenVersion] = version }
    }

    val goalEngineNoticePending: Flow<Boolean> =
        dataStore.data.map { it[Keys.goalEngineNoticePending] ?: false }

    suspend fun setGoalEngineNoticePending(pending: Boolean) {
        dataStore.edit { it[Keys.goalEngineNoticePending] = pending }
    }

    val onboardingDone: Flow<Boolean> =
        dataStore.data.map { it[Keys.onboardingDone] ?: false }

    suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { it[Keys.onboardingDone] = done }
    }

    val mealNames: Flow<Map<MealSlot, String>> = dataStore.data.map { prefs ->
        MealSlot.entries.mapNotNull { slot ->
            prefs[mealNameKey(slot)]?.takeIf { it.isNotBlank() }?.let { slot to it }
        }.toMap()
    }

    suspend fun setMealName(slot: MealSlot, name: String) {
        dataStore.edit { prefs ->
            val limpo = name.trim().take(MEAL_NAME_MAX)
            if (limpo.isEmpty()) prefs.remove(mealNameKey(slot)) else prefs[mealNameKey(slot)] = limpo
        }
    }

    val waterGoalOverrideMl: Flow<Int?> =
        dataStore.data.map { it[Keys.waterGoalOverrideMl] }

    suspend fun setWaterGoalOverrideMl(ml: Int?) {
        dataStore.edit { prefs ->
            if (ml == null) prefs.remove(Keys.waterGoalOverrideMl) else prefs[Keys.waterGoalOverrideMl] = ml
        }
    }

    val lastCelebratedStreak: Flow<Int> =
        dataStore.data.map { it[Keys.lastCelebratedStreak] ?: 0 }

    suspend fun setLastCelebratedStreak(value: Int) {
        dataStore.edit { it[Keys.lastCelebratedStreak] = value }
    }

    val themeMode: Flow<String> =
        dataStore.data.map { it[Keys.themeMode] ?: "SYSTEM" }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[Keys.themeMode] = mode }
    }

    val heroStyle: Flow<String> =
        dataStore.data.map { it[Keys.heroStyle] ?: "CLASSIC" }

    suspend fun setHeroStyle(style: String) {
        dataStore.edit { it[Keys.heroStyle] = style }
    }

    val fastingNotifications: Flow<Boolean> =
        dataStore.data.map { it[Keys.fastingNotifications] ?: true }

    suspend fun setFastingNotifications(enabled: Boolean) {
        dataStore.edit { it[Keys.fastingNotifications] = enabled }
    }

    val patternSuggestions: Flow<Boolean> =
        dataStore.data.map { it[Keys.patternSuggestions] ?: false }

    suspend fun setPatternSuggestions(enabled: Boolean) {
        dataStore.edit { it[Keys.patternSuggestions] = enabled }
    }

    val mealReminders: Flow<Boolean> =
        dataStore.data.map { it[Keys.mealRemindersEnabled] ?: true }

    suspend fun setMealReminders(enabled: Boolean) {
        dataStore.edit { it[Keys.mealRemindersEnabled] = enabled }
    }

    val weighInReminder: Flow<Boolean> =
        dataStore.data.map { it[Keys.weighInReminderEnabled] ?: true }

    suspend fun setWeighInReminder(enabled: Boolean) {
        dataStore.edit { it[Keys.weighInReminderEnabled] = enabled }
    }

    val coachReadyNotif: Flow<Boolean> =
        dataStore.data.map { it[Keys.coachReadyNotifEnabled] ?: true }

    suspend fun setCoachReadyNotif(enabled: Boolean) {
        dataStore.edit { it[Keys.coachReadyNotifEnabled] = enabled }
    }

    val quietHoursEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.quietHoursEnabled] ?: true }

    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.quietHoursEnabled] = enabled }
    }

    val quietStartMin: Flow<Int> =
        dataStore.data.map { it[Keys.quietStartMin] ?: NotificationRules.DEFAULT_QUIET_START_MIN }

    val quietEndMin: Flow<Int> =
        dataStore.data.map { it[Keys.quietEndMin] ?: NotificationRules.DEFAULT_QUIET_END_MIN }

    suspend fun setQuietHours(startMin: Int, endMin: Int) {
        dataStore.edit {
            it[Keys.quietStartMin] = startMin
            it[Keys.quietEndMin] = endMin
        }
    }

    val runOemWarningShown: Flow<Boolean> =
        dataStore.data.map { it[Keys.runOemWarningShown] ?: false }

    suspend fun setRunOemWarningShown() {
        dataStore.edit { it[Keys.runOemWarningShown] = true }
    }

    val gamificationEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.gamificationEnabled] ?: false }

    suspend fun setGamificationEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.gamificationEnabled] = enabled }
    }

    val adminUnlimited: Flow<Boolean> =
        dataStore.data.map { it[Keys.adminUnlimited] ?: false }

    suspend fun setAdminUnlimited(enabled: Boolean) {
        dataStore.edit { it[Keys.adminUnlimited] = enabled }
    }

    val adaptiveTargets: Flow<Boolean> =
        dataStore.data.map { it[Keys.adaptiveTargets] ?: true }

    suspend fun setAdaptiveTargets(enabled: Boolean) {
        dataStore.edit { it[Keys.adaptiveTargets] = enabled }
    }

    val aiUsage: Flow<StoredAiUsage?> = dataStore.data.map { p ->
        val limit = p[Keys.aiLimit] ?: return@map null
        StoredAiUsage(
            used = p[Keys.aiUsed] ?: 0,
            limit = limit,
            trial = p[Keys.aiTrial] ?: true,
            day = p[Keys.aiUsageDay].orEmpty(),
        )
    }

    suspend fun setAiUsage(used: Int, limit: Int, trial: Boolean, day: String) {
        dataStore.edit {
            it[Keys.aiUsed] = used
            it[Keys.aiLimit] = limit
            it[Keys.aiTrial] = trial
            it[Keys.aiUsageDay] = day
        }
    }

    val runGoalType: Flow<String> =
        dataStore.data.map { it[Keys.runGoalType] ?: "NONE" }

    val runGoalValue: Flow<Int> =
        dataStore.data.map { it[Keys.runGoalValue] ?: 0 }

    suspend fun setRunGoal(type: String, value: Int) {
        dataStore.edit {
            it[Keys.runGoalType] = type
            it[Keys.runGoalValue] = value
        }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    val lastSyncAt: Flow<Long> =
        dataStore.data.map { it[Keys.lastSyncAt] ?: 0L }

    suspend fun setLastSyncAt(epochMs: Long) {
        dataStore.edit { it[Keys.lastSyncAt] = epochMs }
    }

    val lastHealthImportAt: Flow<Long> =
        dataStore.data.map { it[Keys.lastHealthImportAt] ?: 0L }

    suspend fun lastHealthImportAtOnce(): Long = lastHealthImportAt.first()

    suspend fun setLastHealthImportAt(epochMs: Long) {
        dataStore.edit { it[Keys.lastHealthImportAt] = epochMs }
    }

    val lastHealthPublishAt: Flow<Long> =
        dataStore.data.map { it[Keys.lastHealthPublishAt] ?: 0L }

    suspend fun lastHealthPublishAtOnce(): Long = lastHealthPublishAt.first()

    suspend fun setLastHealthPublishAt(epochMs: Long) {
        dataStore.edit { it[Keys.lastHealthPublishAt] = epochMs }
    }

}
