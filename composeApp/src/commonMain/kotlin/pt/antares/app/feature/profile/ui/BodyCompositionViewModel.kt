package pt.antares.app.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.antares.app.core.calc.BodyComposition
import pt.antares.app.core.calc.BodyStats
import pt.antares.app.core.calc.NutritionCalc
import pt.antares.app.core.designsystem.oneDecimal
import pt.antares.app.core.database.entities.UserProfileEntity
import pt.antares.app.core.model.BodyFatSource
import pt.antares.app.core.model.Sex
import pt.antares.app.core.util.todayEpochDay
import pt.antares.app.feature.profile.data.BodyMeasurementRepository
import pt.antares.app.feature.profile.data.ProfileRepository

enum class BodyFatMethod { KNOWN, MEASUREMENTS, BMI, NONE }

data class BodyCompositionState(
    val loading: Boolean = true,
    val profile: UserProfileEntity? = null,
    val weightKg: Double? = null,
    val method: BodyFatMethod = BodyFatMethod.NONE,

    val knownPct: String = "",
    val waist: String = "",
    val neck: String = "",
    val hip: String = "",

    val arm: String = "",
    val thigh: String = "",
    val chest: String = "",
    val saved: Boolean = false,
) {
    val sex: Sex? get() = profile?.sex

    val needsHip: Boolean get() = sex == Sex.FEMALE
}

class BodyCompositionViewModel(
    private val repository: ProfileRepository,
    private val measurements: BodyMeasurementRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BodyCompositionState())
    val state: StateFlow<BodyCompositionState> = _state

    init {
        combine(
            repository.observeProfile(),
            repository.observeLatestWeight(),
        ) { profile, weight -> profile to weight?.weightKg }
            .onEach { (profile, weight) ->
                _state.update { current ->

                    if (!current.loading) {
                        current.copy(profile = profile, weightKg = weight)
                    } else {
                        current.copy(
                            loading = false,
                            profile = profile,
                            weightKg = weight,
                            method = profile?.bodyFatSource.toMethod(),
                            knownPct = profile?.bodyFatPct?.takeIf {
                                profile.bodyFatSource == BodyFatSource.MEASURED
                            }?.toText() ?: "",
                            waist = profile?.waistCm?.toText() ?: "",
                            neck = profile?.neckCm?.toText() ?: "",
                            hip = profile?.hipCm?.toText() ?: "",
                        )
                    }
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val ultima = measurements.latest() ?: return@launch
            _state.update {
                it.copy(
                    arm = it.arm.ifBlank { ultima.armCm?.toText() ?: "" },
                    thigh = it.thigh.ifBlank { ultima.thighCm?.toText() ?: "" },
                    chest = it.chest.ifBlank { ultima.chestCm?.toText() ?: "" },
                )
            }
        }
    }

    fun setMethod(method: BodyFatMethod) = _state.update { it.copy(method = method, saved = false) }
    fun setKnownPct(text: String) = _state.update { it.copy(knownPct = text, saved = false) }
    fun setWaist(text: String) = _state.update { it.copy(waist = text, saved = false) }
    fun setNeck(text: String) = _state.update { it.copy(neck = text, saved = false) }
    fun setHip(text: String) = _state.update { it.copy(hip = text, saved = false) }
    fun setArm(text: String) = _state.update { it.copy(arm = text, saved = false) }
    fun setThigh(text: String) = _state.update { it.copy(thigh = text, saved = false) }
    fun setChest(text: String) = _state.update { it.copy(chest = text, saved = false) }

    fun preview(): BodyStats? {
        val s = _state.value
        val profile = s.profile ?: return null
        val weight = s.weightKg ?: return null
        val pct = computedPct() ?: return null
        return BodyComposition.stats(
            sex = profile.sex,
            weightKg = weight,
            heightCm = profile.heightCm,
            ageYears = NutritionCalc.ageYears(profile.birthEpochDay, todayEpochDay()),
            bodyFatPct = pct,
            bodyFatSource = s.method.toSource(),
            waistCm = s.waist.toNumber(),
            neckCm = s.neck.toNumber(),
            hipCm = s.hip.toNumber(),
        )
    }

    fun computedPct(): Double? {
        val s = _state.value
        val profile = s.profile ?: return null
        val weight = s.weightKg ?: return null
        return when (s.method) {
            BodyFatMethod.KNOWN -> s.knownPct.toNumber()
            BodyFatMethod.MEASUREMENTS -> BodyComposition.navyBodyFat(
                sex = profile.sex,
                heightCm = profile.heightCm,
                waistCm = s.waist.toNumber() ?: return null,
                neckCm = s.neck.toNumber() ?: return null,
                hipCm = s.hip.toNumber(),
            )
            BodyFatMethod.BMI -> BodyComposition.bmi(weight, profile.heightCm)?.let {
                BodyComposition.deurenbergBodyFat(
                    sex = profile.sex,
                    bmi = it,
                    ageYears = NutritionCalc.ageYears(profile.birthEpochDay, todayEpochDay()),
                )
            }
            BodyFatMethod.NONE -> null
        }
    }

    fun save() {
        val s = _state.value
        val profile = s.profile ?: return
        val pct = computedPct().takeIf { s.method != BodyFatMethod.NONE }
        viewModelScope.launch {
            repository.saveProfile(
                profile.copy(
                    bodyFatPct = pct,
                    bodyFatSource = pct?.let { s.method.toSource() },
                    waistCm = s.waist.toNumber(),
                    neckCm = s.neck.toNumber(),
                    hipCm = s.hip.toNumber(),
                ),
            )

            measurements.record(
                bodyFatPct = pct,
                bodyFatSource = pct?.let { s.method.toSource() },
                waistCm = s.waist.toNumber(),
                neckCm = s.neck.toNumber(),
                hipCm = s.hip.toNumber(),
                armCm = s.arm.toNumber(),
                thighCm = s.thigh.toNumber(),
                chestCm = s.chest.toNumber(),
            )
            _state.update { it.copy(saved = true) }
        }
    }
}

private fun BodyFatSource?.toMethod(): BodyFatMethod = when (this) {
    BodyFatSource.MEASURED -> BodyFatMethod.KNOWN
    BodyFatSource.NAVY -> BodyFatMethod.MEASUREMENTS
    BodyFatSource.BMI -> BodyFatMethod.BMI
    null -> BodyFatMethod.NONE
}

private fun BodyFatMethod.toSource(): BodyFatSource? = when (this) {
    BodyFatMethod.KNOWN -> BodyFatSource.MEASURED
    BodyFatMethod.MEASUREMENTS -> BodyFatSource.NAVY
    BodyFatMethod.BMI -> BodyFatSource.BMI
    BodyFatMethod.NONE -> null
}

private fun String.toNumber(): Double? = trim().replace(',', '.').toDoubleOrNull()

private fun Double.toText(): String = oneDecimal(this, comma = true)
