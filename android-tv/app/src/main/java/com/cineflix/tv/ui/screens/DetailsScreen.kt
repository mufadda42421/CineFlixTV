package com.cineflix.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cineflix.tv.data.model.CastMemberDto
import com.cineflix.tv.data.model.CineFlixServers
import com.cineflix.tv.data.model.CrewMemberDto
import com.cineflix.tv.data.model.EpisodeDto
import com.cineflix.tv.data.model.MediaItem
import com.cineflix.tv.data.model.VideoDto
import com.cineflix.tv.ui.components.TvActionButton
import com.cineflix.tv.ui.components.TvEpisodeCard
import com.cineflix.tv.ui.components.TvFilterChip
import com.cineflix.tv.ui.components.TvPosterCard
import com.cineflix.tv.ui.theme.CineFlixColors
import java.util.Locale

data class TitleDetailsUiState(
    val mediaId: Int,
    val mediaType: String,
    val item: MediaItem? = null,
    val cast: List<CastMemberDto> = emptyList(),
    val crew: List<CrewMemberDto> = emptyList(),
    val trailers: List<VideoDto> = emptyList(),
    val recommended: List<MediaItem> = emptyList(),
    val similar: List<MediaItem> = emptyList(),
    val selectedSeason: Int = 1,
    val episodes: List<EpisodeDto> = emptyList(),
    val isLoadingDetails: Boolean = true,
    val isLoadingEpisodes: Boolean = false,
    val errorMessage: String? = null
)

@Composable
fun DetailsScreen(
    state: TitleDetailsUiState,
    preferredServerId: String,
    genreAffinity: Map<Int, Int>,
    isInWatchlist: Boolean,
    onSelectSeason: (Int) -> Unit,
    onWatchNow: (MediaItem, Int, Int) -> Unit,
    onPlayTrailer: (MediaItem, String?) -> Unit,
    onToggleWatchlist: (MediaItem) -> Unit,
    onCycleServer: () -> Unit,
    onOpenRelatedTitle: (MediaItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = state.item
    if (state.isLoadingDetails && item == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CineFlixColors.ObsidianBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = CineFlixColors.Crimson)
        }
        return
    }

    if (item == null) {
        TvErrorView(
            message = state.errorMessage ?: "Unable to load title details.",
            onRetry = onBack,
            onOpenSettings = onBack,
            modifier = modifier
        )
        return
    }

    val mediaType = item.resolvedMediaType(state.mediaType)
    val matchPct = item.matchPercentage(genreAffinity)
    val activeServer = CineFlixServers.findById(preferredServerId)
    val bestTrailerKey = state.trailers.firstOrNull()?.key
    var relatedTab by remember { mutableStateOf("recommended") }

    val directors = remember(state.crew) {
        state.crew.filter { it.job == "Director" }.map { it.name }.distinct().take(3)
    }
    val creators = remember(item.createdBy) {
        item.createdBy.map { it.name }.filter { it.isNotBlank() }.take(3)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CineFlixColors.ObsidianBg),
        contentPadding = PaddingValues(bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // 1. Full-Bleed Backdrop + Poster + Primary Metadata & Actions
        item(key = "details_hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(470.dp)
            ) {
                val backdrop = item.backdropUrl("w1280") ?: item.posterUrl("w780")
                if (backdrop != null) {
                    AsyncImage(
                        model = backdrop,
                        contentDescription = item.resolvedTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    CineFlixColors.ObsidianBg,
                                    Color(0xE6080808),
                                    Color(0x73080808)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x80080808),
                                    Color.Transparent,
                                    CineFlixColors.ObsidianBg
                                )
                            )
                        )
                )

                // Back Button Top-Left
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 36.dp, top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TvActionButton(
                        text = "Back",
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBack,
                        isPrimary = false
                    )
                }

                // Poster + Metadata Split
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // 2:3 Poster Card
                    Box(
                        modifier = Modifier
                            .width(196.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CineFlixColors.SurfaceCard)
                            .border(1.5.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    ) {
                        val poster = item.posterUrl("w500")
                        if (poster != null) {
                            AsyncImage(
                                model = poster,
                                contentDescription = item.resolvedTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Metadata & Action Controls
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                            if (item.voteAverage > 0.0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = CineFlixColors.AmberRating,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.1f", item.voteAverage),
                                        color = CineFlixColors.AmberRating,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text("·", color = CineFlixColors.TextMuted)
                            }
                            Text(
                                text = item.resolvedYear,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            item.formattedRuntimeOrSeasons()?.let { meta ->
                                Text("·", color = CineFlixColors.TextMuted)
                                Text(
                                    text = meta,
                                    color = CineFlixColors.TextSecondary,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Text(
                            text = item.resolvedTitle,
                            color = Color.White,
                            fontSize = 38.sp,
                            lineHeight = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (!item.tagline.isNullOrBlank()) {
                            Text(
                                text = "\"${item.tagline}\"",
                                color = CineFlixColors.TextSecondary,
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }

                        val genreLine = item.genres.joinToString(" / ") { it.name }
                        val creditLine = when {
                            directors.isNotEmpty() -> "Directed by ${directors.joinToString(", ")}"
                            creators.isNotEmpty() -> "Created by ${creators.joinToString(", ")}"
                            else -> ""
                        }
                        if (genreLine.isNotEmpty() || creditLine.isNotEmpty()) {
                            Text(
                                text = listOf(genreLine, creditLine).filter { it.isNotEmpty() }.joinToString("  ·  "),
                                color = CineFlixColors.TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = item.overview.orEmpty().ifBlank { "No synopsis available on TMDB." },
                            color = Color(0xFFE4E4E7),
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TvActionButton(
                                text = if (mediaType == "tv") "Watch S${state.selectedSeason} E1" else "Watch Now",
                                icon = Icons.Default.PlayArrow,
                                onClick = { onWatchNow(item, state.selectedSeason, 1) },
                                isPrimary = true
                            )
                            TvActionButton(
                                text = "Official Trailer",
                                icon = Icons.Default.Theaters,
                                onClick = { onPlayTrailer(item, bestTrailerKey) },
                                isPrimary = false
                            )
                            TvActionButton(
                                text = if (isInWatchlist) "In My List" else "Add to My List",
                                icon = if (isInWatchlist) Icons.Default.Check else Icons.Default.Add,
                                onClick = { onToggleWatchlist(item) },
                                isPrimary = false
                            )
                            TvActionButton(
                                text = activeServer.shortLabel,
                                icon = Icons.Default.Dns,
                                onClick = onCycleServer,
                                isPrimary = false
                            )
                        }
                    }
                }
            }
        }

        // 2. TV Series Season & Episode Selector
        if (mediaType == "tv") {
            val totalSeasons = (item.numberOfSeasons ?: 1).coerceIn(1, 40)
            item(key = "tv_episodes_section") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 36.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Episodes",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items((1..totalSeasons).toList(), key = { "season_$it" }) { seasonNum ->
                                TvFilterChip(
                                    label = "Season $seasonNum",
                                    selected = state.selectedSeason == seasonNum,
                                    onClick = { onSelectSeason(seasonNum) }
                                )
                            }
                        }
                    }

                    if (state.isLoadingEpisodes) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CineFlixColors.Crimson)
                        }
                    } else if (state.episodes.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 36.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.episodes, key = { "ep_${it.seasonNumber}_${it.episodeNumber}" }) { ep ->
                                TvEpisodeCard(
                                    episode = ep,
                                    isCurrentEpisode = false,
                                    onClick = {
                                        onWatchNow(item, state.selectedSeason, ep.episodeNumber)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Cast & Crew Rail
        if (state.cast.isNotEmpty()) {
            item(key = "cast_rail") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Top Cast",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 36.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 36.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.cast.take(14), key = { "cast_${it.id}_${it.character}" }) { member ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(124.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CineFlixColors.SurfaceCard)
                                    .border(1.dp, CineFlixColors.BorderSubtle, RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E1E28)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val profile = member.profileUrl("w185")
                                    if (profile != null) {
                                        AsyncImage(
                                            model = profile,
                                            contentDescription = member.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = member.name.take(1),
                                            color = Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = member.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!member.character.isNullOrBlank()) {
                                    Text(
                                        text = member.character,
                                        color = CineFlixColors.TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. More Like This (Recommended / Similar)
        val relatedItems = if (relatedTab == "recommended") {
            state.recommended.ifEmpty { state.similar }
        } else {
            state.similar.ifEmpty { state.recommended }
        }

        if (relatedItems.isNotEmpty()) {
            item(key = "related_rail") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 36.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "More Like This",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TvFilterChip(
                                label = "Recommended",
                                selected = relatedTab == "recommended",
                                onClick = { relatedTab = "recommended" }
                            )
                            TvFilterChip(
                                label = "Similar Titles",
                                selected = relatedTab == "similar",
                                onClick = { relatedTab = "similar" }
                            )
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 36.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(relatedItems, key = { "rel_${it.resolvedMediaType()}_${it.id}" }) { rel ->
                            TvPosterCard(
                                item = rel,
                                onClick = { onOpenRelatedTitle(rel) }
                            )
                        }
                    }
                }
            }
        }
    }
}
