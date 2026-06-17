package team.kid.roadsafety.infrastructure.map

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionDefinition
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import team.kid.roadsafety.domain.aggregates.map.MapCityBbox
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

@Singleton
class MapTileCacheService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val offlineManager: OfflineManager by lazy {
        MapLibre.getInstance(context)
        OfflineManager.getInstance(context)
    }

    private val stylePrefs = context.getSharedPreferences("map_style_cache", Context.MODE_PRIVATE)

    private val tileOkHttpClient: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .apply {
                interceptors().clear()
                networkInterceptors().clear()
                authenticator(okhttp3.Authenticator.NONE)
            }
            .build()
    }

    suspend fun getStyleJsonForCity(cityId: String): String? {
        // Use OpenFreeMap as the main option by returning null.
        // This falls back to BaseStyle.Uri(MapBaseStyleUrl) (https://tiles.openfreemap.org/styles/bright) in MapColoringScreen.kt.
        return null

        /* Original PMTiles implementation:
        val template = withContext(Dispatchers.IO) {
            try {
                context.assets.open("protomaps_light_template.json").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                null
            }
        }

        val pmtilesUrl = when (cityId) {
            "salekhard" -> "pmtiles://https://roadsafety.my.to/maps/salekhard.pmtiles"
            else -> "pmtiles://https://roadsafety.my.to/maps/ekb.pmtiles"
        }

        return if (template != null) {
            template.replace("%PMTILES_URL%", pmtilesUrl)
        } else {
            getFallbackStyleJson(cityId)
        }
        */
    }

    private fun getFallbackStyleJson(cityId: String): String {
        val pmtilesUrl = when (cityId) {
            "salekhard" -> "pmtiles://https://roadsafety.my.to/maps/salekhard.pmtiles"
            else -> "pmtiles://https://roadsafety.my.to/maps/ekb.pmtiles"
        }
        return """
            {
              "version": 8,
              "sources": {
                "openmaptiles": {
                  "type": "vector",
                  "url": "$pmtilesUrl"
                }
              },
              "layers": [
                {
                  "id": "background",
                  "type": "background",
                  "paint": {
                    "background-color": "#f8f4f0"
                  }
                }
              ]
            }
        """.trimIndent()
    }

    suspend fun configureAmbientCache() {
        withContext(Dispatchers.Main) {
            offlineManager.setMaximumAmbientCacheSize(AmbientCacheBytes, null)
        }
    }

    fun clearWarmedCityVersion(cityId: String) {
        val key = "warmed_${cityId}"
        stylePrefs.edit().remove(key).apply()
    }

    suspend fun refreshMapCache(
        cityId: String,
        generationVersion: String,
        bbox: MapCityBbox?,
        styleUrl: String,
        tileUrlTemplate: String
    ) {
        configureAmbientCache()
        if (bbox == null) return

        val key = "warmed_${cityId}"
        val savedVersion = stylePrefs.getString(key, null)
        if (savedVersion == generationVersion) {
            return
        }

        withContext(Dispatchers.Main) {
            offlineManager.setOfflineMapboxTileCountLimit(OfflineTileLimit)
            val region = findRegion(cityId)
            if (region == null) {
                createOfflineRegion(cityId, styleUrl, bbox)
            } else {
                region.invalidateSuspend()
            }
        }

        warmSafetyTiles(tileUrlTemplate, bbox, generationVersion)

        stylePrefs.edit().putString(key, generationVersion).apply()
    }

    private suspend fun createOfflineRegion(
        cityId: String,
        styleUrl: String,
        bbox: MapCityBbox
    ) {
        val definition = OfflineTilePyramidRegionDefinition(
            styleUrl,
            bbox.toLatLngBounds(),
            BaseMapMinZoom,
            BaseMapMaxZoom,
            context.resources.displayMetrics.density.coerceAtLeast(1f),
            false
        )
        val region = offlineManager.createRegionSuspend(definition, regionMetadata(cityId))
        region.setObserver(object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                if (status.isComplete) {
                    region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                    region.setObserver(null)
                }
            }

            override fun onError(error: OfflineRegionError) {
                region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                region.setObserver(null)
            }

            override fun mapboxTileCountLimitExceeded(limit: Long) {
                region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                region.setObserver(null)
            }
        })
        region.setDownloadState(OfflineRegion.STATE_ACTIVE)
    }

    private suspend fun findRegion(cityId: String): OfflineRegion? {
        return offlineManager.listRegionsSuspend()
            .orEmpty()
            .firstOrNull { it.metadata.decodeToString() == regionMetadataText(cityId) }
    }

    private suspend fun warmSafetyTiles(
        tileUrlTemplate: String,
        bbox: MapCityBbox,
        generationVersion: String
    ) = withContext(Dispatchers.IO) {
        val urls = buildSafetyTileUrls(tileUrlTemplate, bbox)
        urls.forEach { url ->
            val request = Request.Builder().url(url).get().build()
            try {
                tileOkHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val bytes = response.body.bytes()
                    offlineManager.putResourceWithUrl(
                        url,
                        bytes,
                        modified = 0L,
                        expires = System.currentTimeMillis() / 1000L + SafetyTileTtlSeconds,
                        etag = generationVersion,
                        mustRevalidate = false
                    )
                }
            } catch (_: Exception) {
                // Best-effort cache warming; the visible map can still stream missing tiles.
            }
        }
    }

    private fun buildSafetyTileUrls(tileUrlTemplate: String, bbox: MapCityBbox): List<String> {
        val urls = mutableListOf<String>()
        for (z in SafetyTileMinZoom..SafetyTileMaxZoom) {
            val minTile = lonLatToTile(bbox.minLon, bbox.maxLat, z)
            val maxTile = lonLatToTile(bbox.maxLon, bbox.minLat, z)
            val xRange = minTile.x..maxTile.x
            val yRange = minTile.y..maxTile.y
            for (x in xRange) {
                for (y in yRange) {
                    if (urls.size >= SafetyTileWarmLimit) return urls
                    urls += tileUrlTemplate
                        .replace("{z}", z.toString())
                        .replace("{x}", x.toString())
                        .replace("{y}", y.toString())
                }
            }
        }
        return urls
    }

    private fun lonLatToTile(lon: Double, lat: Double, zoom: Int): TileCoordinate {
        val latRad = lat * PI / 180.0
        val n = 1 shl zoom
        val x = floor((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
        val y = floor((1.0 - ln(tan(latRad) + 1.0 / kotlin.math.cos(latRad)) / PI) / 2.0 * n)
            .toInt()
            .coerceIn(0, n - 1)
        return TileCoordinate(x, y)
    }

    private fun MapCityBbox.toLatLngBounds(): LatLngBounds {
        return LatLngBounds.from(maxLat, maxLon, minLat, minLon)
    }

    private fun regionMetadata(cityId: String): ByteArray = regionMetadataText(cityId).encodeToByteArray()

    private fun regionMetadataText(cityId: String): String = "roadsafety-base-map-$cityId"

    private suspend fun OfflineManager.listRegionsSuspend(): Array<OfflineRegion>? {
        return suspendCancellableCoroutine { continuation ->
            listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<OfflineRegion>?) {
                    if (continuation.isActive) continuation.resume(offlineRegions)
                }

                override fun onError(error: String) {
                    if (continuation.isActive) continuation.resume(null)
                }
            })
        }
    }

    private suspend fun OfflineManager.createRegionSuspend(
        definition: OfflineRegionDefinition,
        metadata: ByteArray
    ): OfflineRegion {
        return suspendCancellableCoroutine { continuation ->
            createOfflineRegion(
                definition,
                metadata,
                object : OfflineManager.CreateOfflineRegionCallback {
                    override fun onCreate(offlineRegion: OfflineRegion) {
                        if (continuation.isActive) continuation.resume(offlineRegion)
                    }

                    override fun onError(error: String) {
                        if (continuation.isActive) continuation.cancel(IllegalStateException(error))
                    }
                }
            )
        }
    }

    private suspend fun OfflineRegion.invalidateSuspend() {
        suspendCancellableCoroutine<Unit> { continuation ->
            invalidate(object : OfflineRegion.OfflineRegionInvalidateCallback {
                override fun onInvalidate() {
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onError(error: String) {
                    if (continuation.isActive) continuation.resume(Unit)
                }
            })
        }
    }

    private data class TileCoordinate(val x: Int, val y: Int)

    private companion object {
        const val BaseMapMinZoom = 9.0
        const val BaseMapMaxZoom = 18.0
        const val SafetyTileMinZoom = 9
        const val SafetyTileMaxZoom = 13
        const val SafetyTileWarmLimit = 400
        const val SafetyTileTtlSeconds = 24L * 60L * 60L
        const val AmbientCacheBytes = 250L * 1024L * 1024L
        const val OfflineTileLimit = 20_000L
    }
}
