package com.cineflix.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cineflix.tv.data.model.MediaItem
import com.cineflix.tv.ui.components.TvActionButton
import com.cineflix.tv.ui.components.TvFilterChip
import com.cineflix.tv.ui.components.TvPosterCard
import com.cineflix.tv.ui.theme.CineFlixColors

data class SearchUiState(
    val query: String = "",
    val typeFilter: String = "all", // all | movie | tv
    val sortBy: String = "personalized", // personalized | popularity | rating | year
    val results: List<MediaItem> = emptyList(),
    val trendingSuggestions: List<MediaItem> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null
)

@Composable
fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onTypeFilterChange: (String) -> Unit,
    onSortByChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDetails: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.errorMessage != null && state.results.isEmpty() && state.trendingSuggestions.isEmpty()) {
        TvErrorView(
            message = state.errorMessage,
            onRetry = onRetry,
            onOpenSettings = onOpenSettings,
            modifier = modifier
        )
        return
    }

    val filteredResults = remember(state.results, state.typeFilter, state.sortBy) {
        var list = state.results
        if (state.typeFilter != "all") {
            list = list.filter { it.resolvedMediaType() == state.typeFilter }
        }
        when (state.sortBy) {
            "popularity" -> list.sortedByDescending { it.popularity }
            "rating" -> list.sortedByDescending { it.voteAverage }
            "year" -> list.sortedByDescending { it.releaseDate ?: it.firstAirDate ?: "" }
            else -> list
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CineFlixColors.ObsidianBg)
            .padding(top = 24.dp)
    ) {
        // Search Input + D-Pad Quick Keys
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = CineFlixColors.Crimson
                        )
                    },
                    placeholder = {
                        Text(
                            text = "Search movies, series, sci-fi, directors...",
                            color = CineFlixColors.TextMuted,
                            fontSize = 15.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CineFlixColors.Crimson,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = CineFlixColors.SurfaceElevated,
                        unfocusedContainerColor = CineFlixColors.SurfaceElevated
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )

                // Type Filter Chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all" to "All", "movie" to "Movies", "tv" to "TV Shows").forEach { (k, label) ->
                        TvFilterChip(
                            label = label,
                            selected = state.typeFilter == k,
                            onClick = { onTypeFilterChange(k) }
                        )
                    }
                }
            }

            // Compact 10-Foot D-Pad On-Screen Keyboard Strip for Remote-Only Navigation
            TvRemoteQuickKeyboard(
                onAppendChar = { ch -> onQueryChange(state.query + ch) },
                onBackspace = {
                    if (state.query.isNotEmpty()) {
                        onQueryChange(state.query.dropLast(1))
                    }
                },
                onClear = { onQueryChange("") }
            )

            // Quick Trending Query Chips + Sort Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.query.isBlank() && state.trendingSuggestions.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(state.trendingSuggestions.take(6), key = { "q_${it.id}" }) { item ->
                            TvFilterChip(
                                label = item.resolvedTitle,
                                selected = false,
                                onClick = { onQueryChange(item.resolvedTitle) }
                            )
                        }
                    }
                } else {
                    Text(
                        text = if (state.isLoading) "Searching TMDB..."
                        else "${filteredResults.size} result${if (filteredResults.size == 1) "" else "s"} for \"${state.query}\"",
                        color = CineFlixColors.TextSecondary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "personalized" to "Best Match",
                        "popularity" to "Popular",
                        "rating" to "Top Rated",
                        "year" to "Newest"
                    ).forEach { (k, label) ->
                        TvFilterChip(
                            label = label,
                            selected = state.sortBy == k,
                            onClick = { onSortByChange(k) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Results Grid or Popular Suggestions
        if (state.isLoading && filteredResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CineFlixColors.Crimson)
            }
        } else if (state.query.isBlank()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Popular & Trending on CineFlix",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    contentPadding = PaddingValues(start = 32.dp, end = 40.dp, bottom = 40.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.trendingSuggestions,
                        key = { "sug_${it.resolvedMediaType()}_${it.id}" }
                    ) { item ->
                        TvPosterCard(
                            item = item,
                            cardWidth = 176.dp,
                            onClick = { onOpenDetails(item) }
                        )
                    }
                }
            }
        } else if (filteredResults.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No movies or TV series found for \"${state.query}\".",
                    color = CineFlixColors.TextSecondary,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(start = 32.dp, end = 40.dp, bottom = 40.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = filteredResults,
                    key = { "res_${it.resolvedMediaType()}_${it.id}" }
                ) { item ->
                    TvPosterCard(
                        item = item,
                        cardWidth = 176.dp,
                        onClick = { onOpenDetails(item) }
                    )
                }

                if (state.page < state.totalPages) {
                    item(span = { GridItemSpan(5) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isLoadingMore) {
                                CircularProgressIndicator(color = CineFlixColors.Crimson)
                            } else {
                                TvActionButton(
                                    text = "Load More Results",
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

@Composable
private fun TvRemoteQuickKeyboard(
    onAppendChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit
) {
    val keys = remember {
        listOf(
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
            "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
        )
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(keys, key = { "key_$it" }) { ch ->
            KeyboardKeyButton(label = ch, onClick = { onAppendChar(ch.lowercase()) })
        }
        item(key = "key_space") {
            KeyboardIconButton(
                icon = Icons.Default.SpaceBar,
                contentDescription = "Space",
                onClick = { onAppendChar(" ") }
            )
        }
        item(key = "key_backspace") {
            KeyboardIconButton(
                icon = Icons.Default.Backspace,
                contentDescription = "Backspace",
                onClick = onBackspace
            )
        }
        item(key = "key_clear") {
            KeyboardIconButton(
                icon = Icons.Default.Clear,
                contentDescription = "Clear",
                onClick = onClear
            )
        }
    }
}

@Composable
private fun KeyboardKeyButton(
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale = if (isFocused) 1.12f else 1f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 34.dp, height = 34.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) CineFlixColors.Crimson else CineFlixColors.SurfaceCard)
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) Color.White else Color(0x1AFFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun KeyboardIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale = if (isFocused) 1.1f else 1f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 44.dp, height = 34.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) Color.White else CineFlixColors.SurfaceCard)
            .border(
                width = 1.dp,
                color = if (isFocused) CineFlixColors.Crimson else Color(0x26FFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) Color.Black else Color.White,
            modifier = Modifier.size(17.dp)
        )
    }
}
