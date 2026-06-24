package com.mahasiswa.sigma.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahasiswa.sigma.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {

    // --- Warna Navy Gradient ---
    val navyDark   = Color(0xFF04091E)
    val navyMid    = Color(0xFF0A1540)
    val accentBlue = Color(0xFF1A3A8F)
    val glowCyan   = Color(0xFF40C4FF)

    // --- Animasi infinite gradient shimmer ---
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerOffset"
    )

    // --- State animasi masuk ---
    var startAnimation by remember { mutableStateOf(false) }

    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "logoAlpha"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "logoScale"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "titleAlpha"
    )
    val titleOffsetY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 40f,
        animationSpec = tween(durationMillis = 800, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "titleOffsetY"
    )
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 700, delayMillis = 700, easing = FastOutSlowInEasing),
        label = "subtitleAlpha"
    )
    val subtitleOffsetY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 30f,
        animationSpec = tween(durationMillis = 700, delayMillis = 700, easing = FastOutSlowInEasing),
        label = "subtitleOffsetY"
    )

    // --- Glow animasi logo ---
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // --- Pulsing dots ---
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 160, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "dot3"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(3000)
        onTimeout()
    }

    // --- Background navy gradient dinamis ---
    val dynamicGradient = Brush.linearGradient(
        colors = listOf(
            navyDark,
            Color(
                red   = (navyMid.red   + shimmerOffset * 0.05f).coerceIn(0f, 1f),
                green = (navyMid.green + shimmerOffset * 0.02f).coerceIn(0f, 1f),
                blue  = (navyMid.blue  + shimmerOffset * 0.10f).coerceIn(0f, 1f),
                alpha = 1f
            ),
            accentBlue
        ),
        start = Offset(shimmerOffset * 400f, 0f),
        end   = Offset(400f - shimmerOffset * 400f, 1200f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = dynamicGradient),
        contentAlignment = Alignment.Center
    ) {

        // --- Lingkaran dekoratif background ---
        Box(
            modifier = Modifier
                .size(500.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-80).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentBlue.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowCyan.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        // --- Konten utama ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {

            // --- Glow ring di belakang logo ---
            Box(contentAlignment = Alignment.Center) {
                // Outer glow
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .alpha(glowAlpha * 0.4f)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glowCyan.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                // Inner glow
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .alpha(glowAlpha * 0.6f)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // --- Logo ---
                Image(
                    painter = painterResource(id = R.drawable.sigma_logo),
                    contentDescription = "SIGMA Logo",
                    modifier = Modifier
                        .size(150.dp)
                        .scale(logoScale)
                        .alpha(logoAlpha)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Teks SIGMA ---
            Text(
                text = "SIGMA",
                style = TextStyle(
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 14.sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.3f),
                        offset = Offset(0f, 2f),
                        blurRadius = 8f
                    )
                ),
                modifier = Modifier
                    .alpha(titleAlpha)
                    .offset(y = titleOffsetY.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // --- Garis pemisah elegan ---
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(1.dp)
                    .alpha(titleAlpha * 0.5f)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // --- Subtitle ---
            Text(
                text = "Sistem Informasi Gawat Darurat\ndan Mitigasi Bencana",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.55f),
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                ),
                modifier = Modifier
                    .alpha(subtitleAlpha)
                    .offset(y = subtitleOffsetY.dp)
                    .padding(horizontal = 48.dp)
            )

            Spacer(modifier = Modifier.height(64.dp))

            // --- Loading dots (elegan) ---
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.alpha(subtitleAlpha)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .alpha(dot1Alpha)
                        .background(color = Color.White.copy(alpha = 0.7f), shape = CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .alpha(dot2Alpha)
                        .background(color = Color.White.copy(alpha = 0.7f), shape = CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .alpha(dot3Alpha)
                        .background(color = Color.White.copy(alpha = 0.7f), shape = CircleShape)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen(onTimeout = {})
}
