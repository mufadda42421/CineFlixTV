package com.cineflix.tv.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.cineflix.tv.data.model.AudioTrackOption
import com.cineflix.tv.data.model.CineFlixServers
import com.cineflix.tv.data.model.EpisodeDto
import com.cineflix.tv.data.model.MediaItem
import com.cineflix.tv.data.model.PlaybackEngineMode
import com.cineflix.tv.data.model.StreamServerKind
import com.cineflix.tv.data.model.SubtitleTrackOption
import com.cineflix.tv.ui.components.TvActionButton
import com.cineflix.tv.ui.components.TvFilterChip
import com.cineflix.tv.ui.theme.CineFlixColors
import kotlinx.coroutines.delay
import java.util.Locale

data class ActivePlayerSession(
    val item: MediaItem,
    val mediaType: String,
    val seasonNumber: Int = 1,
    val episodeNumber: Int = 1,
    val serverId: String = "vidsrc_pm",
    val customDirectUrl: String = "",
    val youtubeTrailerKey: String? = null,
    val initialProgressSeconds: Long = 0L,
    val episodes: List<EpisodeDto> = emptyList()
)

private enum class PlayerOverlayPanel {
    NONE,
    SERVERS,
    AUDIO_TRACKS,
    SUBTITLE_TRACKS,
    EPISODES
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    session: ActivePlayerSession,
    autoFailover: Boolean,
    onSelectServer: (String) -> Unit,
    onSelectEpisode: (Int, Int) -> Unit,
    onUpdateCustomUrl: (String) -> Unit,
    onRecordProgress: (Long, Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Keep TV screen awake during playback
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val activeServer = remember(session.serverId) {
        CineFlixServers.findById(session.serverId)
    }

    val resolvedUrl = remember(
        session.item.id,
        session.mediaType,
        session.seasonNumber,
        session.episodeNumber,
        session.serverId,
        session.customDirectUrl,
        session.youtubeTrailerKey
    ) {
        activeServer.urlBuilder(
            session.item.id,
            session.mediaType,
            session.seasonNumber,
            session.episodeNumber,
            session.customDirectUrl,
            session.youtubeTrailerKey
        )
    }

    var sniffedDirectStreamUrl by remember(resolvedUrl) { mutableStateOf<String?>(null) }
    var forceWebViewFallback by remember(resolvedUrl) { mutableStateOf(false) }

    val effectiveDirectUrl = remember(resolvedUrl, sniffedDirectStreamUrl, activeServer.kind) {
        when {
            activeServer.kind == StreamServerKind.DIRECT_MEDIA -> resolvedUrl
            isDirectPlayableMediaUrl(resolvedUrl) -> resolvedUrl
            else -> sniffedDirectStreamUrl
        }
    }

    val engineMode = remember(effectiveDirectUrl, forceWebViewFallback) {
        if (!forceWebViewFallback && !effectiveDirectUrl.isNullOrBlank()) {
            PlaybackEngineMode.EXOPLAYER_NATIVE
        } else {
            PlaybackEngineMode.HYBRID_WEBVIEW
        }
    }

    var showHud by remember { mutableStateOf(true) }
    var activePanel by remember { mutableStateOf(PlayerOverlayPanel.NONE) }
    var statusNotice by remember { mutableStateOf<String?>(null) }
    var customUrlDraft by remember(session.customDirectUrl) { mutableStateOf(session.customDirectUrl) }

    // Auto-hide HUD when playing and no modal panel is open
    LaunchedEffect(showHud, activePanel, engineMode) {
        if (showHud && activePanel == PlayerOverlayPanel.NONE && engineMode == PlaybackEngineMode.EXOPLAYER_NATIVE) {
            delay(5000L)
            showHud = false
        }
    }

    // Auto-failover watchdog for external embeds
    var embedLoaded by remember(resolvedUrl) { mutableStateOf(false) }
    LaunchedEffect(resolvedUrl, engineMode, autoFailover) {
        if (engineMode == PlaybackEngineMode.HYBRID_WEBVIEW &&
            activeServer.kind == StreamServerKind.EXTERNAL_EMBED &&
            autoFailover &&
            !embedLoaded
        ) {
            delay(9000L)
            if (!embedLoaded) {
                val next = CineFlixServers.nextServer(activeServer.id)
                statusNotice = "Slow response on ${activeServer.shortLabel}. Switched to ${next.shortLabel}."
                onSelectServer(next.id)
            }
        }
    }

    BackHandler {
        if (activePanel != PlayerOverlayPanel.NONE) {
            activePanel = PlayerOverlayPanel.NONE
        } else {
            onClose()
        }
    }

    val currentEpisodeObj = remember(session.episodes, session.episodeNumber) {
        session.episodes.find { it.episodeNumber == session.episodeNumber }
    }

    // Track state shared from ExoPlayer
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(session.initialProgressSeconds * 1000L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var audioTracks by remember { mutableStateOf<List<AudioTrackOption>>(emptyList()) }
    var subtitleTracks by remember { mutableStateOf<List<SubtitleTrackOption>>(emptyList()) }
    var exoPlayerRef by remember { mutableStateOf<ExoPlayer?>(null) }
    var trackSelectorRef by remember { mutableStateOf<DefaultTrackSelector?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                when (keyEvent.key) {
                    Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
                        if (engineMode == PlaybackEngineMode.EXOPLAYER_NATIVE) {
                            exoPlayerRef?.let { p ->
                                if (p.isPlaying) p.pause() else p.play()
                            }
                        } else {
                            webViewRef?.evaluateJavascript(
                                "(function(){var v=document.querySelector('video');if(v){v.paused?v.play():v.pause();}})();",
                                null
                            )
                        }
                        showHud = true
                        true
                    }

                    Key.MediaFastForward, Key.MediaStepForward -> {
                        if (engineMode == PlaybackEngineMode.EXOPLAYER_NATIVE) {
                            exoPlayerRef?.let { p ->
                                val target = (p.currentPosition + 10_000L).coerceAtMost(p.duration.coerceAtLeast(0L))
                                p.seekTo(target)
                            }
                        } else {
                            webViewRef?.evaluateJavascript(
                                "(function(){var v=document.querySelector('video');if(v){v.currentTime+=10;}})();",
                                null
                            )
                        }
                        showHud = true
                        true
                    }

                    Key.MediaRewind, Key.MediaStepBackward -> {
                        if (engineMode == PlaybackEngineMode.EXOPLAYER_NATIVE) {
                            exoPlayerRef?.let { p ->
                                val target = (p.currentPosition - 10_000L).coerceAtLeast(0L)
                                p.seekTo(target)
                            }
                        } else {
                            webViewRef?.evaluateJavascript(
                                "(function(){var v=document.querySelector('video');if(v){v.currentTime=Math.max(0,v.currentTime-10);}})();",
                                null
                            )
                        }
                        showHud = true
                        true
                    }

                    Key.Menu, Key.Info -> {
                        showHud = !showHud
                        true
                    }

                    Key.DirectionCenter, Key.Enter -> {
                        if (engineMode == PlaybackEngineMode.EXOPLAYER_NATIVE && !showHud && activePanel == PlayerOverlayPanel.NONE) {
                            exoPlayerRef?.let { p ->
                                if (p.isPlaying) p.pause() else p.play()
                            }
                            showHud = true
                            true
                        } else {
                            false
                        }
                    }

                    Key.DirectionLeft -> {
                        if (engineMode == PlaybackEngineMode.EXOPLAYER_NATIVE && !showHud && activePanel == PlayerOverlayPanel.NONE) {
                            exoPlayerRef?.let { p ->
                                p.seekTo((p.currentPosition - 10_000L).coerceAtLeast(0L))
                            }
                            showHud = true
                            true
                        } else {
                            false
                        }
                    }

                    Key.DirectionRight -> {
                        if (engineMode == PlaybackEngineMode.EXOPLAYER_NATIVE && !showHud && activePanel == PlayerOverlayPanel.NONE) {
                            exoPlayerRef?.let { p ->
                                p.seekTo((p.currentPosition + 10_000L).coerceAtMost(p.duration.coerceAtLeast(0L)))
                            }
                            showHud = true
                            true
                        } else {
                            false
                        }
                    }

                    else -> false
                }
            }
    ) {
        // 1. Playback Surface (Native Media3 ExoPlayer OR Hybrid TV WebView)
        if (engineMode == PlaybackEngineMode.EXOPLAYER_NATIVE && !effectiveDirectUrl.isNullOrBlank()) {
            val trackSelector = remember {
                DefaultTrackSelector(context).apply {
                    setParameters(
                        buildUponParameters()
                            .setPreferredAudioLanguage("en")
                            .setPreferredTextLanguage("en")
                    )
                }
            }
            val exoPlayer = remember(effectiveDirectUrl) {
                ExoPlayer.Builder(context)
                    .setTrackSelector(trackSelector)
                    .build()
                    .apply {
                        setMediaItem(Media3Item.fromUri(effectiveDirectUrl))
                        prepare()
                        if (session.initialProgressSeconds > 5L) {
                            seekTo(session.initialProgressSeconds * 1000L)
                        }
                        playWhenReady = true
                    }
            }

            DisposableEffect(exoPlayer) {
                exoPlayerRef = exoPlayer
                trackSelectorRef = trackSelector

                val listener = object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                        if (!playing) {
                            onRecordProgress(
                                exoPlayer.currentPosition / 1000L,
                                (exoPlayer.duration.coerceAtLeast(0L)) / 1000L
                            )
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isBuffering = playbackState == Player.STATE_BUFFERING
                        if (playbackState == Player.STATE_READY) {
                            durationMs = exoPlayer.duration.coerceAtLeast(0L)
                        }
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        audioTracks = extractAudioTracks(tracks)
                        subtitleTracks = extractSubtitleTracks(tracks)
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        // Seamless hybrid fallback if ExoPlayer encounters a non-direct or unsupported stream
                        if (activeServer.kind != StreamServerKind.DIRECT_MEDIA) {
                            statusNotice = "ExoPlayer fallback to Hybrid WebView (${error.errorCodeName})"
                            forceWebViewFallback = true
                        } else {
                            statusNotice = "Direct stream error (${error.errorCodeName}). Select another server or update URL."
                            activePanel = PlayerOverlayPanel.SERVERS
                            showHud = true
                        }
                    }
                }

                exoPlayer.addListener(listener)
                onDispose {
                    onRecordProgress(
                        exoPlayer.currentPosition / 1000L,
                        (exoPlayer.duration.coerceAtLeast(0L)) / 1000L
                    )
                    exoPlayer.removeListener(listener)
                    exoPlayer.release()
                    exoPlayerRef = null
                }
            }

            // Periodic position & progress sync
            LaunchedEffect(exoPlayer) {
                var tick = 0
                while (true) {
                    currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                    tick++
                    if (tick % 10 == 0 && exoPlayer.isPlaying) {
                        onRecordProgress(currentPositionMs / 1000L, durationMs / 1000L)
                    }
                    delay(1000L)
                }
            }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false // Custom 10-foot Google TV OSD overlay
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setBackgroundColor(AndroidColor.BLACK)
                        player = exoPlayer
                    }
                },
                update = { pv ->
                    if (pv.player !== exoPlayer) {
                        pv.player = exoPlayer
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isBuffering) {
                CircularProgressIndicator(
                    color = CineFlixColors.Crimson,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(52.dp)
                )
            }
        } else if (activeServer.kind == StreamServerKind.DIRECT_MEDIA && effectiveDirectUrl.isNullOrBlank()) {
            // Prompt to enter a direct MP4 / HLS stream URL when Server 6 is chosen with empty URL
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(620.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CineFlixColors.SurfaceElevated)
                    .border(1.dp, CineFlixColors.BorderSubtle, RoundedCornerShape(20.dp))
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Native Media3 ExoPlayer · Direct Stream",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Enter a direct .mp4, .m3u8 (HLS), or .mpd (DASH) URL to stream in hardware Media3 ExoPlayer, or switch to Server 4 (VidSrc Master Node).",
                    color = CineFlixColors.TextSecondary,
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = customUrlDraft,
                    onValueChange = { customUrlDraft = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "https://.../stream.m3u8",
                            color = CineFlixColors.TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CineFlixColors.Crimson,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvActionButton(
                        text = "Play in ExoPlayer",
                        icon = Icons.Default.PlayArrow,
                        onClick = {
                            if (customUrlDraft.isNotBlank()) {
                                onUpdateCustomUrl(customUrlDraft.trim())
                            }
                        },
                        isPrimary = true
                    )
                    TvActionButton(
                        text = "Use Server 4 (VidSrc)",
                        icon = Icons.Default.Dns,
                        onClick = { onSelectServer("vidsrc_pm") },
                        isPrimary = false
                    )
                }
            }
        } else {
            // Hybrid TV WebView Player (Cloud Embeds + Official TMDB Trailers + Fallback)
            HybridTvWebView(
                url = resolvedUrl,
                onPageFinished = { embedLoaded = true },
                onDirectStreamSniffed = { streamUrl ->
                    if (sniffedDirectStreamUrl == null && activeServer.kind == StreamServerKind.EXTERNAL_EMBED) {
                        sniffedDirectStreamUrl = streamUrl
                        statusNotice = "Direct media stream detected! Playing in Native Media3 ExoPlayer."
                    }
                },
                onWebViewCreated = { wv -> webViewRef = wv },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Top & Bottom 10-Foot Google TV OSD Controls
        AnimatedVisibility(
            visible = showHud || engineMode == PlaybackEngineMode.HYBRID_WEBVIEW,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Control Strip
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xE6080808), Color(0x80080808), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 28.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        TvActionButton(
                            text = "Back",
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            onClick = onClose,
                            isPrimary = false
                        )
                        Column {
                            val titleText = if (session.mediaType == "tv") {
                                "${session.item.resolvedTitle}  ·  S${session.seasonNumber} E${session.episodeNumber}"
                            } else {
                                session.item.resolvedTitle
                            }
                            Text(
                                text = titleText,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val engineLabel = if (engineMode == PlaybackEngineMode.EXOPLAYER_NATIVE) {
                                "Media3 ExoPlayer · Hardware"
                            } else {
                                "${activeServer.name} · Hybrid WebView"
                            }
                            val subLine = listOfNotNull(
                                engineLabel,
                                currentEpisodeObj?.name?.takeIf { it.isNotBlank() }
                            ).joinToString("  ·  ")
                            Text(
                                text = subLine,
                                color = CineFlixColors.TextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Server, Audio, Subtitles, Episodes Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (engineMode == PlaybackEngineMode.EXOPLAYER_NATIVE) {
                            TvFilterChip(
                                label = "Audio (${audioTracks.size})",
                                selected = activePanel == PlayerOverlayPanel.AUDIO_TRACKS,
                                onClick = {
                                    activePanel = if (activePanel == PlayerOverlayPanel.AUDIO_TRACKS) {
                                        PlayerOverlayPanel.NONE
                                    } else PlayerOverlayPanel.AUDIO_TRACKS
                                }
                            )
                            TvFilterChip(
                                label = "Subtitles (${subtitleTracks.size})",
                                selected = activePanel == PlayerOverlayPanel.SUBTITLE_TRACKS,
                                onClick = {
                                    activePanel = if (activePanel == PlayerOverlayPanel.SUBTITLE_TRACKS) {
                                        PlayerOverlayPanel.NONE
                                    } else PlayerOverlayPanel.SUBTITLE_TRACKS
                                }
                            )
                        }

                        if (session.mediaType == "tv" && session.episodes.isNotEmpty()) {
                            TvFilterChip(
                                label = "Episodes",
                                selected = activePanel == PlayerOverlayPanel.EPISODES,
                                onClick = {
                                    activePanel = if (activePanel == PlayerOverlayPanel.EPISODES) {
                                        PlayerOverlayPanel.NONE
                                    } else PlayerOverlayPanel.EPISODES
                                }
                            )
                        }

                        TvActionButton(
                            text = activeServer.shortLabel,
                            icon = Icons.Default.Dns,
                            onClick = {
                                activePanel = if (activePanel == PlayerOverlayPanel.SERVERS) {
                                    PlayerOverlayPanel.NONE
                                } else PlayerOverlayPanel.SERVERS
                            },
                            isPrimary = true
                        )
                    }
                }

                // Status Notice Banner (e.g., Auto-Failover or ExoPlayer Upgrade)
                if (!statusNotice.isNullOrBlank()) {
                    Text(
                        text = statusNotice!!,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 86.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xCC14141C))
                            .border(1.dp, CineFlixColors.Crimson, RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Bottom Transport Bar for Native Media3 ExoPlayer
                if (engineMode == PlaybackEngineMode.EXOPLAYER_NATIVE && showHud) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xCC080808), Color(0xF5080808))
                                )
                            )
                            .padding(horizontal = 32.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val fraction = if (durationMs > 0L) {
                            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formatDurationMs(currentPositionMs),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            LinearProgressIndicator(
                                progress = { fraction },
                                color = CineFlixColors.Crimson,
                                trackColor = Color(0x44FFFFFF),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            Text(
                                text = formatDurationMs(durationMs),
                                color = CineFlixColors.TextSecondary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TvActionButton(
                                    text = "-10s",
                                    icon = Icons.Default.Replay10,
                                    onClick = {
                                        exoPlayerRef?.let { p ->
                                            p.seekTo((p.currentPosition - 10_000L).coerceAtLeast(0L))
                                        }
                                    },
                                    isPrimary = false
                                )
                                TvActionButton(
                                    text = if (isPlaying) "Pause" else "Play",
                                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    onClick = {
                                        exoPlayerRef?.let { p ->
                                            if (p.isPlaying) p.pause() else p.play()
                                        }
                                    },
                                    isPrimary = true
                                )
                                TvActionButton(
                                    text = "+10s",
                                    icon = Icons.Default.Forward10,
                                    onClick = {
                                        exoPlayerRef?.let { p ->
                                            p.seekTo((p.currentPosition + 10_000L).coerceAtMost(p.duration.coerceAtLeast(0L)))
                                        }
                                    },
                                    isPrimary = false
                                )
                                TvActionButton(
                                    text = "${playbackSpeed}x",
                                    icon = Icons.Default.Speed,
                                    onClick = {
                                        val speeds = listOf(1.0f, 1.25f, 1.5f, 0.75f)
                                        val next = speeds[(speeds.indexOf(playbackSpeed) + 1) % speeds.size]
                                        playbackSpeed = next
                                        exoPlayerRef?.setPlaybackSpeed(next)
                                    },
                                    isPrimary = false
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (session.mediaType == "tv" && session.episodeNumber < session.episodes.size) {
                                    TvActionButton(
                                        text = "Next Episode (E${session.episodeNumber + 1})",
                                        icon = Icons.Default.SkipNext,
                                        onClick = {
                                            onSelectEpisode(session.seasonNumber, session.episodeNumber + 1)
                                        },
                                        isPrimary = false
                                    )
                                }
                                TvActionButton(
                                    text = "WebView Mode",
                                    onClick = { forceWebViewFallback = true },
                                    isPrimary = false
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. D-Pad Side Drawers (Server Switcher / Audio Tracks / Subtitle Tracks / Episodes)
        if (activePanel != PlayerOverlayPanel.NONE) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(420.dp)
                    .background(Color(0xF50E0E12))
                    .border(1.dp, CineFlixColors.BorderSubtle)
                    .padding(24.dp)
            ) {
                when (activePanel) {
                    PlayerOverlayPanel.SERVERS -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Select Streaming Node",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(CineFlixServers.all, key = { it.id }) { srv ->
                                    val selected = srv.id == activeServer.id
                                    TvFilterChip(
                                        label = "${srv.name} (${srv.badge})",
                                        selected = selected,
                                        onClick = {
                                            forceWebViewFallback = false
                                            sniffedDirectStreamUrl = null
                                            onSelectServer(srv.id)
                                            activePanel = PlayerOverlayPanel.NONE
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    PlayerOverlayPanel.AUDIO_TRACKS -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Audiotrack, null, tint = CineFlixColors.Crimson)
                                Spacer(Modifier.width(8.dp))
                                Text("Audio Tracks", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            if (audioTracks.isEmpty()) {
                                Text(
                                    text = "Default audio stream active. No alternate audio tracks reported by this container.",
                                    color = CineFlixColors.TextSecondary,
                                    fontSize = 14.sp
                                )
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(audioTracks, key = { "${it.groupIndex}_${it.trackIndex}" }) { track ->
                                        TvFilterChip(
                                            label = track.label,
                                            selected = track.isSelected,
                                            onClick = {
                                                val player = exoPlayerRef
                                                val selector = trackSelectorRef
                                                if (player != null && selector != null) {
                                                    val group = player.currentTracks.groups.getOrNull(track.groupIndex)
                                                    if (group != null) {
                                                        selector.parameters = selector.buildUponParameters()
                                                            .setOverrideForType(
                                                                TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex)
                                                            )
                                                            .build()
                                                    }
                                                }
                                                activePanel = PlayerOverlayPanel.NONE
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    PlayerOverlayPanel.SUBTITLE_TRACKS -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ClosedCaption, null, tint = CineFlixColors.Crimson)
                                Spacer(Modifier.width(8.dp))
                                Text("Subtitles & Captions", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                item(key = "sub_off") {
                                    TvFilterChip(
                                        label = "Off (Disable Subtitles)",
                                        selected = subtitleTracks.none { it.isSelected },
                                        onClick = {
                                            trackSelectorRef?.let { selector ->
                                                selector.parameters = selector.buildUponParameters()
                                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                                    .build()
                                            }
                                            activePanel = PlayerOverlayPanel.NONE
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                items(subtitleTracks, key = { "${it.groupIndex}_${it.trackIndex}" }) { track ->
                                    TvFilterChip(
                                        label = track.label,
                                        selected = track.isSelected,
                                        onClick = {
                                            val player = exoPlayerRef
                                            val selector = trackSelectorRef
                                            if (player != null && selector != null) {
                                                val group = player.currentTracks.groups.getOrNull(track.groupIndex)
                                                if (group != null) {
                                                    selector.parameters = selector.buildUponParameters()
                                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                                        .setOverrideForType(
                                                            TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex)
                                                        )
                                                        .build()
                                                }
                                            }
                                            activePanel = PlayerOverlayPanel.NONE
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    PlayerOverlayPanel.EPISODES -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Season ${session.seasonNumber} Episodes",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(session.episodes, key = { it.episodeNumber }) { ep ->
                                    val selected = ep.episodeNumber == session.episodeNumber
                                    TvFilterChip(
                                        label = "E${ep.episodeNumber} · ${ep.name.ifBlank { "Episode ${ep.episodeNumber}" }}",
                                        selected = selected,
                                        onClick = {
                                            onSelectEpisode(session.seasonNumber, ep.episodeNumber)
                                            activePanel = PlayerOverlayPanel.NONE
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    PlayerOverlayPanel.NONE -> Unit
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HybridTvWebView(
    url: String,
    onPageFinished: () -> Unit,
    onDirectStreamSniffed: (String) -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var fullscreenView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(AndroidColor.BLACK)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(this@apply, true)
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setSupportMultipleWindows(false)
                        javaScriptCanOpenWindowsAutomatically = false
                        // Desktop Chrome UA ensures clean 16:9 widescreen theatre players rather than mobile UI
                        userAgentString =
                            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val target = request?.url?.toString().orEmpty()
                            val host = request?.url?.host.orEmpty().lowercase()
                            val allowedHosts = listOf(
                                "vidsrc.pm", "vidsrc.to", "vidsrc.sh", "vidsrc.me", "vidsrc.xyz",
                                "smashystream.xyz", "anyembed.xyz", "youtube-nocookie.com",
                                "youtube.com", "googlevideo.com", "cineflixbymj.vercel.app"
                            )
                            val isAllowed = allowedHosts.any { host == it || host.endsWith(".$it") }
                            if (!isAllowed && request?.isForMainFrame == true) {
                                // Block third-party ad-redirect tabs while preserving embed playback
                                return true
                            }
                            if (isDirectPlayableMediaUrl(target)) {
                                onDirectStreamSniffed(target)
                                return true
                            }
                            return false
                        }

                        override fun onLoadResource(view: WebView?, resourceUrl: String?) {
                            super.onLoadResource(view, resourceUrl)
                            if (resourceUrl != null && isDirectPlayableMediaUrl(resourceUrl)) {
                                onDirectStreamSniffed(resourceUrl)
                            }
                        }

                        override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                            super.onPageFinished(view, finishedUrl)
                            onPageFinished()
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            fullscreenView = view
                            customViewCallback = callback
                        }

                        override fun onHideCustomView() {
                            fullscreenView = null
                            customViewCallback?.onCustomViewHidden()
                            customViewCallback = null
                        }
                    }

                    onWebViewCreated(this)
                    if (url.isNotBlank()) {
                        loadUrl(url)
                    }
                }
            },
            update = { wv ->
                if (url.isNotBlank() && wv.url != url) {
                    wv.loadUrl(url)
                }
            },
            onRelease = { wv ->
                wv.stopLoading()
                wv.destroy()
            },
            modifier = Modifier.fillMaxSize()
        )

        if (fullscreenView != null) {
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        setBackgroundColor(AndroidColor.BLACK)
                        addView(
                            fullscreenView,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun isDirectPlayableMediaUrl(url: String): Boolean {
    val clean = url.substringBefore('?').lowercase(Locale.US)
    return clean.endsWith(".mp4") ||
        clean.endsWith(".m3u8") ||
        clean.endsWith(".mpd") ||
        clean.endsWith(".webm") ||
        clean.endsWith(".mkv") ||
        clean.endsWith(".mov") ||
        clean.endsWith(".ts")
}

private fun extractAudioTracks(tracks: Tracks): List<AudioTrackOption> {
    val result = mutableListOf<AudioTrackOption>()
    tracks.groups.forEachIndexed { groupIdx, group ->
        if (group.type == C.TRACK_TYPE_AUDIO) {
            for (trackIdx in 0 until group.length) {
                val format = group.getTrackFormat(trackIdx)
                val lang = format.language?.let { Locale(it).displayLanguage }?.takeIf { it.isNotBlank() }
                    ?: "Audio ${result.size + 1}"
                val channels = when (format.channelCount) {
                    6 -> "5.1 Surround"
                    8 -> "7.1 Surround"
                    2 -> "Stereo"
                    1 -> "Mono"
                    else -> ""
                }
                val label = listOfNotNull(
                    format.label?.takeIf { it.isNotBlank() } ?: lang,
                    channels.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                result += AudioTrackOption(
                    groupIndex = groupIdx,
                    trackIndex = trackIdx,
                    label = label,
                    language = lang,
                    channelCount = format.channelCount,
                    isSelected = group.isTrackSelected(trackIdx)
                )
            }
        }
    }
    return result
}

private fun extractSubtitleTracks(tracks: Tracks): List<SubtitleTrackOption> {
    val result = mutableListOf<SubtitleTrackOption>()
    tracks.groups.forEachIndexed { groupIdx, group ->
        if (group.type == C.TRACK_TYPE_TEXT) {
            for (trackIdx in 0 until group.length) {
                val format = group.getTrackFormat(trackIdx)
                val lang = format.language?.let { Locale(it).displayLanguage }?.takeIf { it.isNotBlank() }
                    ?: "Subtitle ${result.size + 1}"
                val label = format.label?.takeIf { it.isNotBlank() } ?: lang
                result += SubtitleTrackOption(
                    groupIndex = groupIdx,
                    trackIndex = trackIdx,
                    label = label,
                    language = lang,
                    isSelected = group.isTrackSelected(trackIdx)
                )
            }
        }
    }
    return result
}

private fun formatDurationMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
