package com.legendstreams.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.view.ViewGroup

private val Ink = Color(0xFF090B12)
private val Panel = Color(0xFF151A28)
private val Violet = Color(0xFF8B5CF6)
private val Mint = Color(0xFF5EEAD4)

// Public test stream for development only. Replace with a provider URL you are licensed to distribute.
private const val TEST_HLS_URL = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"

data class MediaItem(
    val title: String,
    val type: String,
    val year: String,
    val genre: String,
    val description: String,
    val playbackUrl: String? = null,
    val tmdbId: Int? = null,
    val posterUrl: String? = null,
    val sourceUrl: String? = null,
    val licenseAttribution: String? = null
)

private val demoTitles = listOf(
    MediaItem(
        "Big Buck Bunny", "Movie", "2008", "Open animation",
        "A short animated open movie from the Blender Foundation. Licensed under Creative Commons Attribution 3.0; attribution must be retained.",
        playbackUrl = "https://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_320x180.mp4",
        posterUrl = "https://peach.blender.org/wp-content/uploads/title_anouncement.jpg?x11217",
        sourceUrl = "https://peach.blender.org/about/",
        licenseAttribution = "Big Buck Bunny by the Blender Foundation. Licensed under Creative Commons Attribution 3.0 (CC BY 3.0). Source and license: https://peach.blender.org/about/"
    ),
    MediaItem("HLS Player Test", "Movie", "DEMO", "Test stream", "A public test stream used to validate the video player. This is not a commercial movie.", TEST_HLS_URL),
    MediaItem("Midnight Signal", "Movie", "2025", "Sci-Fi", "A deep-space transmission changes the course of a quiet mission."),
    MediaItem("The Last Harbor", "Series", "2024", "Drama", "A coastal town rebuilds after a mysterious storm."),
    MediaItem("Wild Earth", "Series", "2025", "Documentary", "Explore remarkable habitats and the wildlife that calls them home."),
    MediaItem("Open Road", "Movie", "2023", "Adventure", "Two strangers set out on a journey across the country."),
    MediaItem("City Lights Live", "Live TV", "LIVE", "News", "Demo channel listing. Connect a live-TV provider with distribution rights to enable it.")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LegendStreamsApp() }
    }
}

@Composable
fun LegendStreamsApp() {
    var selectedTab by remember { mutableStateOf("Home") }
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<MediaItem?>(null) }
    var saved by remember { mutableStateOf(setOf<String>()) }
    var showPlans by remember { mutableStateOf(false) }
    var remoteTitles by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loadingCatalog by remember { mutableStateOf(false) }
    var catalogError by remember { mutableStateOf<String?>(null) }
    var archiveTitles by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var archiveLoading by remember { mutableStateOf(false) }
    var archiveError by remember { mutableStateOf<String?>(null) }
    val apiKey = BuildConfig.TMDB_API_KEY.trim()

    LaunchedEffect(selectedTab, search, apiKey) {
        if (selectedTab == "Movies") {
            archiveLoading = true
            archiveError = null
            try {
                archiveTitles = InternetArchiveApi.searchMovies(if (search.isNotBlank() && selectedTab == "Search") search else "")
            } catch (e: Exception) {
                archiveError = e.message ?: "Could not load archive movies."
                archiveTitles = emptyList()
            } finally {
                archiveLoading = false
            }
        }
        if (apiKey.isBlank()) {
            remoteTitles = emptyList()
            loadingCatalog = false
            catalogError = null
        } else if (selectedTab == "Live TV" || selectedTab == "My List") {
            loadingCatalog = false
            catalogError = null
        } else {
            loadingCatalog = true
            catalogError = null
            try {
                remoteTitles = TmdbApi.loadCatalog(apiKey, selectedTab, if (selectedTab == "Search") search else "")
            } catch (e: Exception) {
                catalogError = e.message ?: "Could not load the catalog."
                remoteTitles = emptyList()
            } finally {
                loadingCatalog = false
            }
        }
    }

    val catalogItems = when (selectedTab) {
        "Movies" -> archiveTitles + remoteTitles + demoTitles.filter { it.title == "Big Buck Bunny" || it.title == "HLS Player Test" }
        "Live TV" -> demoTitles
        else -> remoteTitles + demoTitles.filter { it.playbackUrl != null } + demoTitles.filter { saved.contains(it.title) }
    }
    val filtered = catalogItems.distinctBy { it.tmdbId?.toString() ?: it.title }.filter {
        (selectedTab == "Home" || selectedTab == "Search" ||
                (selectedTab == "Movies" && it.type == "Movie") ||
                (selectedTab == "Series" && it.type == "Series") ||
                (selectedTab == "Live TV" && it.type == "Live TV") ||
                (selectedTab == "My List" && saved.contains(it.title))) &&
        (search.isBlank() || "${it.title} ${it.genre} ${it.description}".contains(search, true))
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Ink, surface = Panel, primary = Violet,
            secondary = Mint, onBackground = Color.White, onSurface = Color.White
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
            if (selected != null) {
                DetailScreen(
                    item = selected!!,
                    isSaved = saved.contains(selected!!.title),
                    onBack = { selected = null },
                    onSave = {
                        val title = selected!!.title
                        saved = if (saved.contains(title)) saved - title else saved + title
                    },
                    onSubscribe = { showPlans = true }
                )
            } else {
                Column(Modifier.fillMaxSize()) {
                    Header(onPlans = { showPlans = true })
                    if (selectedTab == "Search") {
                        OutlinedTextField(
                            value = search, onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            label = { Text("Search movies, shows, genres") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Violet, unfocusedBorderColor = Color.DarkGray
                            )
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 18.dp)
                    ) {
                        item {
                            if (selectedTab == "Home") HeroBanner(onExplore = { selectedTab = "Movies" })
                        }
                        item {
                            when {
                                selectedTab == "Movies" && archiveLoading -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 18.dp), color = Mint)
                                loadingCatalog -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 18.dp), color = Mint)
                                selectedTab == "Movies" && archiveError != null -> Text("Archive source: $archiveError", color = Color(0xFFFFB4AB), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                                catalogError != null && selectedTab != "Movies" -> Text("Catalog connection: $catalogError", color = Color(0xFFFFB4AB), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                                selectedTab == "Movies" -> Text("Free movie candidates from Internet Archive plus modern catalog metadata. Verify each item's rights before commercial distribution.", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                                apiKey.isBlank() -> Text("TMDB is not configured yet. Add TMDB_API_KEY to your local Gradle properties and sync the project.", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                                else -> Text("Catalog powered by TMDB · metadata only", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                            }
                        }
                        item {
                            Text(
                                when (selectedTab) {
                                    "Home" -> "Featured for you"
                                    "My List" -> "Your watchlist"
                                    else -> selectedTab
                                },
                                fontSize = 21.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 18.dp, top = 18.dp, bottom = 10.dp)
                            )
                        }
                        if (filtered.isEmpty()) {
                            item { Text("Nothing here yet. Try another search.", color = Color.LightGray, modifier = Modifier.padding(20.dp)) }
                        }
                        items(filtered) { item ->
                            MediaRow(item = item, onClick = { selected = item }, isSaved = saved.contains(item.title))
                        }
                        item {
                            Text(
                                "Uses the TMDB API but is not endorsed or certified by TMDB. Metadata does not include streaming rights.",
                                color = Color.Gray, fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)
                            )
                        }
                    }
                    NavigationBar(containerColor = Color(0xFF10131D)) {
                        val tabs = listOf("Home", "Movies", "Series", "Live TV", "Search", "My List")
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        when (tab) {
                                            "Home" -> Icons.Default.Home
                                            "Live TV" -> Icons.Default.LiveTv
                                            "Search" -> Icons.Default.Search
                                            "My List" -> Icons.Default.Bookmark
                                            else -> Icons.Default.Star
                                        }, contentDescription = tab
                                    )
                                },
                                label = { Text(tab, maxLines = 1, fontSize = 9.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Mint, selectedTextColor = Mint,
                                    indicatorColor = Panel, unselectedIconColor = Color.LightGray,
                                    unselectedTextColor = Color.LightGray
                                )
                            )
                        }
                    }
                }
            }
            if (showPlans) {
                AlertDialog(
                    onDismissRequest = { showPlans = false },
                    title = { Text("Legend Streams Plus") },
                    text = {
                        Text("Billing is not active yet. Before charging users, connect Google Play Billing, server-side purchase verification, and entitlement handling. Ad placements also require an ad-network integration and consent flow.")
                    },
                    confirmButton = { TextButton(onClick = { showPlans = false }) { Text("Got it") } },
                    containerColor = Panel, titleContentColor = Color.White, textContentColor = Color.LightGray
                )
            }
        }
    }
}

@Composable
private fun Header(onPlans: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Ink).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(40.dp).background(
                    Brush.linearGradient(listOf(Violet, Color(0xFF4338CA))), RoundedCornerShape(12.dp)
                ), contentAlignment = Alignment.Center
            ) { Text("L", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White) }
            Column {
                Text("LEGEND", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("STREAMS", fontSize = 10.sp, color = Mint, letterSpacing = 3.sp)
            }
        }
        TextButton(onClick = onPlans) { Text("PLUS", color = Mint, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun HeroBanner(onExplore: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
            .background(
                Brush.linearGradient(listOf(Color(0xFF30245E), Color(0xFF15243B), Color(0xFF11141E))),
                RoundedCornerShape(22.dp)
            ).padding(22.dp)
    ) {
        Text("YOUR NEXT FAVORITE", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(12.dp))
        Text("Stories worth\nstaying in for.", fontSize = 31.sp, lineHeight = 35.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))
        Text("Movies • Series • Live TV", color = Color.LightGray, fontSize = 14.sp)
        Spacer(Modifier.height(18.dp))
        Button(onClick = onExplore, colors = ButtonDefaults.buttonColors(containerColor = Violet)) { Text("Explore movies") }
    }
}

@Composable
private fun MediaRow(item: MediaItem, onClick: () -> Unit, isSaved: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(104.dp).height(132.dp).background(
                Brush.linearGradient(listOf(Color(0xFF3C315D), Color(0xFF182C3B), Panel)),
                RoundedCornerShape(14.dp)
            ), contentAlignment = Alignment.Center
        ) {
            if (item.posterUrl != null) {
                AsyncImage(model = item.posterUrl, contentDescription = "Poster for ${item.title}", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text(item.title.split(" ").take(2).joinToString("\n"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        Column(Modifier.weight(1f).padding(start = 13.dp, end = 6.dp)) {
            Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text("${item.type}  •  ${item.year}  •  ${item.genre}", color = Mint, fontSize = 11.sp)
            Spacer(Modifier.height(7.dp))
            Text(item.description, color = Color.LightGray, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (isSaved) Text("IN YOUR LIST", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DetailScreen(
    item: MediaItem,
    isSaved: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onSubscribe: () -> Unit
) {
    var launchPlayer by remember(item.title) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Ink).padding(20.dp)) {
        TextButton(onClick = { if (launchPlayer) launchPlayer = false else onBack() }) {
            Text("← Back", color = Mint)
        }
        if (launchPlayer && item.playbackUrl != null) {
            Text(item.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            HlsPlayer(url = item.playbackUrl)
            Spacer(Modifier.height(12.dp))
            Text(
                item.licenseAttribution ?: "Development test stream. Replace with a licensed provider stream before production.",
                color = Color.Gray, fontSize = 12.sp
            )
        } else {
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier.fillMaxWidth().height(220.dp).background(
                    Brush.linearGradient(listOf(Color(0xFF3C315D), Color(0xFF182C3B), Panel)),
                    RoundedCornerShape(22.dp)
                ), contentAlignment = Alignment.Center
            ) { Text("LEGEND STREAMS", color = Mint, fontWeight = FontWeight.Black, letterSpacing = 3.sp) }
            Spacer(Modifier.height(22.dp))
            Text(item.title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text("${item.type}  •  ${item.year}  •  ${item.genre}", color = Mint)
            Spacer(Modifier.height(12.dp))
            Text(item.description, color = Color.LightGray)
            Spacer(Modifier.height(22.dp))
            if (item.playbackUrl != null) {
                Button(onClick = { launchPlayer = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Violet)) {
                    Text(if (item.title == "HLS Player Test") "▶ Play test stream" else "▶ Play video")
                }
            } else if (item.sourceUrl != null) {
                val context = LocalContext.current
                Button(onClick = { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(item.sourceUrl))) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Violet)) {
                    Text("View source and license")
                }
            } else {
                Button(onClick = onSubscribe, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Violet)) {
                    Text(if (item.type == "Live TV") "Connect authorized live provider" else "Connect licensed playback source")
                }
            }
            OutlinedButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Bookmark, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isSaved) "Remove from My List" else "Add to My List")
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (item.genre == "Internet Archive candidate") "Catalog candidate only. Internet Archive does not guarantee copyright status; review the item page and license and obtain permission where needed before streaming it in Legend Streams." else "Only entries with an authorized playback URL can play. Modern commercial titles and live TV require licensed provider configuration.",
                color = Color.Gray, fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun HlsPlayer(url: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItemBuilder = PlayerMediaItem.Builder().setUri(url)
            if (url.substringBefore('?').endsWith(".m3u8", ignoreCase = true)) {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            }
            setMediaItem(mediaItemBuilder.build())
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> if (player.playbackState != androidx.media3.common.Player.STATE_IDLE) player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = true
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        },
        update = { it.player = player },
        modifier = Modifier.fillMaxWidth().height(230.dp)
    )
}
