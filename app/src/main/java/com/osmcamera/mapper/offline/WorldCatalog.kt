package com.osmcamera.mapper.offline

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Catalogue mondial des zones construisables (miroir de offline-regions/regions.yml).
 * Embarque en asset : recherche 100% hors-ligne, mise a jour par le job CI
 * finalize qui re-committe les deux fichiers ensemble.
 */
@Singleton
class WorldCatalog @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val entries: List<CatalogEntry> by lazy { load() }

    fun search(query: String, limit: Int = 80): List<CatalogEntry> {
        val q = normalize(query)
        if (q.isEmpty()) return entries.take(limit)
        return entries.filter {
            normalize(it.name).contains(q) || it.slug.contains(q)
        }.take(limit)
    }

    private fun load(): List<CatalogEntry> = try {
        context.assets.open(ASSET).bufferedReader().readLines()
            .mapNotNull { line ->
                val m = LINE_REGEX.find(line) ?: return@mapNotNull null
                CatalogEntry(
                    slug = m.groupValues[1],
                    name = m.groupValues[2],
                    continent = m.groupValues[3],
                    geopath = m.groupValues[4]
                )
            }
    } catch (e: Exception) {
        Log.e(TAG, "lecture catalogue impossible", e)
        emptyList()
    }

    data class CatalogEntry(
        val slug: String,
        val name: String,
        val continent: String,
        val geopath: String
    )

    companion object {
        private const val TAG = "WorldCatalog"
        private const val ASSET = "offline_regions.yml"
        private val LINE_REGEX =
            Regex("""id:\s*(\S+),\s*name:\s*"([^"]*)",\s*country:\s*"(\w+)",\s*geopath:\s*"([^"]+)"""")

        fun normalize(query: String): String = java.text.Normalizer
            .normalize(query.lowercase(), java.text.Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
    }
}
