package com.osmcamera.mapper.offline

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

/**
 * Manages offline routing regions: manifest fetch, graph download/extract, deletion
 */
@Singleton
class OfflineRegionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @com.osmcamera.mapper.di.DownloadClient private val okHttpClient: OkHttpClient
) {

    companion object {
        private const val TAG = "OfflineRegions"

        /**
         * Regions are hosted on GitHub Releases (HTTPS, no personal server):
         * - manifest: versioned in-repo at offline-regions/manifest.json
         * - graphs: attached to region-<id>-<week> releases
         * Rebuilt weekly by .github/workflows/offline-regions.yml
         */
        const val MANIFEST_URL =
            "https://raw.githubusercontent.com/muarf/BalanceTaCam/main/offline-regions/manifest.json"
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState

    /** Expose mutable state for tile download progress updates from ViewModel */
    internal val tileDownloadState: MutableStateFlow<DownloadState> get() = _downloadState

    private fun regionsDir(): File = File(context.filesDir, "regions").apply { mkdirs() }

    suspend fun fetchManifest(): Result<List<RegionInfo>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(MANIFEST_URL).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Manifest HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(IOException("Manifest vide"))
                val root = JSONObject(body)
                val regions = mutableListOf<RegionInfo>()
                val arr = root.getJSONArray("regions")
                for (i in 0 until arr.length()) {
                    val r = arr.getJSONObject(i)
                    regions.add(RegionInfo(
                        id = r.getString("id"),
                        name = r.getString("name"),
                        country = r.getString("country"),
                        graphUrl = r.getString("graphUrl"),
                        graphBytes = r.getLong("graphBytes"),
                        bbox = r.getJSONArray("bbox").let { b ->
                            (0 until b.length()).map { b.getDouble(it) }
                        },
                        builtAt = r.optString("builtAt", "")
                    ))
                }
                // Vector basemaps (Mapsforge) — optional section
                if (root.has("basemaps")) {
                    val bList = mutableListOf<BasemapInfo>()
                    val bArr = root.getJSONArray("basemaps")
                    for (i in 0 until bArr.length()) {
                        val b = bArr.getJSONObject(i)
                        bList.add(BasemapInfo(
                            id = b.getString("id"),
                            name = b.getString("name"),
                            url = b.getString("url"),
                            bytes = b.getLong("bytes"),
                            bbox = b.getJSONArray("bbox").let { a ->
                                (0 until a.length()).map { a.getDouble(it) }
                            }
                        ))
                    }
                    _cachedBasemaps.value = bList
                    Log.i(TAG, "Manifest: ${_cachedBasemaps.value.size} cartes vectorielles")
                } else {
                    _cachedBasemaps.value = emptyList()
                }
                Log.i(TAG, "Manifest: ${regions.size} régions disponibles")
                Result.success(regions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur manifest", e)
            Result.failure(e)
        }
    }

    private val _cachedBasemaps = MutableStateFlow<List<BasemapInfo>>(emptyList())
    val cachedBasemaps: StateFlow<List<BasemapInfo>> = _cachedBasemaps

    /**
     * Basemap files live at files/basemaps/<id>.map with an <id>.json sidecar
     */
    private fun basemapsDir(): File = File(context.filesDir, "basemaps").apply { mkdirs() }

    fun installedBasemapIds(): List<String> =
        basemapsDir().listFiles()?.filter { it.extension == "map" }?.map { it.nameWithoutExtension } ?: emptyList()

    fun installedBasemapFiles(): List<File> =
        basemapsDir().listFiles()?.filter { it.extension == "map" && it.length() > 0 }?.sortedBy { it.name } ?: emptyList()

    fun installedBasemapUrl(id: String): String? {
        val metaFile = File(basemapsDir(), "$id.json")
        if (!metaFile.isFile) return null
        return try {
            JSONObject(metaFile.readText()).optString("url", "").ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    fun basemapFile(id: String): File? {
        val f = File(basemapsDir(), "$id.map")
        return if (f.isFile && f.length() > 0) f else null
    }

    suspend fun downloadBasemap(info: BasemapInfo, overwrite: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = basemapFile(info.id)
        if (!overwrite && existing != null) return@withContext Result.success(Unit)
        _downloadState.value = DownloadState.Downloading(0f, 0L, info.bytes)
        try {
            val request = Request.Builder().url(info.url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Téléchargement HTTP ${response.code}")
                val total = response.body?.contentLength() ?: info.bytes
                val tmp = File(context.cacheDir, "${info.id}.map.part")

                response.body!!.byteStream().use { input ->
                    tmp.outputStream().use { output ->
                        val buf = ByteArray(DEFAULT_BUFFER_SIZE * 8)
                        var read: Int
                        var downloaded = 0L
                        var lastEmit = 0L
                        while (input.read(buf).also { read = it } != -1) {
                            output.write(buf, 0, read)
                            downloaded += read
                            if (downloaded - lastEmit > 1024 * 1024) {
                                lastEmit = downloaded
                                _downloadState.value = DownloadState.Downloading(
                                    progress = downloaded.toFloat() / total,
                                    downloadedBytes = downloaded,
                                    totalBytes = total
                                )
                            }
                        }
                        _downloadState.value = DownloadState.Downloading(1f, downloaded, total)
                    }
                }

                if (!tmp.renameTo(File(basemapsDir(), "${info.id}.map"))) {
                    tmp.copyTo(File(basemapsDir(), "${info.id}.map"), overwrite = true)
                    tmp.delete()
                }
                File(basemapsDir(), "${info.id}.json").writeText(
                    JSONObject().apply {
                        put("id", info.id)
                        put("url", info.url)
                        put("bytes", info.bytes)
                    }.toString()
                )
                Log.i(TAG, "Carte ${info.id} installée")
            }
            _downloadState.value = DownloadState.Done
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Échec téléchargement carte ${info.id}", e)
            File(basemapsDir(), "${info.id}.map").delete()
            _downloadState.value = DownloadState.Error(e.message ?: "Erreur inconnue")
            Result.failure(e)
        }
    }

    fun deleteBasemap(id: String) {
        File(basemapsDir(), "$id.map").delete()
        File(basemapsDir(), "$id.json").delete()
        Log.i(TAG, "Carte $id supprimée")
    }

    /**
     * Installed region ids: one directory per region under files/regions/
     */
    fun installedRegionIds(): List<String> =
        regionsDir().listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()

    /**
     * Find the first installed region containing the given point
     */
    fun findInstalledRegionFor(lat: Double, lon: Double): String? {
        val all = regionsDir().listFiles()
        Log.d(TAG, "findInstalledRegionFor($lat, $lon): dirs=${all?.map { it.name }}, filesDir=${context.filesDir}")
        return all
            ?.filter { it.isDirectory && File(it, "graph-cache/edges").isFile }
            ?.firstOrNull { region ->
                val metaFile = File(region, "meta.json")
                if (!metaFile.isFile) return@firstOrNull true // accept without metadata
                try {
                    val meta = JSONObject(metaFile.readText())
                    val bbox = meta.getJSONArray("bbox")
                    lat >= bbox.getDouble(0) && lon >= bbox.getDouble(1)
                        && lat <= bbox.getDouble(2) && lon <= bbox.getDouble(3)
                } catch (_: Exception) {
                    true
                }
            }?.name
    }

    /**
     * Directory holding the GraphHopper graph-cache for an installed region
     */
    fun graphCacheDir(regionId: String): File? {
        val dir = File(File(regionsDir(), regionId), "graph-cache")
        return if (dir.isDirectory && File(dir, "edges").isFile) dir else null
    }

    /**
     * Source URL of the installed region build, used to detect newer weekly builds
     */
    fun installedGraphUrl(regionId: String): String? {
        val metaFile = File(File(regionsDir(), regionId), "meta.json")
        if (!metaFile.isFile) return null
        return try {
            JSONObject(metaFile.readText()).optString("graphUrl", "").ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun downloadRegion(info: RegionInfo, overwrite: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        if (overwrite) {
            deleteRegion(info.id)
        } else if (installedRegionIds().contains(info.id)) {
            return@withContext Result.success(Unit)
        }
        _downloadState.value = DownloadState.Downloading(0f, 0L, info.graphBytes)
        try {
            val request = Request.Builder().url(info.graphUrl).build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Téléchargement HTTP ${response.code}")
                val total = response.body?.contentLength() ?: info.graphBytes
                val tmp = File(context.cacheDir, "${info.id}.ghz.part")

                response.body!!.byteStream().use { input ->
                    tmp.outputStream().use { output ->
                        val buf = ByteArray(DEFAULT_BUFFER_SIZE * 8)
                        var read: Int
                        var downloaded = 0L
                        var lastEmit = 0L
                        while (input.read(buf).also { read = it } != -1) {
                            output.write(buf, 0, read)
                            downloaded += read
                            if (downloaded - lastEmit > 512 * 1024) {
                                lastEmit = downloaded
                                _downloadState.value = DownloadState.Downloading(
                                    progress = downloaded.toFloat() / total,
                                    downloadedBytes = downloaded,
                                    totalBytes = total
                                )
                            }
                        }
                        _downloadState.value = DownloadState.Downloading(1f, downloaded, total)
                    }
                }

                // Extract into final location
                val targetDir = File(regionsDir(), info.id).apply { mkdirs() }
                val files = unzip(tmp, targetDir)
                File(targetDir, "meta.json").writeText(
                    JSONObject().apply {
                        put("id", info.id)
                        put("name", info.name)
                        put("bbox", org.json.JSONArray(info.bbox))
                        put("builtAt", info.builtAt)
                        put("bytes", info.graphBytes)
                        put("graphUrl", info.graphUrl)
                    }.toString()
                )
                tmp.delete()
                Log.i(TAG, "Région ${info.id} installée ($files fichiers extraits)")
            }
            _downloadState.value = DownloadState.Done
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Échec téléchargement région ${info.id}", e)
            File(regionsDir(), info.id).deleteRecursively()
            _downloadState.value = DownloadState.Error(e.message ?: "Erreur inconnue")
            Result.failure(e)
        }
    }

    private fun unzip(zipFile: File, targetDir: File): Int {
        var count = 0
        java.util.zip.ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name).canonicalFile
                if (!outFile.path.startsWith(targetDir.canonicalPath)) {
                    throw IOException("Entrée zip suspecte: ${entry.name}")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { zis.copyTo(it) }
                    count++
                    _downloadState.value = DownloadState.Extracting(count, 7)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return count
    }

    fun deleteRegion(regionId: String) {
        File(regionsDir(), regionId).deleteRecursively()
        Log.i(TAG, "Région $regionId supprimée")
    }

    // ── MAPNIK Tile Cache (raster tiles for offline basemap) ──────────────

    private fun tileCacheDir(): File = File(context.filesDir, "tile-cache").apply { mkdirs() }

    fun tileCacheFile(regionId: String): File = File(tileCacheDir(), "$regionId.mbtiles")

    fun installedTileCacheIds(): List<String> =
        tileCacheDir().listFiles()
            ?.filter { it.extension == "mbtiles" && it.length() > 0 }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()

    fun tileCacheSize(regionId: String): Long =
        tileCacheFile(regionId).let { if (it.isFile) it.length() else 0L }

    fun deleteTileCache(regionId: String) {
        tileCacheFile(regionId).delete()
        Log.i(TAG, "Cache tuiles $regionId supprimé")
    }

    /**
     * Download MAPNIK raster tiles for a region bbox into an MBTiles SQLite file.
     * Zoom range 8-14 gives a good balance: ~5-50 Mo per region.
     */
    suspend fun downloadMapTiles(
        regionId: String,
        bbox: List<Double>,
        zoomMin: Int = 8,
        zoomMax: Int = 14,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val target = tileCacheFile(regionId)
        if (target.isFile && target.length() > 0) {
            return@withContext Result.success(Unit)
        }
        if (bbox.size < 4) return@withContext Result.failure(IOException("BBOX invalide"))

        val latS = bbox[0]; val lonW = bbox[1]; val latN = bbox[2]; val lonE = bbox[3]

        val tmp = File(context.cacheDir, "$regionId.mbtiles.part")
        try {
            val db = SQLiteDatabase.create(null)
            db.execSQL("CREATE TABLE IF NOT EXISTS metadata (name TEXT, value TEXT)")
            db.execSQL("INSERT INTO metadata (name, value) VALUES ('name', ?)", arrayOf(regionId))
            db.execSQL("INSERT INTO metadata (name, value) VALUES ('type', 'overlay')")
            db.execSQL("INSERT INTO metadata (name, value) VALUES ('version', '1')")
            db.execSQL("CREATE TABLE IF NOT EXISTS tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB)")

            var totalTiles = 0
            for (z in zoomMin..zoomMax) {
                val n = 2.0.pow(z.toDouble())
                val xMin = ((lonW + 180) / 360 * n).toInt().coerceIn(0, n.toInt() - 1)
                val xMax = ((lonE + 180) / 360 * n).toInt().coerceIn(0, n.toInt() - 1)
                val yMin = ((1 - ln(tan(Math.toRadians(latN)) + 1 / cos(Math.toRadians(latN))) / Math.PI) / 2 * n).toInt().coerceIn(0, n.toInt() - 1)
                val yMax = ((1 - ln(tan(Math.toRadians(latS)) + 1 / cos(Math.toRadians(latS))) / Math.PI) / 2 * n).toInt().coerceIn(0, n.toInt() - 1)
                totalTiles += (xMax - xMin + 1) * (yMax - yMin + 1)
            }
            if (totalTiles == 0) {
                db.close()
                tmp.delete()
                return@withContext Result.failure(IOException("Aucune tuile à télécharger"))
            }

            Log.i(TAG, "Téléchargement tuiles MAPNIK $regionId: $totalTiles tuiles (z$zoomMin-z$zoomMax)")
            var done = 0
            val insertStmt = db.compileStatement("INSERT INTO tiles VALUES (?, ?, ?, ?)")

            for (z in zoomMin..zoomMax) {
                val n = 2.0.pow(z.toDouble())
                val xMin = ((lonW + 180) / 360 * n).toInt().coerceIn(0, n.toInt() - 1)
                val xMax = ((lonE + 180) / 360 * n).toInt().coerceIn(0, n.toInt() - 1)
                val yMin = ((1 - ln(tan(Math.toRadians(latN)) + 1 / cos(Math.toRadians(latN))) / Math.PI) / 2 * n).toInt().coerceIn(0, n.toInt() - 1)
                val yMax = ((1 - ln(tan(Math.toRadians(latS)) + 1 / cos(Math.toRadians(latS))) / Math.PI) / 2 * n).toInt().coerceIn(0, n.toInt() - 1)

                for (x in xMin..xMax) {
                    for (y in yMin..yMax) {
                        try {
                            val url = "https://tile.openstreetmap.org/$z/$x/$y.png"
                            val request = Request.Builder().url(url)
                                .header("User-Agent", "BalanceTaCam/4.1 (offline map tiles)")
                                .build()
                            val response = okHttpClient.newCall(request).execute()
                            if (response.isSuccessful) {
                                val bytes = response.body?.bytes()
                                if (bytes != null && bytes.isNotEmpty()) {
                                    // MBTiles uses TMS y-flipping: y_tms = 2^z - 1 - y
                                    val yTms = (1 shl z) - 1 - y
                                    insertStmt.bindLong(1, z.toLong())
                                    insertStmt.bindLong(2, x.toLong())
                                    insertStmt.bindLong(3, yTms.toLong())
                                    insertStmt.bindBlob(4, bytes)
                                    insertStmt.executeInsert()
                                }
                            }
                            response.close()
                        } catch (_: Exception) {
                            // skip failed tiles
                        }
                        done++
                        if (done % 20 == 0 || done == totalTiles) {
                            onProgress(done, totalTiles)
                        }
                    }
                }
            }
            insertStmt.close()
            db.close()

            tmp.copyTo(target, overwrite = true)
            tmp.delete()
            Log.i(TAG, "Cache tuiles $regionId: ${target.length() / 1024} Ko")
            onProgress(totalTiles, totalTiles)
            Result.success(Unit)
        } catch (e: Exception) {
            tmp.delete()
            target.delete()
            Log.e(TAG, "Échec téléchargement tuiles $regionId", e)
            Result.failure(e)
        }
    }
}