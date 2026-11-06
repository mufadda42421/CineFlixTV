package com.cineflix.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cineflix.tv.data.model.MediaItem
import com.cineflix.tv.data.model.WatchHistoryEntry
import com.cineflix.tv.ui.components.TvActionButton
import com.cineflix.tv.ui.components.TvPosterCard
import com.cineflix.tv.ui.theme.CineFlixColors
import java.util.Locale

data class HomeCatalogState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val trending: List<MediaItem> = emptyList(),
    val recommended: List<MediaItem> = emptyList(),
    val popularMovies: List<MediaItem> = emptyList(),
    val popularTv: List<MediaItem> = emptyList(),
    val topRatedMovies: List<MediaItem> = emptyList(),
    val upcomingMovies: List<MediaItem> = emptyList(),
    val topRatedTv: List<MediaItem> = emptyList(),
    val actionAdventure: List<MediaItem> = emptyList(),
    val sciFi: List<MediaItem> = emptyList(),
    val comedy: List<MediaItem> = emptyList(),
    val horror: List<MediaItem> = emptyList()
)

@Composable
fun HomeScreen(
    catalog: HomeCatalogState,
    history: List<WatchHistoryEntry>,
    genreMap: Map<Int, String>,
    genreAffinity: Map<Int, Int>,
    isInWatchlist: (Int, String) -> Boolean,
    onWatchNow: (MediaItem, Int, Int) -> Unit,
    onPlayTrailer: (MediaItem) -> Unit,
    onOpenDetails: (MediaItem) -> Unit,
    onToggleWatchlist: (MediaItem) -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (catalog.isLoading) {
        TvShimmerLoadingView(modifier = modifier)
        return
    }

    if (catalog.errorMessage != null) {
        TvErrorView(
            message = catalog.errorMessage,
            onRetry = onRetry,
            onOpenSettings = onOpenSettings,
            modifier = modifier
        )
        return
    }

    val heroPool = remember(catalog.trending) { catalog.trending.take(6) }
    var heroIndex by remember { mutableIntStateOf(0) }
    val featuredItem = heroPool.getOrNull(heroIndex.coerceIn(0, (heroPool.size - 1).coerceAtLeast(0)))

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CineFlixColors.ObsidianBg),
        contentPadding = PaddingValues(bottom = 52.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        // 1. Full-Bleed 16:9 Featured Hero Banner
        if (featuredItem != null) {
            item(key = "hero_banner") {
                val type = featuredItem.resolvedMediaType()
                val inList = isInWatchlist(featuredItem.id, type)
                val matchPct = featuredItem.matchPercentage(genreAffinity)
                val genreNames = featuredItem.allGenreIds()
                    .mapNotNull { genreMap[it] }
                    .take(3)
                    .joinToString(" / ")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(415.dp)
                ) {
                    val backdrop = featuredItem.backdropUrl("w1280") ?: featuredItem.posterUrl("w780")
                    if (backdrop != null) {
                        AsyncImage(
                            model = backdrop,
                            contentDescription = featuredItem.resolvedTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF28080C), CineFlixColors.ObsidianBg)
                                    )
                                )
                        )
                    }

                    // Horizontal & vertical cinematic contrast scrims
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        CineFlixColors.ObsidianBg,
                                        Color(0xD9080808),
                                        Color(0x40080808),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x66080808),
                                        Color.Transparent,
                                        CineFlixColors.ObsidianBg
                                    )
                                )
                            )
                    )

                    // Hero Content Overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(0.68f)
                            .padding(start = 32.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Clean unboxed metadata row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "$matchPct% Match",
                                color = CineFlixColors.EmeraldMatch,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text("·", color = CineFlixColors.TextMuted)
                            if (featuredItem.voteAverage > 0.0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = CineFlixColors.AmberRating,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.1f", featuredItem.voteAverage),
                                        color = CineFlixColors.AmberRating,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text("·", color = CineFlixColors.TextMuted)
                            }
                            Text(
                                text = featuredItem.resolvedYear,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (genreNames.isNotEmpty()) {
                                Text("·", color = CineFlixColors.TextMuted)
                                Text(
                                    text = genreNames,
                                    color = CineFlixColors.TextSecondary,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Text(
                            text = featuredItem.resolvedTitle,
                            color = Color.White,
                            fontSize = 42.sp,
                            lineHeight = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = featuredItem.overview.orEmpty()
                                .ifBlank { "Stream in high definition on CineFlix by MJ for Google TV." },
                            color = Color(0xFFD4D4D8),
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TvActionButton(
                                text = "Watch Now",
                                icon = Icons.Default.PlayArrow,
                                onClick = { onWatchNow(featuredItem, 1, 1) },
                                isPrimary = true
                            )
                            TvActionButton(
                                text = "Trailer",
                                icon = Icons.Default.Theaters,
                                onClick = { onPlayTrailer(featuredItem) },
                                isPrimary = false
                            )
                            TvActionButton(
                                text = "Details",
                                icon = Icons.Default.Info,
                                onClick = { onOpenDetails(featuredItem) },
                                isPrimary = false
                            )
                            TvActionButton(
                                text = if (inList) "In My List" else "My List",
                                icon = if (inList) Icons.Default.Check else Icons.Default.Add,
                                onClick = { onToggleWatchlist(featuredItem) },
                                isPrimary = false
                            )
                        }
                    }

                    // Hero Slide Controls (Bottom Right)
                    if (heroPool.size > 1) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 40.dp, bottom = 28.dp)
                        ) {
                            TvActionButton(
                                text = "",
                                icon = Icons.Default.ChevronLeft,
                                onClick = {
                                    heroIndex = if (heroIndex <= 0) heroPool.lastIndex else heroIndex - 1
                                },
                                isPrimary = false
                            )
                            Text(
                                text = "${heroIndex + 1} / ${heroPool.size}",
                                color = CineFlixColors.TextSecondary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            TvActionButton(
                                text = "",
                                icon = Icons.Default.ChevronRight,
                                onClick = {
                                    heroIndex = (heroIndex + 1) % heroPool.size
                                },
                                isPrimary = false
                            )
                        }
                    }
                }
            }
        }

        // 2. Continue Watching Rail (if history exists)
        if (history.isNotEmpty()) {
            item(key = "rail_continue_watching") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    RailHeader(
                        title = "Continue Watching",
                        subtitle = "Resume right where you left off",
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(history, key = { "hist_${it.mediaType}_${it.id}" }) { entry ->
                            val media = entry.toMediaItem()
                            val epLabel = if (entry.mediaType == "tv") {
                                "S${entry.seasonNumber} · E${entry.episodeNumber}"
                            } else null
                            TvPosterCard(
                                item = media,
                                progressFraction = entry.progressFraction.coerceAtLeast(0.08f),
                                episodeLabel = epLabel,
                                onClick = {
                                    onWatchNow(media, entry.seasonNumber, entry.episodeNumber)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. All Curated TMDB Horizontal Content Rails
        val rails = listOf(
            Triple("Trending Now", "Today's most watched movies and series across TMDB", catalog.trending),
            Triple("Recommended For You", "Personalized to your favorite genres and watch activity", catalog.recommended),
            Triple("Popular Movies", null, catalog.popularMovies),
            Triple("Popular TV Shows", null, catalog.popularTv),
            Triple("Top Rated Movies", "Critically acclaimed cinema of all time", catalog.topRatedMovies),
            Triple("Action & Adventure", null, catalog.actionAdventure),
            Triple("Sci-Fi", null, catalog.sciFi),
            Triple("Upcoming Movies", "Theatrical releases arriving soon", catalog.upcomingMovies),
            Triple("Top Rated TV Shows", null, catalog.topRatedTv),
            Triple("Comedy", null, catalog.comedy),
            Triple("Horror", null, catalog.horror)
        )

        rails.forEach { (title, subtitle, items) ->
            if (items.isNotEmpty()) {
                item(key = "rail_$title") {
                    MediaPosterRail(
                        title = title,
                        subtitle = subtitle,
                        items = items,
                        onItemClick = onOpenDetails
                    )
                }
            }
        }
    }
}

@Composable
fun RailHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                color = CineFlixColors.TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

@Composable
fun MediaPosterRail(
    title: String,
    subtitle: String?,
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RailHeader(
            title = title,
            subtitle = subtitle,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(
                items = items,
                key = { "${title}_${it.resolvedMediaType()}_${it.id}" }
            ) { item ->
                TvPosterCard(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}
