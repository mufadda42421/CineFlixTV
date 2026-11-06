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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cineflix.tv.data.model.CloudSyncAccount
import com.cineflix.tv.data.model.MediaItem
import com.cineflix.tv.data.model.WatchHistoryEntry
import com.cineflix.tv.data.model.WatchlistEntry
import com.cineflix.tv.ui.components.TvActionButton
import com.cineflix.tv.ui.components.TvFilterChip
import com.cineflix.tv.ui.components.TvPosterCard
import com.cineflix.tv.ui.theme.CineFlixColors

@Composable
fun LibraryScreen(
    watchlist: List<WatchlistEntry>,
    history: List<WatchHistoryEntry>,
    cloudAccount: CloudSyncAccount?,
    onOpenDetails: (MediaItem) -> Unit,
    onResumeHistory: (WatchHistoryEntry) -> Unit,
    onRemoveWatchlist: (WatchlistEntry) -> Unit,
    onClearHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf("watchlist") } // watchlist | history
    var mediaFilter by remember { mutableStateOf("all") } // all | movie | tv

    val filteredWatchlist = remember(watchlist, mediaFilter) {
        if (mediaFilter == "all") watchlist else watchlist.filter { it.mediaType == mediaFilter }
    }
    val filteredHistory = remember(history, mediaFilter) {
        if (mediaFilter == "all") history else history.filter { it.mediaType == mediaFilter }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CineFlixColors.ObsidianBg)
            .padding(top = 28.dp)
    ) {
        // Cloud Library Header Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CineFlixColors.SurfaceElevated)
                .border(1.dp, CineFlixColors.BorderSubtle, RoundedCornerShape(18.dp))
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (cloudAccount != null) Icons.Default.CloudDone else Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    tint = if (cloudAccount != null) CineFlixColors.EmeraldMatch else CineFlixColors.Crimson,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = if (cloudAccount != null) {
                            "${cloudAccount.displayName.ifBlank { "Member" }}'s Synced Library"
                        } else {
                            "My List & Watch History"
                        },
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (cloudAccount != null) {
                            "Synced with CineFlix Cloud (${cloudAccount.email})"
                        } else {
                            "Saved locally on Google TV. Sign in via Settings to sync with CineFlix Web."
                        },
                        color = CineFlixColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (activeTab == "history" && history.isNotEmpty()) {
                    TvActionButton(
                        text = "Clear History",
                        icon = Icons.Default.DeleteOutline,
                        onClick = onClearHistory,
                        isPrimary = false
                    )
                }
                TvActionButton(
                    text = if (cloudAccount != null) "Sync Settings" else "Cloud Sign In",
                    onClick = onOpenSettings,
                    isPrimary = cloudAccount == null
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tab & Type Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TvFilterChip(
                    label = "My List (${watchlist.size})",
                    selected = activeTab == "watchlist",
                    onClick = { activeTab = "watchlist" }
                )
                TvFilterChip(
                    label = "Viewing History (${history.size})",
                    selected = activeTab == "history",
                    onClick = { activeTab = "history" }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "All", "movie" to "Movies", "tv" to "TV Shows").forEach { (k, label) ->
                    TvFilterChip(
                        label = label,
                        selected = mediaFilter == k,
                        onClick = { mediaFilter = k }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (activeTab == "watchlist") {
            if (filteredWatchlist.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Your My List is empty. Select '+ My List' on any movie or TV series to save it here.",
                        color = CineFlixColors.TextSecondary,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    contentPadding = PaddingValues(start = 32.dp, end = 40.dp, bottom = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredWatchlist, key = { "wl_${it.mediaType}_${it.id}" }) { entry ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TvPosterCard(
                                item = entry.toMediaItem(),
                                cardWidth = 176.dp,
                                onClick = { onOpenDetails(entry.toMediaItem()) }
                            )
                            TvFilterChip(
                                label = "Remove",
                                selected = false,
                                onClick = { onRemoveWatchlist(entry) }
                            )
                        }
                    }
                }
            }
        } else {
            if (filteredHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No viewing history yet. Start watching any title to track resume progress.",
                        color = CineFlixColors.TextSecondary,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    contentPadding = PaddingValues(start = 32.dp, end = 40.dp, bottom = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredHistory, key = { "vh_${it.mediaType}_${it.id}" }) { entry ->
                        val epLabel = if (entry.mediaType == "tv") {
                            "S${entry.seasonNumber} · E${entry.episodeNumber}"
                        } else null
                        TvPosterCard(
                            item = entry.toMediaItem(),
                            cardWidth = 176.dp,
                            progressFraction = entry.progressFraction.coerceAtLeast(0.08f),
                            episodeLabel = epLabel,
                            onClick = { onResumeHistory(entry) }
                        )
                    }
                }
            }
        }
    }
}
