package com.legendstreams.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Searches Internet Archive for video candidates. Archive item metadata and uploader-selected
 * licenses are not a legal guarantee; items must be reviewed before commercial redistribution.
 */
object InternetArchiveApi {
    private const val SEARCH_URL = "https://archive.org/advancedsearch.php"
    private const val METADATA_URL = "https://archive.org/metadata/"

    suspend fun searchMovies(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val terms = query.trim().takeIf { it.isNotBlank() }?.let { " AND title:\"${it.replace("\\\"", "") }\"" } ?: ""
        val q = "mediatype:movies$terms"
        val encoded = URLEncoder.encode(q, "UTF-8")
        val url = "$SEARCH_URL?q=$encoded&fl[]=identifier&fl[]=title&fl[]=year&fl[]=description&fl[]=licenseurl&rows=12&page=1&output=json&sort[]=downloads+desc"
        val response = getJson(url)
        val docs = response.optJSONObject("response")?.optJSONArray("docs") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until docs.length()) {
                val doc = docs.optJSONObject(i) ?: continue
                val id = doc.optString("identifier").takeIf { it.isNotBlank() } ?: continue
                val title = doc.optString("title").takeIf { it.isNotBlank() } ?: continue
                val license = doc.optString("licenseurl")
                val description = doc.optString("description").take(500).ifBlank { "Video item hosted by Internet Archive. Review the item's rights and license before reuse." }
                add(MediaItem(
                    title = title,
                    type = "Movie",
                    year = doc.optString("year").takeIf { it.isNotBlank() } ?: "—",
                    genre = "Internet Archive candidate",
                    description = "$description\nLicense info: ${license.ifBlank { "not provided" }}",
                    playbackUrl = null,
                    posterUrl = "https://archive.org/services/img/$id",
                    sourceUrl = "https://archive.org/details/$id"
                ))
            }
        }
    }

    private fun getJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "LegendStreams-Prototype/0.3")
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            if (status !in 200..299) throw IllegalStateException("Internet Archive request failed (HTTP $status).")
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }
}
