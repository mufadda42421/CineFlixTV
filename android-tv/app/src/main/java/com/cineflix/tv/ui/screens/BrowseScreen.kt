package com.cineflix.tv.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cineflix.tv.data.model.GenreDto
import com.cineflix.tv.data.model.MediaItem
import com.cineflix.tv.ui.components.TvActionButton
import com.cineflix.tv.ui.components.TvFilterChip
import com.cineflix.tv.ui.components.TvPosterCard
import com.cineflix.tv.ui.theme.CineFlixColors

enum class BrowseMode(val title: String, val subtitle: String) {
    MOVIE("Explore Movies", "Theatrical releases, award-winning films, and genre classics from TMDB"),
    TV("Explore TV Shows", "Acclaimed television series, miniseries, and global originals"),
    TRENDING("Trending Today", "Real-time daily trending movies and series across the globe")
}

data class BrowseUiState(
    val mode: BrowseMode = BrowseMode.MOVIE,
    val selectedGenreId: Int? = null,
    val sortBy: String = "popularity.desc",
    val items: List<MediaItem> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null
)

@Composable
fun BrowseScreen(
    state: BrowseUiState,
    genres: List<GenreDto>,
    onSelectGenre: (Int?) -> Unit,
    onSelectSort: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDetails: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading && state.items.isEmpty()) {
        TvShimmerLoadingView(modifier = modifier)
        return
    }

    if (state.errorMessage != null && state.items.isEmpty()) {
        TvErrorView(
            message = state.errorMessage,
            onRetry = onRetry,
            onOpenSettings = onOpenSettings,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CineFlixColors.ObsidianBg)
            .padding(top = 28.dp)
    ) {
        // Header + Sort Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = state.mode.title,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.mode.subtitle,
                    color = CineFlixColors.TextSecondary,
                    fontSize = 14.sp
                )
            }

            if (state.mode != BrowseMode.TRENDING) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val sortOptions = listOf(
                        "popularity.desc" to "Most Popular",
                        "vote_average.desc" to "Top Rated",
                        "release_date.desc" to "Newest"
                    )
                    sortOptions.forEach { (key, label) ->
                        TvFilterChip(
                            label = label,
                            selected = state.sortBy == key,
                            onClick = { onSelectSort(key) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Horizontal Genre Filter Strip
        LazyRow(
            contentPadding = PaddingValues(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "genre_all") {
                TvFilterChip(
                    label = "All Genres",
                    selected = state.selectedGenreId == null,
                    onClick = { onSelectGenre(null) }
                )
            }
            items(genres, key = { "genre_${it.id}" }) { genre ->
                TvFilterChip(
                    label = genre.name,
                    selected = state.selectedGenreId == genre.id,
                    onClick = { onSelectGenre(genre.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (state.items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No titles match this genre filter. Try selecting All Genres.",
                    color = CineFlixColors.TextSecondary,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(start = 32.dp, end = 40.dp, bottom = 48.dp, top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = state.items,
                    key = { "${state.mode.name}_${it.resolvedMediaType()}_${it.id}" }
                ) { item ->
                    TvPosterCard(
                        item = item,
                        cardWidth = 178.dp,
                        onClick = { onOpenDetails(item) }
                    )
                }

                if (state.page < state.totalPages) {
                    item(span = { GridItemSpan(5) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isLoadingMore) {
                                CircularProgressIndicator(color = CineFlixColors.Crimson)
                            } else {
                                TvActionButton(
                                    text = "Load More Titles (Page ${state.page + 1})",
                                    icon = Icons.Default.ExpandMore,
                                    onClick = onLoadMore,
                                    isPrimary = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
