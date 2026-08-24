package com.osmcamera.mapper.offline

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.graphhopper.GHRequest
import com.graphhopper.GraphHopper
import com.graphhopper.GraphHopperConfig
import com.graphhopper.config.Profile
import com.graphhopper.util.shapes.GHPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.hypot

class PocOfflineActivity : Activity() {

    private lateinit var status: TextView

    // République -> Montparnasse ; zone caméra ~Châtelet (16 caméras OSM réelles)
    private val fromLat = 48.8674; private val fromLon = 2.3636
    private val toLat = 48.8405; private val toLon = 2.3231
    private val zoneRing = arrayOf(
        doubleArrayOf(2.3450113, 48.8592288),
        doubleArrayOf(2.3447969, 48.8597788),
        doubleArrayOf(2.3442113, 48.8601814),
        doubleArrayOf(2.3434113, 48.8603288),
        doubleArrayOf(2.3426113, 48.8601814),
        doubleArrayOf(2.3420253, 48.8597788),
        doubleArrayOf(2.3418113, 48.8592288),
        doubleArrayOf(2.3420253, 48.8586788),
        doubleArrayOf(2.3426113, 48.8582762),
        doubleArrayOf(2.3434113, 48.8581288),
        doubleArrayOf(2.3442113, 48.8582762),
        doubleArrayOf(2.3447969, 48.8586788),
        doubleArrayOf(2.3450113, 48.8592288)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this)
        setContentView(status)
        CoroutineScope(Dispatchers.Default).launch {
            try {
                run()
            } catch (t: Throwable) {
                t.printStackTrace()
                append("ERREUR: $t")
            }
        }
    }

    private fun append(s: String) = runOnUiThread {
        status.text = "${status.text}\n$s"
        android.util.Log.i("PocOffline", s)
    }

    private fun route(vararg points: DoubleArray): Pair<Double, Int> {
        val request = GHRequest(points.map { GHPoint(it[1], it[0]) })
            .setProfile("foot")
            .setLocale("fr")
        val response = hopper.route(request)
        if (response.hasErrors()) throw IllegalStateException(response.errors.toString())
        val path = response.best
        var inZone = 0
        for (i in 1 until path.points.size() - 1) {
            if (pointInZone(path.points.getLon(i), path.points.getLat(i))) inZone++
        }
        return path.distance to inZone
    }

    private lateinit var hopper: GraphHopper

    private fun run() {
        val ghDir = File(filesDir, "gh-poc/graph-cache")
        append("Graphe: ${ghDir.absolutePath} existe=${ghDir.exists()}")

        val cfg = GraphHopperConfig()
            .putObject("graph.location", ghDir.absolutePath)
            .putObject("dataaccess", "MMAP")
        cfg.setProfiles(listOf(Profile("foot").setVehicle("foot").setWeighting("fastest")))

        hopper = GraphHopper()
        hopper.init(cfg)
        hopper.setAllowWrites(false)
        hopper.importOrLoad()
        append("Graphe chargé")

        val tAll = System.currentTimeMillis()

        // 1. Route directe
        val (dDirect, zDirect) = route(doubleArrayOf(fromLon, fromLat), doubleArrayOf(toLon, toLat))
        append("DIRECT : ${"%.0f".format(dDirect)} m | pts en zone caméra: $zDirect")

        // 2. Candidats de contournement (perpendiculaire +/-, offsets croissants)
        val pts8 = zoneRing.take(zoneRing.size - 1)
        val cx = pts8.fold(0.0) { a, p -> a + p[0] } / pts8.size
        val cy = pts8.fold(0.0) { a, p -> a + p[1] } / pts8.size
        val dx = toLon - fromLon; val dy = toLat - fromLat
        val n = hypot(dx, dy)
        var best: Double? = null
        append("Centre zone: $cx, $cy | dir: ${"%.4f".format(dx / n)},${"%.4f".format(dy / n)}")
        for (side in intArrayOf(1, -1)) {
            for (off in doubleArrayOf(0.0015, 0.0025, 0.0035)) {
                val viaLon = cx - dy / n * side * off
                val viaLat = cy + dx / n * side * off
                append("Candidat side=$side off=$off -> via=$viaLon,$viaLat")
                try {
                    val (d, z) = route(
                        doubleArrayOf(fromLon, fromLat),
                        doubleArrayOf(viaLon, viaLat),
                        doubleArrayOf(toLon, toLat)
                    )
                    append("   -> $d m, z=$z")
                    if (z == 0 && (best == null || d < best!!)) best = d
                } catch (e: Exception) {
                    append("   -> ERREUR: ${e.message?.take(80)}")
                }
            }
        }
        val dt = System.currentTimeMillis() - tAll

        append("DÉTOUR : ${if (best != null) "%.0f".format(best) + " m (+%.0f)".format(best - dDirect) else "aucun"}")
        append("Calcul total: $dt ms")
        append(if (best != null && best < dDirect * 1.5) "RESULTAT: ÉVITEMENT OFFLINE OK ✅" else "RESULTAT: ÉCHEC ❌")
    }

    private fun pointInZone(lon: Double, lat: Double): Boolean {
        var inside = false
        var j = zoneRing.size - 1
        for (i in zoneRing.indices) {
            if ((zoneRing[i][1] > lat) != (zoneRing[j][1] > lat) &&
                lon < (zoneRing[j][0] - zoneRing[i][0]) * (lat - zoneRing[i][1]) /
                (zoneRing[j][1] - zoneRing[i][1]) + zoneRing[i][0]
            ) inside = !inside
            j = i
        }
        return inside
    }
}
