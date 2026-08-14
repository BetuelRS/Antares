package pt.antares.app.feature.running

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.getSystemService
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import pt.antares.app.feature.running.domain.GeoSample

internal interface RunLocationSource {

    fun start(onSample: (GeoSample) -> Unit)
    fun stop()

    companion object {

        const val INTERVAL_MS = 1_000L

        /**
         * Duas implementações: a dos serviços da Google, que funde GPS com sensores e dá
         * posições melhores, e a do sistema, para telemóveis que não os têm — o que inclui
         * boa parte dos Huawei e as versões sem Google.
         */
        fun create(context: Context): RunLocationSource {
            val available = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
            return if (available) FusedLocationSource(context) else LegacyLocationSource(context)
        }
    }
}

private class FusedLocationSource(context: Context) : RunLocationSource {

    private val client = LocationServices.getFusedLocationProviderClient(context)
    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    override fun start(onSample: (GeoSample) -> Unit) {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, RunLocationSource.INTERVAL_MS)
            .setMinUpdateIntervalMillis(RunLocationSource.INTERVAL_MS)

            // Espera pela primeira posição boa em vez de entregar logo uma da rede móvel,
            // que viria com quilómetros de erro e sujaria o início do percurso.
            .setWaitForAccurateLocation(true)
            // Sem distância mínima: o filtro de ruído é do [RunEngine], que precisa de ver
            // as amostras todas para distinguir parado de a andar devagar.
            .setMinUpdateDistanceMeters(0f)
            .build()

        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { onSample(it.toGeoSample()) }
            }
        }
        callback = cb
        client.requestLocationUpdates(request, cb, android.os.Looper.getMainLooper())
    }

    override fun stop() {
        callback?.let { client.removeLocationUpdates(it) }
        callback = null
    }
}

private class LegacyLocationSource(context: Context) : RunLocationSource {

    private val manager = context.getSystemService<LocationManager>()
    private var listener: LocationListener? = null

    @SuppressLint("MissingPermission")
    override fun start(onSample: (GeoSample) -> Unit) {
        val l = LocationListener { loc -> onSample(loc.toGeoSample()) }
        listener = l
        manager?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            RunLocationSource.INTERVAL_MS,
            0f,
            l,
        )
    }

    override fun stop() {
        listener?.let { manager?.removeUpdates(it) }
        listener = null
    }
}

internal fun Location.toGeoSample(): GeoSample = GeoSample(
    tMs = time,
    lat = latitude,
    lon = longitude,
    altM = if (hasAltitude()) altitude else null,
    accM = if (hasAccuracy()) accuracy.toDouble() else 0.0,
    speedMps = if (hasSpeed()) speed.toDouble() else null,
)
