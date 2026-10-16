package com.legendstreams.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** TMDB supplies catalog metadata only; it does not supply playback rights or streams. */
object TmdbApi {
    private const val BASE_URL = "https://api.themoviedb.org/3"
    private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"

    suspend fun loadCatalog(apiKey: String, tab: String, search: String): List<MediaItem> = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "Add your TMDB API key in the build configuration to load the catalog." }
        val path = when {
            search.isNotBlank() -> "/search/multi"
            tab == "Movies" -> "/movie/popular"
            tab == "Series" -> "/tv/popular"
            else -> "/trending/all/week"
        }
        val params = mutableListOf("api_key=${enc(apiKey)}", "language=en-US", "page=1")
        if (search.isNotBlank()) params += "query=${enc(search)}"
        val connection = (URL("$BASE_URL$path?${params.joinToString("&")}").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 12000
            setRequestProperty("Accept", "application/json")
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            if (status !in 200..299) {
                val message = runCatching { JSONObject(body).optString("status_message") }.getOrNull()
                throw IllegalStateException(message?.takeIf { it.isNotBlank() } ?: "TMDB request failed (HTTP $status). Check the API key and connection.")
            }
            val results = JSONObject(body).optJSONArray("results") ?: return@withContext emptyList()
            buildList {
                for (index in 0 until results.length()) {
                    val obj = results.optJSONObject(index) ?: continue
                    val mediaType = when {
                        obj.optString("media_type") == "movie" -> "Movie"
                        obj.optString("media_type") == "tv" -> "Series"
                        obj.has("title") -> "Movie"
                        obj.has("name") -> "Series"
                        else -> continue
                    }
                    if (tab == "Movies" && mediaType != "Movie") continue
                    if (tab == "Series" && mediaType != "Series") continue
                    val title = obj.optString("title").takeIf { it.isNotBlank() }
                        ?: obj.optString("name").takeIf { it.isNotBlank() } ?: "Untitled"
                    val date = obj.optString("release_date").takeIf { it.isNotBlank() }
                        ?: obj.optString("first_air_date").takeIf { it.isNotBlank() } ?: ""
                    val year = date.take(4).ifBlank { "—" }
                    val overview = obj.optString("overview").ifBlank { "No description is available for this title yet." }
                    val poster = obj.optString("poster_path").takeIf { it.isNotBlank() && it != "null" }
                        ?.let { "$IMAGE_BASE_URL$it" }
                    add(MediaItem(
                        title = title,
                        type = mediaType,
                        year = year,
                        genre = "TMDB catalog",
                        description = overview,
                        tmdbId = obj.optInt("id").takeIf { it > 0 },
                        posterUrl = poster
                    ))
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}
