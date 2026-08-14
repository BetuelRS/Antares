package pt.antares.app.feature.running.ui

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

// Os quadrados do mapa vêm de fora, e isso é uma saída de dados: cada pedido diz a este
// serviço, pelo endereço IP, que zona do mundo está a ser vista — e durante uma corrida, essa
// zona é onde a pessoa está. O percurso em si nunca é enviado; fica na base do telemóvel.
private const val STYLE_URI = "https://tiles.openfreemap.org/styles/liberty"
private const val SRC = "run_path_src"
private const val LAYER = "run_path_layer"

private const val MAP_PADDING_PX = 64

@Composable
actual fun RunMap(
    path: List<Pair<Double, Double>>,
    modifier: Modifier,
    follow: Boolean,
) {
    val context = LocalContext.current

    remember { MapLibre.getInstance(context) }

    val mapView = remember { MapView(context) }
    val mapRef = remember { mutableStateOf<MapLibreMap?>(null) }
    val styleRef = remember { mutableStateOf<Style?>(null) }

    val fitted = remember { mutableStateOf(false) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        mapView.onCreate(null)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        mapView.getMapAsync { map ->
            mapRef.value = map
            map.setStyle(Style.Builder().fromUri(STYLE_URI)) { style ->
                styleRef.value = style
                style.addSource(GeoJsonSource(SRC))
                style.addLayer(
                    LineLayer(LAYER, SRC).withProperties(
                        PropertyFactory.lineColor(Color.parseColor("#4C8DFF")),
                        PropertyFactory.lineWidth(5f),
                    ),
                )
            }
        }
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = {
            val map = mapRef.value ?: return@AndroidView
            val style = styleRef.value ?: return@AndroidView
            if (path.isEmpty()) return@AndroidView
            val points = path.map { Point.fromLngLat(it.second, it.first) }
            style.getSourceAs<GeoJsonSource>(SRC)?.setGeoJson(LineString.fromLngLats(points))
            if (follow) {
                val last = path.last()
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(last.first, last.second))
                    .zoom(16.0)
                    .build()
            } else if (!fitted.value) {

                val ll = path.map { LatLng(it.first, it.second) }
                val distinct = ll.distinct()

                mapView.post {
                    runCatching {
                        if (distinct.size >= 2) {
                            val bounds = LatLngBounds.Builder().includes(distinct).build()
                            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, MAP_PADDING_PX))
                        } else {
                            map.cameraPosition = CameraPosition.Builder()
                                .target(ll.first())
                                .zoom(16.0)
                                .build()
                        }
                        fitted.value = true
                    }
                }
            }
        },
    )
}
