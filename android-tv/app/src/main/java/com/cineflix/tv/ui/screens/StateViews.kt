package com.cineflix.tv.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cineflix.tv.ui.components.TvActionButton
import com.cineflix.tv.ui.theme.CineFlixColors

@Composable
fun TvShimmerLoadingView(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    val shimmerColor = Color(0xFF1A1A22).copy(alpha = alpha)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CineFlixColors.ObsidianBg),
        contentPadding = PaddingValues(start = 32.dp, end = 48.dp, top = 28.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(shimmerColor, Color(0xFF111116))
                        )
                    )
                    .border(1.dp, CineFlixColors.BorderSubtle, RoundedCornerShape(20.dp))
                    .padding(36.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerColor)
                    )
                    Box(
                        modifier = Modifier
                            .width(420.dp)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerColor)
                    )
                    Box(
                        modifier = Modifier
                            .width(520.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerColor)
                    )
                }
            }
        }

        items(3) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .width(210.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmerColor)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    items(6) {
                        Box(
                            modifier = Modifier
                                .width(172.dp)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(shimmerColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TvErrorView(
    message: String,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CineFlixColors.ObsidianBg)
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(620.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CineFlixColors.SurfaceElevated)
                .border(1.dp, CineFlixColors.BorderSubtle, RoundedCornerShape(20.dp))
                .padding(36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = CineFlixColors.Crimson,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Unable to Load Catalog",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = message,
                color = CineFlixColors.TextSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TvActionButton(
                    text = "Retry Connection",
                    icon = Icons.Default.Refresh,
                    onClick = onRetry,
                    isPrimary = true
                )
                TvActionButton(
                    text = "Configure TMDB Key",
                    icon = Icons.Default.Settings,
                    onClick = onOpenSettings,
                    isPrimary = false
                )
            }
        }
    }
}

@Composable
fun TvApiSetupGuideScreen(
    initialCredential: String,
    companionUrl: String,
    isVerifying: Boolean,
    errorMessage: String?,
    verifiedSuccess: Boolean,
    onVerifyAndSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputKey by remember(initialCredential) { mutableStateOf(initialCredential) }
    var showKey by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1F070A), CineFlixColors.ObsidianBg)
                )
            )
            .padding(horizontal = 56.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CineFlixColors.SurfaceElevated)
                .border(1.dp, CineFlixColors.BorderSubtle, RoundedCornerShape(24.dp))
                .padding(36.dp),
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Key Entry & Verification
            Column(
                modifier = Modifier.weight(1.35f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = CineFlixColors.Crimson,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "CINEFLIX BY MJ · TMDB CREDENTIAL SETUP",
                        color = CineFlixColors.Crimson,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "Connect Your TMDB API Key",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "CineFlix streams live movie and TV metadata directly from TMDB. Enter your free TMDB v3 API Key or v4 Read Access Token below, or send it from your phone on the same Wi-Fi.",
                    color = CineFlixColors.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )

                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "Paste 32-char API Key or eyJ... Read Access Token",
                            color = CineFlixColors.TextMuted,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onVerifyAndSave(inputKey) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CineFlixColors.Crimson,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0A0A0E),
                        unfocusedContainerColor = Color(0xFF0A0A0E)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFCA5A5),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x4D7F1D1D))
                            .padding(12.dp)
                    )
                }

                if (verifiedSuccess) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x4D064E3B))
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CineFlixColors.EmeraldMatch,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TMDB key verified! Launching CineFlix Google TV...",
                            color = Color(0xFFA7F3D0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvActionButton(
                        text = if (isVerifying) "Verifying..." else "Verify & Launch CineFlix",
                        onClick = { if (!isVerifying) onVerifyAndSave(inputKey) },
                        isPrimary = true
                    )
                    TvActionButton(
                        text = if (showKey) "Hide Key" else "Show Key",
                        onClick = { showKey = !showKey },
                        isPrimary = false
                    )
                    if (isVerifying) {
                        CircularProgressIndicator(
                            color = CineFlixColors.Crimson,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            }

            // Right Column: Phone-to-TV Companion Input & 3-Step Guide
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CineFlixColors.SurfaceCard)
                    .border(1.dp, CineFlixColors.BorderSubtle, RoundedCornerShape(18.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = CineFlixColors.EmeraldMatch,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quick Phone / PC Input",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Don't want to type on a TV remote? Open this local address on your phone or laptop browser:",
                    color = CineFlixColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF09090D))
                        .border(1.dp, Color(0x4434D399), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    Text(
                        text = companionUrl,
                        color = CineFlixColors.EmeraldMatch,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "How to Get a Free TMDB Key",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "01. Sign in at themoviedb.org/settings/api\n02. Copy your API Read Access Token or v3 API Key\n03. Paste via Phone URL above or D-Pad input",
                    color = CineFlixColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
