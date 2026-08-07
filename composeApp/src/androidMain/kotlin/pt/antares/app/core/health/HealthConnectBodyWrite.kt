package pt.antares.app.core.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import kotlinx.datetime.TimeZone
import java.time.Instant
import java.time.ZoneOffset

internal suspend fun HealthConnectClient.writeBodyCompositionRecords(
    epochDay: Long,
    bodyFatPct: Double?,
    leanMassKg: Double?,
): Boolean {
    if (bodyFatPct == null && leanMassKg == null) return false

    val instante: Instant = Instant.ofEpochSecond(epochDay * 86_400L + 12 * 3600L)
    val offset = ZoneOffset.systemDefault().rules.getOffset(instante)

    val registos = buildList {
        bodyFatPct?.let {
            add(
                BodyFatRecord(
                    time = instante,
                    zoneOffset = offset,
                    percentage = Percentage(it),
                    metadata = Metadata.manualEntry(clientRecordId = "antares-bf-$epochDay"),
                ),
            )
        }
        leanMassKg?.let {
            add(
                LeanBodyMassRecord(
                    time = instante,
                    zoneOffset = offset,
                    mass = Mass.kilograms(it),
                    metadata = Metadata.manualEntry(clientRecordId = "antares-lbm-$epochDay"),
                ),
            )
        }
    }
    return try {
        insertRecords(registos)
        true
    } catch (e: Exception) {

        false
    }
}
