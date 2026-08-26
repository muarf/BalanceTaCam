package com.osmcamera.mapper.offline

import android.util.Log
import com.graphhopper.GraphHopper
import com.graphhopper.config.Profile
import com.osmcamera.mapper.data.model.Camera
import com.osmcamera.mapper.data.model.Route
import com.osmcamera.mapper.data.model.RouteComparison
import com.osmcamera.mapper.utils.GeometryUtils
import org.osmdroid.util.GeoPoint
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fully offline routing engine: embedded GraphHopper + waypoint-based camera avoidance
 * (Custom Models are not available on ART: no javax.tools compiler)
 */
@Singleton
class OfflineRoutingEngine @Inject constructor() {

    companion object {
        private const val TAG = "OfflineRoutingEngine"
        private const val AVOID_RADIUS_M = 40.0
        private const val MAX_ROUNDS = 4
        private val OFFSETS_METERS = doubleArrayOf(150.0, 300.0, 500.0, 800.0)
    }

    @Volatile
    private var hopper: GraphHopper? = null

    @Volatile
    private var loadedRegionId: String? = null

    /**
     * Load a region graph (idempotent). Blocking I/O: call from Dispatchers.IO.
     */
    fun ensureLoaded(graphCacheDir: File, regionId: String): Boolean {
        if (loadedRegionId == regionId && hopper != null) return true
        synchronized(this) {
            if (loadedRegionId == regionId && hopper != null) return true
            close()
            val start = System.currentTimeMillis()
            val cfg = com.graphhopper.GraphHopperConfig()
                .putObject("graph.location", graphCacheDir.absolutePath)
                .putObject("dataaccess", "MMAP")
            cfg.setProfiles(listOf(Profile("foot").setVehicle("foot").setWeighting("fastest")))
            val h = GraphHopper()
            h.init(cfg)
            h.setAllowWrites(false)
            return try {
                h.importOrLoad()
                hopper = h
                loadedRegionId = regionId
                Log.i(TAG, "Graphe $regionId chargé en ${System.currentTimeMillis() - start} ms")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Chargement du graphe impossible", e)
                h.close()
                false
            }
        }
    }

    fun close() {
        hopper?.close()
        hopper = null
        loadedRegionId = null
    }

    fun isReady(): Boolean = hopper != null

    fun calculateAntiCameraRoutes(
        start: GeoPoint,
        end: GeoPoint,
        cameras: List<Camera>,
        avoidanceRadius: Double = AVOID_RADIUS_M
    ): Result<RouteComparison> {
        val h = hopper ?: return Result.failure(IllegalStateException("Graphe hors-ligne non chargé"))

        return try {
            val t0 = System.currentTimeMillis()
            val publicCameras = cameras.filter { it.surveillance == "public" || it.surveillance == null }
            Log.d(TAG, "Offline routing: ${publicCameras.size} public cameras available")

            // 1. Direct route
            val directRoute = route(h, start, end, publicCameras, "direct", avoidanceRadius)
                ?: return Result.failure(Exception("Aucun itinéraire direct trouvé"))
            Log.d(TAG, "DIRECT : ${directRoute.distance.toInt()} m | ${directRoute.cameraCount} caméras")

            val candidates = mutableListOf(directRoute)

            // 2. Native GraphHopper alternative routes
            try {
                val altReq = com.graphhopper.GHRequest(
                    listOf(
                        com.graphhopper.util.shapes.GHPoint(start.latitude, start.longitude),
                        com.graphhopper.util.shapes.GHPoint(end.latitude, end.longitude)
                    )
                ).setProfile("foot")
                altReq.algorithm = "alternative_route"
                altReq.hints.putObject("alternative_route.max_share_factor", 0.7)
                altReq.hints.putObject("alternative_route.max_weight_factor", 1.8)

                val altResp = h.route(altReq)
                if (!altResp.hasErrors()) {
                    altResp.all.forEachIndexed { idx, path ->
                        val pl = path.points
                        val pts = ArrayList<GeoPoint>(pl.size())
                        for (i in 0 until pl.size()) {
                            pts.add(GeoPoint(pl.getLat(i), pl.getLon(i)))
                        }
                        val d = path.distance
                        candidates.add(Route(
                            id = "alt_$idx",
                            points = pts,
                            distance = d,
                            duration = d / WALKING_SPEED_MS,
                            cameraCount = GeometryUtils.countCamerasNearRoute(pts, publicCameras, avoidanceRadius)
                        ))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Native alternatives error: ${e.message}")
            }

            // 3. Iterative local street detours around camera obstacles
            var currentBest = candidates.minByOrNull { it.cameraCount } ?: directRoute
            var currentVias = emptyList<GeoPoint>()

            loop@ for (round in 0 until MAX_ROUNDS) {
                if (currentBest.cameraCount == 0) break@loop

                // Find the first public camera hit on the current route
                val hitCameras = GeometryUtils.getCamerasAlongRoute(currentBest.points, publicCameras, avoidanceRadius)
                if (hitCameras.isEmpty()) break@loop
                val targetCam = hitCameras.first()

                // Find local road direction near this camera on current route
                val pts = currentBest.points
                var bestIdx = 0
                var minD = Double.MAX_VALUE
                for (i in pts.indices) {
                    val d = GeometryUtils.distance(pts[i].latitude, pts[i].longitude, targetCam.latitude, targetCam.longitude)
                    if (d < minD) {
                        minD = d
                        bestIdx = i
                    }
                }

                val prev = pts[maxOf(0, bestIdx - 2)]
                val next = pts[minOf(pts.size - 1, bestIdx + 2)]

                val cLat = targetCam.latitude
                val cLon = targetCam.longitude
                val cosLat = cos(cLat * Math.PI / 180.0)

                val dLat = (next.latitude - prev.latitude)
                val dLon = (next.longitude - prev.longitude) * cosLat
                val norm = kotlin.math.hypot(dLat, dLon)

                val (nLat, nLon) = if (norm > 1e-7) {
                    val tLat = dLat / norm
                    val tLon = dLon / norm
                    Pair(-tLon, tLat)
                } else {
                    Pair(0.0, 1.0)
                }

                var improved = false
                val offsets = doubleArrayOf(35.0, 60.0, 95.0, 140.0)

                for (offsetM in offsets) {
                    for (side in listOf(1.0, -1.0)) {
                        val vLat = cLat + (offsetM * side * nLat) / METERS_PER_DEG_LAT
                        val vLon = cLon + (offsetM * side * nLon) / (METERS_PER_DEG_LAT * cosLat)
                        val via = GeoPoint(vLat, vLon)

                        val vias = currentVias + via
                        val route = routePoints(
                            h,
                            listOf(start) + vias + listOf(end),
                            publicCameras,
                            "round${round}_${side}_${offsetM.toInt()}",
                            avoidanceRadius
                        ) ?: continue

                        candidates.add(route)

                        val better = route.cameraCount < currentBest.cameraCount ||
                            (route.cameraCount == currentBest.cameraCount && route.distance < currentBest.distance)
                        if (better) {
                            currentBest = route
                            currentVias = vias
                            improved = true
                        }
                    }
                }

                if (!improved) {
                    Log.i(TAG, "Round $round: aucune amélioration locale")
                    break@loop
                }
            }

            // 4. Dedup + rank
            val unique = mutableListOf<Route>()
            candidates.forEach { r ->
                if (unique.none { abs(it.distance - r.distance) < 25 && it.cameraCount == r.cameraCount }) {
                    unique.add(r)
                }
            }
            val sorted = unique.sortedWith(compareBy<Route> { it.cameraCount }.thenBy { it.distance })
            val comparison = RouteComparison(
                routes = sorted.take(5),
                bestRoute = sorted.first(),
                directRoute = directRoute
            )
            Log.i(TAG, "Routage offline terminé en ${System.currentTimeMillis() - t0} ms (meilleure: ${sorted.first().cameraCount} caméras, ${sorted.first().distance.toInt()}m)")
            Result.success(comparison)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur routage offline", e)
            Result.failure(e)
        }
    }

    private fun route(
        h: GraphHopper,
        start: GeoPoint,
        end: GeoPoint,
        cameras: List<Camera>,
        label: String,
        avoidanceRadius: Double = AVOID_RADIUS_M
    ): Route? = routePoints(h, listOf(start, end), cameras, label, avoidanceRadius)

    private fun routePoints(
        h: GraphHopper,
        waypoints: List<GeoPoint>,
        cameras: List<Camera>,
        label: String,
        avoidanceRadius: Double = AVOID_RADIUS_M
    ): Route? {
        return try {
            val request = com.graphhopper.GHRequest(
                waypoints.map { com.graphhopper.util.shapes.GHPoint(it.latitude, it.longitude) }
            ).setProfile("foot")

            val response = h.route(request)
            if (response.hasErrors()) {
                Log.w(TAG, "$label: erreurs ${response.errors}")
                return null
            }

            val bestPath = response.best
            val pl = bestPath.points
            val pts = ArrayList<GeoPoint>(pl.size())
            for (i in 0 until pl.size()) {
                pts.add(GeoPoint(pl.getLat(i), pl.getLon(i)))
            }

            val distance = bestPath.distance
            Route(
                id = label,
                points = pts,
                distance = distance,
                duration = distance / WALKING_SPEED_MS,
                cameraCount = GeometryUtils.countCamerasNearRoute(pts, cameras, avoidanceRadius)
            )
        } catch (e: Exception) {
            Log.e(TAG, "$label: échec", e)
            null
        }
    }

    /**
     * Cluster cameras near the direct route into avoidance zones
     */
    private fun clusterCamerasOnRoute(
        routePoints: List<GeoPoint>,
        cameras: List<Camera>
    ): List<List<Camera>> {
        val onRoute = GeometryUtils.getCamerasAlongRoute(routePoints, cameras, 120.0)
        if (onRoute.isEmpty()) return emptyList()

        val clusters = mutableListOf<MutableList<Camera>>()
        onRoute.forEach { cam ->
            val target = clusters.firstOrNull { c ->
                c.any { other ->
                    com.osmcamera.mapper.utils.GeometryUtils.distance(
                        cam.latitude, cam.longitude, other.latitude, other.longitude
                    ) < 150.0
                }
            }
            if (target != null) target.add(cam) else clusters.add(mutableListOf(cam))
        }

        return clusters.sortedByDescending { it.size }
    }

    private val WALKING_SPEED_MS = 1.35 // ~4.9 km/h
    private val METERS_PER_DEG_LAT = 111320.0
}
