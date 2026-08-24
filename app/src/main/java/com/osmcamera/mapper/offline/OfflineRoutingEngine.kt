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
        cameras: List<Camera>
    ): Result<RouteComparison> {
        val h = hopper ?: return Result.failure(IllegalStateException("Graphe non chargé"))

        return try {
            val t0 = System.currentTimeMillis()

            // 1. Direct route
            val directRoute = route(h, start, end, cameras, "direct")
                ?: return Result.failure(Exception("Aucun itinéraire direct trouvé"))
            Log.d(TAG, "DIRECT : ${directRoute.distance.toInt()} m | ${directRoute.cameraCount} caméras")

            val candidates = mutableListOf(directRoute)

            // 2. Iterative waypoint refinement: repeatedly detour the worst remaining
            // camera cluster on the CURRENT best route, composing previous vias.
            var currentVias = emptyList<GeoPoint>()
            var currentBest = directRoute

            loop@ for (round in 0 until MAX_ROUNDS) {
                if (currentBest.cameraCount == 0) break@loop

                val clusters = clusterCamerasOnRoute(currentBest.points, cameras)
                if (clusters.isEmpty()) break@loop
                val target = clusters.first()

                val dirLat = end.latitude - start.latitude
                val dirLon = end.longitude - start.longitude
                val norm = kotlin.math.sqrt(dirLat * dirLat + dirLon * dirLon)
                if (norm == 0.0) break@loop
                val uLat = dirLat / norm
                val uLon = dirLon / norm

                val cLat = target.map { it.latitude }.average()
                val cLon = target.map { it.longitude }.average()

                var improved = false
                OFFSETS_METERS.forEach { offsetM ->
                    listOf(1.0, -1.0).forEach { side ->
                        val dLat = offsetM * side * (-uLon) / METERS_PER_DEG_LAT
                        val dLon = offsetM * side * uLon / METERS_PER_DEG_LAT * cos(cLat * Math.PI / 180)
                        val via = GeoPoint(cLat + dLat, cLon + dLon)

                        val vias = currentVias + via
                        val route = routePoints(
                            h,
                            listOf(start) + vias + listOf(end),
                            cameras,
                            "round${round}_${side}_$offsetM"
                        ) ?: return@forEach

                        Log.d(TAG, "Round $round via=${route.id} -> ${route.distance.toInt()} m, ${route.cameraCount} caméras")
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
                    Log.i(TAG, "Round $round: aucune amélioration, arrêt")
                    break@loop
                } else {
                    Log.i(TAG, "Round $round: ${currentBest.cameraCount} caméras restantes (${currentBest.distance.toInt()} m)")
                }
            }

            // 3. Dedup + rank like the online engine
            val unique = mutableListOf<Route>()
            candidates.forEach { r ->
                if (unique.none { abs(it.distance - r.distance) < 35 && it.cameraCount == r.cameraCount }) {
                    unique.add(r)
                }
            }
            val sorted = unique.sortedWith(compareBy<Route> { it.cameraCount }.thenBy { it.distance })
            val comparison = RouteComparison(
                routes = sorted.take(5),
                bestRoute = sorted.first(),
                directRoute = directRoute
            )
            Log.i(TAG, "Routage offline terminé en ${System.currentTimeMillis() - t0} ms")
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
        label: String
    ): Route? = routePoints(h, listOf(start, end), cameras, label)

    private fun routePoints(
        h: GraphHopper,
        waypoints: List<GeoPoint>,
        cameras: List<Camera>,
        label: String
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
                cameraCount = GeometryUtils.countCamerasNearRoute(pts, cameras, AVOID_RADIUS_M)
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
