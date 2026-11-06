package com.cineflix.tv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cineflix.tv.data.model.EpisodeDto
import com.cineflix.tv.data.model.MediaItem
import com.cineflix.tv.ui.theme.CineFlixColors
import java.util.Locale

enum class TvDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Default.Home),
    MOVIES("movies", "Movies", Icons.Default.Movie),
    TV("tv", "TV Shows", Icons.Default.Tv),
    TRENDING("trending", "Trending", Icons.Default.LocalFireDepartment),
    MY_LIST("my_list", "My List", Icons.Default.Bookmark),
    SEARCH("search", "Search", Icons.Default.Search),
    SETTINGS("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun TvNavigationRail(
    currentDestination: TvDestination,
    watchlistCount: Int,
    onNavigate: (TvDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(216.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xF508080A),
                        Color(0xE008080A),
                        Color(0x0008080A)
                    )
                )
            )
            .padding(start = 24.dp, end = 16.dp, top = 28.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Brand Lockup
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CineFlixColors.Crimson),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "CINEFLIX",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "BY MJ · GOOGLE TV",
                    color = CineFlixColors.TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.8.sp
                )
            }
        }

        // Primary Navigation Destinations
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TvDestination.entries.forEach { dest ->
                val selected = dest == currentDestination
                val interactionSource = remember { MutableInteractionSource() }
                val isFocused by interactionSource.collectIsFocusedAsState()

                val scale by animateFloatAsState(
                    targetValue = if (isFocused) 1.05f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "navScale"
                )
                val bgColor by animateColorAsState(
                    targetValue = when {
                        isFocused -> CineFlixColors.Crimson
                        selected -> Color(0x29E50914)
                        else -> Color.Transparent
                    },
                    label = "navBg"
                )
                val textColor by animateColorAsState(
                    targetValue = when {
                        isFocused -> Color.White
                        selected -> Color.White
                        else -> CineFlixColors.TextSecondary
                    },
                    label = "navText"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(scale)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .then(
                            if (selected && !isFocused) {
                                Modifier.border(
                                    width = 1.dp,
                                    color = Color(0x66E50914),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            } else Modifier
                        )
                        .focusable(interactionSource = interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onNavigate(dest) }
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = dest.label,
                        tint = if (selected && !isFocused) CineFlixColors.Crimson else textColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    val displayLabel = if (dest == TvDestination.MY_LIST && watchlistCount > 0) {
                        "My List ($watchlistCount)"
                    } else dest.label
                    Text(
                        text = displayLabel,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = if (selected || isFocused) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Quiet D-Pad Navigation Hint
        Text(
            text = "D-Pad · OK · Back",
            color = CineFlixColors.TextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
fun TvActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.07f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btnScale"
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            isPrimary && isFocused -> CineFlixColors.CrimsonHover
            isPrimary -> CineFlixColors.Crimson
            isFocused -> Color.White
            else -> Color(0x26FFFFFF)
        },
        label = "btnBg"
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            !isPrimary && isFocused -> Color.Black
            else -> Color.White
        },
        label = "btnText"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.5.dp else 1.dp,
        label = "btnBorder"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(
            width = borderWidth,
            color = if (isFocused) Color.White else Color(0x29FFFFFF)
        ),
        shadowElevation = if (isFocused) 14.dp else 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TvFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.07f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chipScale"
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color.White
            selected -> CineFlixColors.Crimson
            else -> CineFlixColors.SurfaceCard
        },
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color.Black
            selected -> Color.White
            else -> CineFlixColors.TextSecondary
        },
        label = "chipText"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) CineFlixColors.Crimson else Color(0x1FFFFFFF)
        )
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (selected || isFocused) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

@Composable
fun TvPosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 172.dp,
    progressFraction: Float? = null,
    episodeLabel: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.07f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "posterScale"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 3.dp else 1.dp,
        label = "posterBorder"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) CineFlixColors.Crimson else Color(0x1AFFFFFF),
        label = "posterBorderColor"
    )

    Column(
        modifier = modifier
            .width(cardWidth)
            .scale(scale)
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(14.dp))
                .background(CineFlixColors.SurfaceCard)
                .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
        ) {
            val poster = item.posterUrl("w500")
            if (poster != null) {
                AsyncImage(
                    model = poster,
                    contentDescription = item.resolvedTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1F1215), Color(0xFF0E0E12))
                            )
                        )
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.resolvedTitle,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Bottom contrast scrim for progress bar or focus highlight
            if (progressFraction != null || isFocused) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xE6080808))
                            )
                        )
                )
            }

            if (episodeLabel != null) {
                Text(
                    text = episodeLabel,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 10.dp, bottom = if (progressFraction != null) 12.dp else 8.dp)
                )
            }

            if (progressFraction != null && progressFraction > 0f) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    color = CineFlixColors.Crimson,
                    trackColor = Color(0x55FFFFFF),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.resolvedTitle,
            color = if (isFocused) Color.White else CineFlixColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (item.voteAverage > 0.0) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = CineFlixColors.AmberRating,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = String.format(Locale.US, "%.1f", item.voteAverage),
                    color = CineFlixColors.AmberRating,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "·",
                    color = CineFlixColors.TextMuted,
                    fontSize = 12.sp
                )
            }
            Text(
                text = item.resolvedYear,
                color = CineFlixColors.TextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "·",
                color = CineFlixColors.TextMuted,
                fontSize = 12.sp
            )
            Text(
                text = if (item.resolvedMediaType() == "tv") "Series" else "Movie",
                color = CineFlixColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun TvEpisodeCard(
    episode: EpisodeDto,
    isCurrentEpisode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "epScale"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color.White
            isCurrentEpisode -> CineFlixColors.Crimson
            else -> Color(0x1FFFFFFF)
        },
        label = "epBorder"
    )

    Column(
        modifier = modifier
            .width(280.dp)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isFocused) CineFlixColors.SurfaceFocused else CineFlixColors.SurfaceCard)
            .border(
                width = if (isFocused || isCurrentEpisode) 2.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color(0xFF0A0A0E))
        ) {
            val still = episode.stillUrl("w500")
            if (still != null) {
                AsyncImage(
                    model = still,
                    contentDescription = episode.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xCC080808))
                        )
                    )
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "E${episode.episodeNumber}",
                    color = CineFlixColors.Crimson,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                )
                if (episode.runtime != null && episode.runtime > 0) {
                    Text(
                        text = "${episode.runtime}m",
                        color = CineFlixColors.TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = episode.name.ifBlank { "Episode ${episode.episodeNumber}" },
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = episode.overview.orEmpty().ifBlank { "Stream in HD on CineFlix Google TV." },
                color = CineFlixColors.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
