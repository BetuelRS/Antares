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

// De duas em duas horas é o mais curto que ainda não é assédio; de seis em seis já não chega
// para mudar nada num dia. Três horas dá quatro ou cinco avisos entre acordar e deitar.
val WATER_REMINDER_HOURS = 2..6
const val WATER_REMINDER_DEFAULT_H = 3

const val DIAS_DA_SEMANA = 7
const val MINUTOS_DO_DIA = 24 * 60

// Uma chave por refeição, derivada do nome da constante da enumeração. Renomear um valor
// de [MealSlot] faz a app esquecer o nome que a pessoa tinha escolhido.
private fun mealNameKey(slot: MealSlot) = stringPreferencesKey("meal_name_" + slot.name)

fun createPreferencesDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })

/**
 * A última contagem de AI que o servidor devolveu, guardada para o ecrã não abrir em
 * branco. Não é a verdade: quem conta é o servidor, e isto é só o que ele disse por último.
 */
data class StoredAiUsage(
    val used: Int,
    val limit: Int,
    val trial: Boolean,
    val day: String,
) {

    /**
     * O limite diário reinicia à meia-noite, por isso uma contagem de ontem vale zero
     * usado. A experiência é a exceção: esse saldo é total e não renova com o dia.
     */
    fun remaining(today: String): Int =
        if (!trial && day != today) limit else (limit - used).coerceAtLeast(0)
}

/**
 * Todas as preferências da app num sítio só. Aqui vive o que é escolha e definição; o que
 * são dados fica na base. Cada leitura tem o valor por omissão embutido — nenhuma devolve
 * nulo por a chave ainda não existir, e é isso que faz a primeira abertura funcionar.
 */
// Trinta e uma funções, e é para ter trinta e uma: são pares de ler e escrever, um por
// preferência. Parti-las por classes daria a alguém dois sítios onde procurar a mesma chave,
// e o comentário das `Keys` diz porque é que uma chave errada não dá erro nenhum.
@Suppress("TooManyFunctions")
class AppPreferences(private val dataStore: DataStore<Preferences>) {

    // Os nomes das chaves são o formato guardado no disco: mudar um deles apaga a
    // preferência de quem já tem a app instalada, sem erro nenhum a avisar.
    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val waterGoalOverrideMl = androidx.datastore.preferences.core.intPreferencesKey("water_goal_override_ml")
        val lastCelebratedStreak = androidx.datastore.preferences.core.intPreferencesKey("last_celebrated_streak")
        val themeMode = androidx.datastore.preferences.core.stringPreferencesKey("theme_mode")
        val fastingNotifications = booleanPreferencesKey("fasting_notifications_enabled")
        val patternSuggestions = booleanPreferencesKey("pattern_suggestions")

        val mealRemindersEnabled = booleanPreferencesKey("notif_meal_reminders")
        val weighInReminderEnabled = booleanPreferencesKey("notif_weighin")
        val coachReadyNotifEnabled = booleanPreferencesKey("notif_coach_ready")
        val quietHoursEnabled = booleanPreferencesKey("notif_quiet_enabled")
        val quietStartMin = androidx.datastore.preferences.core.intPreferencesKey("notif_quiet_start")
        val quietEndMin = androidx.datastore.preferences.core.intPreferencesKey("notif_quiet_end")
        val runOemWarningShown = booleanPreferencesKey("run_oem_warning_shown")
        val adminUnlimited = booleanPreferencesKey("admin_unlimited")
        val adminRevelado = booleanPreferencesKey("admin_revelado")
        val adaptiveTargets = booleanPreferencesKey("adaptive_targets_enabled")
        val runGoalType = androidx.datastore.preferences.core.stringPreferencesKey("run_goal_type")
        val runGoalValue = androidx.datastore.preferences.core.intPreferencesKey("run_goal_value")
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
        val onboardingSkipped =
            androidx.datastore.preferences.core.stringSetPreferencesKey("onboarding_skipped")
        val waterReminderEnabled = booleanPreferencesKey("notif_water")
        val waterReminderIntervalH =
            androidx.datastore.preferences.core.intPreferencesKey("notif_water_interval_h")
        val lastWaterNotifAt =
            androidx.datastore.preferences.core.longPreferencesKey("notif_water_last_at")
        val weighInDayIso = androidx.datastore.preferences.core.intPreferencesKey("notif_weighin_day")
        val weighInMinute = androidx.datastore.preferences.core.intPreferencesKey("notif_weighin_min")
        val lastWeighInNotifDay =
            androidx.datastore.preferences.core.longPreferencesKey("notif_weighin_last_day")
    }

    /**
     * Os passos do arranque que a pessoa saltou, pelo nome da constante.
     *
     * Guarda-se para a app poder pedir o que falta quando fizer falta: o que ficou por
     * responder entrou no perfil como valor por omissão, e um valor por omissão que ninguém
     * sabe que lá está é indistinguível de uma resposta.
     */
    val onboardingSkipped: Flow<Set<String>> =
        dataStore.data.map { it[Keys.onboardingSkipped] ?: emptySet() }

    suspend fun setOnboardingSkipped(steps: Set<String>) {
        dataStore.edit { it[Keys.onboardingSkipped] = steps }
    }

    suspend fun clearOnboardingSkipped(step: String) {
        dataStore.edit { prefs ->
            val restantes = (prefs[Keys.onboardingSkipped] ?: emptySet()) - step
            prefs[Keys.onboardingSkipped] = restantes
        }
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

    // Anulável de propósito: nulo quer dizer que a meta de água vem do peso, pelo
    // [DailyGoals]. Um zero seria uma meta de zero mililitros.
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

    val fastingNotifications: Flow<Boolean> =
        dataStore.data.map { it[Keys.fastingNotifications] ?: true }

    suspend fun setFastingNotifications(enabled: Boolean) {
        dataStore.edit { it[Keys.fastingNotifications] = enabled }
    }

    // Desligado por omissão, ao contrário dos lembretes: apontar hábitos a quem não os
    // pediu é opinião, e a app só a dá quando lha pedem. O mesmo vale para a gamificação.
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

    /**
     * O lembrete de água. **Desligado por omissão**, ao contrário dos outros três: a app não
     * decide sozinha que alguém quer ser interrompida de duas em duas horas por causa de um
     * copo de água. Quem o quer, liga-o.
     */
    val waterReminder: Flow<Boolean> =
        dataStore.data.map { it[Keys.waterReminderEnabled] ?: false }

    suspend fun setWaterReminder(enabled: Boolean) {
        dataStore.edit { it[Keys.waterReminderEnabled] = enabled }
    }

    val waterReminderIntervalH: Flow<Int> =
        dataStore.data.map { it[Keys.waterReminderIntervalH] ?: WATER_REMINDER_DEFAULT_H }

    suspend fun setWaterReminderIntervalH(hours: Int) {
        dataStore.edit { it[Keys.waterReminderIntervalH] = hours.coerceIn(WATER_REMINDER_HOURS) }
    }

    /** Quando o último aviso de água saiu. É o que faz o intervalo escolhido valer. */
    val lastWaterNotifAt: Flow<Long> =
        dataStore.data.map { it[Keys.lastWaterNotifAt] ?: 0L }

    suspend fun setLastWaterNotifAt(epochMillis: Long) {
        dataStore.edit { it[Keys.lastWaterNotifAt] = epochMillis }
    }

    /** O dia da semana da pesagem, em ISO: 1 é segunda-feira. */
    val weighInDayIso: Flow<Int> =
        dataStore.data.map { it[Keys.weighInDayIso] ?: NotificationRules.DEFAULT_WEIGH_IN_DAY_ISO }

    val weighInMinuteOfDay: Flow<Int> =
        dataStore.data.map { it[Keys.weighInMinute] ?: NotificationRules.DEFAULT_WEIGH_IN_MIN }

    suspend fun setWeighInSchedule(dayIso: Int, minuteOfDay: Int) {
        dataStore.edit {
            it[Keys.weighInDayIso] = dayIso.coerceIn(1, DIAS_DA_SEMANA)
            it[Keys.weighInMinute] = minuteOfDay.coerceIn(0, MINUTOS_DO_DIA - 1)
        }
    }

    /** O dia em que o último aviso de pesagem saiu, para não sair duas vezes no mesmo. */
    val lastWeighInNotifDay: Flow<Long> =
        dataStore.data.map { it[Keys.lastWeighInNotifDay] ?: -1L }

    suspend fun setLastWeighInNotifDay(epochDay: Long) {
        dataStore.edit { it[Keys.lastWeighInNotifDay] = epochDay }
    }

    val coachReadyNotif: Flow<Boolean> =
        dataStore.data.map { it[Keys.coachReadyNotifEnabled] ?: true }

    suspend fun setCoachReadyNotif(enabled: Boolean) {
        dataStore.edit { it[Keys.coachReadyNotifEnabled] = enabled }
    }

    // Horas de silêncio ligadas de origem: uma app de nutrição não acorda ninguém.
    val quietHoursEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.quietHoursEnabled] ?: true }

    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.quietHoursEnabled] = enabled }
    }

    val quietStartMin: Flow<Int> =
        dataStore.data.map { it[Keys.quietStartMin] ?: NotificationRules.DEFAULT_QUIET_START_MIN }

    val quietEndMin: Flow<Int> =
        dataStore.data.map { it[Keys.quietEndMin] ?: NotificationRules.DEFAULT_QUIET_END_MIN }

    // Escrita conjunta: gravadas em separado, uma leitura pelo meio apanhava o início novo
    // com o fim antigo, e a janela de silêncio ficava invertida.
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

    val adminUnlimited: Flow<Boolean> =
        dataStore.data.map { it[Keys.adminUnlimited] ?: false }

    suspend fun setAdminUnlimited(enabled: Boolean) {
        dataStore.edit { it[Keys.adminUnlimited] = enabled }
    }

    /**
     * Se a entrada da administração aparece nas definições. Fica falsa até alguém tocar
     * sete vezes na versão, como o Android faz com o modo de programador — e mesmo depois
     * de revelada continua a pedir o código, que nunca entra em ficheiro nenhum.
     */
    val adminRevelado: Flow<Boolean> =
        dataStore.data.map { it[Keys.adminRevelado] ?: false }

    suspend fun revelarAdmin() {
        dataStore.edit { it[Keys.adminRevelado] = true }
    }

    val adaptiveTargets: Flow<Boolean> =
        dataStore.data.map { it[Keys.adaptiveTargets] ?: true }

    suspend fun setAdaptiveTargets(enabled: Boolean) {
        dataStore.edit { it[Keys.adaptiveTargets] = enabled }
    }

    val aiUsage: Flow<StoredAiUsage?> = dataStore.data.map { p ->
        // O limite é o que decide se já houve alguma resposta do servidor. Sem ele o valor
        // é nulo, e o ecrã diz que ainda não sabe em vez de mostrar um zero inventado.
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

    // Chamado ao apagar a conta e ao importar uma cópia de segurança: as preferências têm
    // de desaparecer com os dados, ou a app volta com metade do estado de outra pessoa.
    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    // Marcas de água da sincronização com o Health Connect. A zero, a próxima importação
    // varre o histórico todo em vez de só o que é novo.
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
