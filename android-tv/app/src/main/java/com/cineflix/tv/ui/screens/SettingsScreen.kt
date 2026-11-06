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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cineflix.tv.data.local.AppPreferencesState
import com.cineflix.tv.data.model.CineFlixServers
import com.cineflix.tv.ui.components.TvActionButton
import com.cineflix.tv.ui.components.TvFilterChip
import com.cineflix.tv.ui.theme.CineFlixColors

@Composable
fun SettingsScreen(
    prefs: AppPreferencesState,
    companionUrl: String,
    isVerifyingKey: Boolean,
    keyFeedback: String?,
    cloudAuthFeedback: String?,
    isCloudAuthLoading: Boolean,
    onSaveAndVerifyTmdbKey: (String) -> Unit,
    onSelectPreferredServer: (String) -> Unit,
    onToggleAutoFailover: (Boolean) -> Unit,
    onToggleAutoplayTrailers: (Boolean) -> Unit,
    onSaveCustomStreamUrl: (String) -> Unit,
    onCloudSignIn: (String, String) -> Unit,
    onCloudSignUp: (String, String) -> Unit,
    onCloudSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tmdbInput by remember(prefs.tmdbCredential) { mutableStateOf(prefs.tmdbCredential) }
    var showTmdbKey by remember { mutableStateOf(false) }
    var customUrlInput by remember(prefs.customStreamUrl) { mutableStateOf(prefs.customStreamUrl) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CineFlixColors.ObsidianBg),
        contentPadding = PaddingValues(start = 32.dp, end = 48.dp, top = 28.dp, bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item(key = "settings_header") {
            Column {
                Text(
                    text = "Settings & Hybrid Player Configuration",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Manage TMDB credentials, streaming nodes, Media3 ExoPlayer direct URLs, and CineFlix Cloud sync.",
                    color = CineFlixColors.TextSecondary,
                    fontSize = 14.sp
                )
            }
        }

        // 1. TMDB API Credential (BYOK) & Phone Companion
        item(key = "settings_tmdb") {
            SettingsCard(
                title = "TMDB API Credential (BYOK)",
                icon = Icons.Default.Key
            ) {
                Text(
                    text = "Configure your TMDB v3 API Key or v4 Read Access Token. Stored locally in Jetpack DataStore.",
                    color = CineFlixColors.TextSecondary,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = tmdbInput,
                    onValueChange = { tmdbInput = it },
                    singleLine = true,
                    visualTransformation = if (showTmdbKey) VisualTransformation.None else PasswordVisualTransformation(),
                    placeholder = {
                        Text(
                            text = "Paste 32-char API Key or eyJ... Read Access Token",
                            color = CineFlixColors.TextMuted,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!keyFeedback.isNullOrBlank()) {
                    Text(
                        text = keyFeedback,
                        color = CineFlixColors.EmeraldMatch,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvActionButton(
                        text = if (isVerifyingKey) "Verifying..." else "Verify & Save Key",
                        icon = Icons.Default.Save,
                        onClick = { if (!isVerifyingKey) onSaveAndVerifyTmdbKey(tmdbInput) },
                        isPrimary = true
                    )
                    TvActionButton(
                        text = if (showTmdbKey) "Hide" else "Reveal",
                        onClick = { showTmdbKey = !showTmdbKey },
                        isPrimary = false
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0A0A0E))
                        .border(1.dp, Color(0x3334D399), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = CineFlixColors.EmeraldMatch,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Phone / PC Wi-Fi Companion Input: $companionUrl",
                        color = CineFlixColors.EmeraldMatch,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 2. Streaming Server & Auto-Failover
        item(key = "settings_servers") {
            SettingsCard(
                title = "Default Streaming Node & Hybrid Failover",
                icon = Icons.Default.Dns
            ) {
                Text(
                    text = "Select your preferred CineFlix playback node. Direct streams (.mp4 / .m3u8 / .mpd) launch in Native Media3 ExoPlayer; cloud embeds use Hybrid TV WebView.",
                    color = CineFlixColors.TextSecondary,
                    fontSize = 13.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CineFlixServers.all.forEach { srv ->
                        val selected = prefs.preferredServerId == srv.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Color(0x24E50914) else Color(0xFF0B0B10))
                                .border(
                                    width = 1.dp,
                                    color = if (selected) CineFlixColors.Crimson else CineFlixColors.BorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${srv.name}  ·  ${srv.badge}",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = srv.description,
                                    color = CineFlixColors.TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            TvFilterChip(
                                label = if (selected) "Active Default" else "Select",
                                selected = selected,
                                onClick = { onSelectPreferredServer(srv.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvFilterChip(
                        label = if (prefs.autoFailover) "Auto-Failover: ON (8s Watchdog)" else "Auto-Failover: OFF",
                        selected = prefs.autoFailover,
                        onClick = { onToggleAutoFailover(!prefs.autoFailover) }
                    )
                    TvFilterChip(
                        label = if (prefs.autoplayTrailers) "Autoplay Trailers: ON" else "Autoplay Trailers: OFF",
                        selected = prefs.autoplayTrailers,
                        onClick = { onToggleAutoplayTrailers(!prefs.autoplayTrailers) }
                    )
                }
            }
        }

        // 3. Server 6 Custom Direct Stream URL (ExoPlayer)
        item(key = "settings_direct_stream") {
            SettingsCard(
                title = "Native Media3 ExoPlayer Direct Stream URL (Server 6)",
                icon = Icons.Default.Link
            ) {
                Text(
                    text = "Provide a direct .mp4, .m3u8 (HLS), or .mpd (DASH) URL for native Media3 ExoPlayer playback with hardware decoding, audio track switching, and closed captions.",
                    color = CineFlixColors.TextSecondary,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = customUrlInput,
                    onValueChange = { customUrlInput = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "https://example.com/movie.mp4 or https://.../master.m3u8",
                            color = CineFlixColors.TextMuted,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvActionButton(
                        text = "Save Stream URL",
                        icon = Icons.Default.Save,
                        onClick = { onSaveCustomStreamUrl(customUrlInput) },
                        isPrimary = true
                    )
                    if (customUrlInput.isNotBlank()) {
                        TvActionButton(
                            text = "Clear URL",
                            onClick = {
                                customUrlInput = ""
                                onSaveCustomStreamUrl("")
                            },
                            isPrimary = false
                        )
                    }
                }
            }
        }

        // 4. CineFlix Cloud Sync (Firebase Auth + Firestore)
        item(key = "settings_cloud_sync") {
            SettingsCard(
                title = "CineFlix Cloud Library Sync",
                icon = Icons.Default.AccountCircle
            ) {
                val account = prefs.cloudAccount
                if (account != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = CineFlixColors.EmeraldMatch,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Signed in as ${account.displayName.ifBlank { account.email }}",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = account.email,
                                    color = CineFlixColors.TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        TvActionButton(
                            text = "Sign Out",
                            icon = Icons.Default.Logout,
                            onClick = onCloudSignOut,
                            isPrimary = false
                        )
                    }
                } else {
                    Text(
                        text = "Sign in with your CineFlix account to sync your Watchlist and Continue Watching progress across devices.",
                        color = CineFlixColors.TextSecondary,
                        fontSize = 13.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            singleLine = true,
                            placeholder = { Text("Email address", color = CineFlixColors.TextMuted, fontSize = 13.sp) },
                            colors = textFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            placeholder = { Text("Password", color = CineFlixColors.TextMuted, fontSize = 13.sp) },
                            colors = textFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!cloudAuthFeedback.isNullOrBlank()) {
                        Text(
                            text = cloudAuthFeedback,
                            color = Color(0xFFFCA5A5),
                            fontSize = 13.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvActionButton(
                            text = if (isCloudAuthLoading) "Signing in..." else "Sign In",
                            onClick = { if (!isCloudAuthLoading) onCloudSignIn(emailInput, passwordInput) },
                            isPrimary = true
                        )
                        TvActionButton(
                            text = "Create Account",
                            onClick = { if (!isCloudAuthLoading) onCloudSignUp(emailInput, passwordInput) },
                            isPrimary = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CineFlixColors.SurfaceElevated)
            .border(1.dp, CineFlixColors.BorderSubtle, RoundedCornerShape(20.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CineFlixColors.Crimson,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }
        content()
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CineFlixColors.Crimson,
    unfocusedBorderColor = Color(0x33FFFFFF),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color(0xFF0A0A0E),
    unfocusedContainerColor = Color(0xFF0A0A0E)
)
