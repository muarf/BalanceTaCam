package com.osmcamera.mapper.offline

import android.content.Context
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
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

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
        private val BASE_URL = com.osmcamera.mapper.BuildConfig.REGION_SERVER_URL
        private val MANIFEST_URL = "$BASE_URL/manifest.json"
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState

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
                    // graphUrl may be relative (resolved against the manifest location)
                    val graphUrl = r.getString("graphUrl").let {
                        if (it.startsWith("http")) it else "$BASE_URL/$it"
                    }
                    regions.add(RegionInfo(
                        id = r.getString("id"),
                        name = r.getString("name"),
                        country = r.getString("country"),
                        graphUrl = graphUrl,
                        graphBytes = r.getLong("graphBytes"),
                        bbox = r.getJSONArray("bbox").let { b ->
                            (0 until b.length()).map { b.getDouble(it) }
                        },
                        builtAt = r.optString("builtAt", "")
                    ))
                }
                Log.i(TAG, "Manifest: ${regions.size} régions disponibles")
                Result.success(regions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur manifest", e)
            Result.failure(e)
        }
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

    suspend fun downloadRegion(info: RegionInfo): Result<Unit> = withContext(Dispatchers.IO) {
        if (installedRegionIds().contains(info.id)) {
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
}
