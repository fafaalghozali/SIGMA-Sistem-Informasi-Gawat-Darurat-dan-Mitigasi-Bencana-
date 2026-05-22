package com.mahasiswa.sigma.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mahasiswa.sigma.data.model.*
import com.mahasiswa.sigma.ui.theme.*
import com.mahasiswa.sigma.ui.viewmodel.DashboardViewModel
import com.mahasiswa.sigma.ui.viewmodel.DashboardUiState
import kotlin.math.absoluteValue

@Composable
fun DashboardScreen(
    userRole: UserRole,
    userName: String,
    onFeatureClick: (Int) -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToProfile: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.onLocationPermissionGranted()
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData(userRole, isDark)
        viewModel.onPermissionRequested()
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    DashboardContent(
        userName = userName,
        uiState = uiState,
        isDark = isDark,
        onFeatureClick = onFeatureClick,
        onDismissNotification = { viewModel.dismissNotification() },
        onRetryLocation = {
            viewModel.onPermissionRequested()
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        },
        onRetryNews = { viewModel.retryNews() },
        onOpenSettings = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    userName: String,
    uiState: DashboardUiState,
    isDark: Boolean,
    onFeatureClick: (Int) -> Unit,
    onDismissNotification: () -> Unit,
    onRetryLocation: () -> Unit,
    onRetryNews: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val backgroundColor = if (isDark) DarkBackground else Color(0xFFF5F7FA)

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            DashboardHeader(userName = userName, isDark = isDark)
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 16.dp,
                    bottom = 160.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                
                item(span = { GridItemSpan(2) }) {
                    StatusCard(
                        weather = uiState.weatherInfo,
                        isLoading = uiState.isWeatherLoading,
                        error = uiState.weatherError,
                        isDark = isDark,
                        permissionDenied = uiState.locationPermissionDenied,
                        lastUpdated = uiState.lastUpdated,
                        onRetry = onRetryLocation,
                        onOpenSettings = onOpenSettings
                    )
                }

                
                item(span = { GridItemSpan(2) }) {
                    AnimatedVisibility(
                        visible = uiState.showNotification,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        EmergencyAlertCard(onDismiss = onDismissNotification, isDark = isDark)
                    }
                }

                
                val hasAlerts = uiState.earthquakeInfo != null || uiState.bmkgWarnings.isNotEmpty()

                if (hasAlerts) {
                    uiState.earthquakeInfo?.let { eq ->
                        item(span = { GridItemSpan(2) }) {
                            EarthquakeCard(earthquake = eq, isDark = isDark)
                        }
                    }
                    if (uiState.bmkgWarnings.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            BmkgWarningBanner(
                                warnings = uiState.bmkgWarnings,
                                isDark = isDark
                            )
                        }
                    }
                } else {
                    item(span = { GridItemSpan(2) }) {
                        SafeStateCard(isDark = isDark)
                    }
                }

                
                item(span = { GridItemSpan(2) }) {
                    SectionHeader(
                        title = "Layanan Utama",
                        subtitle = "Fitur darurat & bantuan mitigasi",
                        isDark = isDark
                    )
                }

                
                items(uiState.menuItems) { item ->
                    ServiceMenuCard(item, isDark) { onFeatureClick(item.id) }
                }

                
                item(span = { GridItemSpan(2) }) {
                    Spacer(modifier = Modifier.height(8.dp))
                    NewsCarouselSection(
                        newsItems = uiState.newsItems,
                        isLoading = uiState.isNewsLoading,
                        error = uiState.newsError,
                        isDark = isDark,
                        onRetry = onRetryNews
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor.copy(alpha = 0.5f))
                ) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(end = 20.dp, bottom = 100.dp)
            ) {
                EmergencyFab(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardHeader(userName: String, isDark: Boolean) {
    val containerColor = if (isDark) DarkSurface else MaterialTheme.colorScheme.primary
    val contentColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color.White

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Halo, $userName",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = contentColor
                )
                Text(
                    text = "Sistem Informasi Gawat Darurat & Mitigasi Bencana",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.75f)
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = contentColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "SIGMA",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun shimmerBrush(isDark: Boolean): Brush {
    val shimmerColors = if (isDark) {
        listOf(
            Color(0xFF2A2D35),
            Color(0xFF3A3D45),
            Color(0xFF2A2D35)
        )
    } else {
        listOf(
            Color(0xFFE8ECF0),
            Color(0xFFF5F7FA),
            Color(0xFFE8ECF0)
        )
    }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim + 200f, 0f)
    )
}

@Composable
private fun ShimmerBox(
    modifier: Modifier = Modifier,
    isDark: Boolean
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(shimmerBrush(isDark))
    )
}


@Composable
fun StatusCard(
    weather: WeatherInfo?,
    isLoading: Boolean,
    error: String?,
    isDark: Boolean,
    permissionDenied: Boolean,
    lastUpdated: Long?,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkElevatedSurface else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isDark)
            BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f))
        else
            BorderStroke(0.5.dp, Color(0xFFE8ECF0))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    StatusCardShimmer(isDark = isDark)
                }
                permissionDenied -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = EmergencyRed,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Izin Lokasi Diperlukan",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color.White else Color(0xFF1A1A1A)
                        )
                        Text(
                            text = "Aktifkan izin lokasi untuk melihat status wilayah real-time",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color.Gray else Color(0xFF888888),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onOpenSettings,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Pengaturan", style = MaterialTheme.typography.labelMedium)
                            }
                            Button(
                                onClick = onRetry,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Izinkan", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                error != null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color.Gray else Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = onRetry) {
                            Text("Coba Lagi", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                weather != null -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = weather.location,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isDark) Color.LightGray else Color.DarkGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Status Wilayah",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) Color.Gray else Color.DarkGray
                            )
                            Text(
                                text = weather.riskStatus,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = weather.riskColor
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.WaterDrop,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = MitigationBlue.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = weather.humidity,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDark) Color.Gray else Color(0xFF888888)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Air,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = MitigationBlue.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = weather.windSpeed,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDark) Color.Gray else Color(0xFF888888)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PulseIndicator(color = weather.riskColor)
                                Spacer(modifier = Modifier.width(6.dp))

                                val timeText = if (lastUpdated != null) {
                                    val relativeTime = DateUtils.getRelativeTimeSpanString(
                                        lastUpdated,
                                        System.currentTimeMillis(),
                                        DateUtils.MINUTE_IN_MILLIS,
                                        DateUtils.FORMAT_ABBREV_RELATIVE
                                    ).toString().lowercase()
                                    "Diperbarui $relativeTime"
                                } else {
                                    "Pantauan langsung"
                                }

                                Text(
                                    text = timeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) Color.LightGray else Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = weatherCodeToIcon(weather.weatherCode),
                                contentDescription = weather.condition,
                                modifier = Modifier.size(48.dp),
                                tint = weatherCodeToIconTint(weather.weatherCode, isDark)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = weather.temperature,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Light
                                ),
                                color = if (isDark) Color.White else Color(0xFF1A1A1A)
                            )
                            Text(
                                text = weather.condition,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = weatherCodeToIconTint(weather.weatherCode, isDark),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCardShimmer(isDark: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(
                modifier = Modifier
                    .width(100.dp)
                    .height(12.dp),
                isDark = isDark
            )
            Spacer(modifier = Modifier.height(10.dp))
            ShimmerBox(
                modifier = Modifier
                    .width(80.dp)
                    .height(10.dp),
                isDark = isDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            ShimmerBox(
                modifier = Modifier
                    .width(160.dp)
                    .height(18.dp),
                isDark = isDark
            )
            Spacer(modifier = Modifier.height(10.dp))
            ShimmerBox(
                modifier = Modifier
                    .width(120.dp)
                    .height(10.dp),
                isDark = isDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBox(
                modifier = Modifier
                    .width(140.dp)
                    .height(10.dp),
                isDark = isDark
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ShimmerBox(
                modifier = Modifier.size(48.dp),
                isDark = isDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            
            ShimmerBox(
                modifier = Modifier
                    .width(50.dp)
                    .height(20.dp),
                isDark = isDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            ShimmerBox(
                modifier = Modifier
                    .width(60.dp)
                    .height(10.dp),
                isDark = isDark
            )
        }
    }
}


private fun weatherCodeToIcon(code: Int): ImageVector = when (code) {
    0 -> Icons.Default.WbSunny                          
    1, 2 -> Icons.Default.WbCloudy                      
    3 -> Icons.Default.Cloud                             
    45, 48 -> Icons.Default.BlurOn                      
    51, 53, 55, 56, 57 -> Icons.Default.Grain           
    61, 63, 66, 80, 81 -> Icons.Default.Umbrella        
    65, 67, 82 -> Icons.Default.Umbrella                
    71, 73, 75, 77, 85, 86 -> Icons.Default.AcUnit      
    95, 96, 99 -> Icons.Default.Thunderstorm            
    else -> Icons.Default.Cloud
}


private fun weatherCodeToIconTint(code: Int, isDark: Boolean): Color = when (code) {
    0 -> Color(0xFFFFC107)                               
    1, 2 -> Color(0xFF90CAF9)                            
    3 -> if (isDark) Color(0xFFB0BEC5) else Color(0xFF78909C)
    45, 48 -> Color(0xFFB0BEC5)                          
    51, 53, 55, 56, 57 -> Color(0xFF64B5F6)             
    61, 63, 65, 66, 67, 80, 81, 82 -> Color(0xFF42A5F5) 
    71, 73, 75, 77, 85, 86 -> Color(0xFFE3F2FD)         
    95, 96, 99 -> Color(0xFF7E57C2)                      
    else -> if (isDark) Color(0xFFB0BEC5) else Color(0xFF78909C)
}

@Composable
fun EmergencyAlertCard(onDismiss: () -> Unit, isDark: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF2C1515) else Color(0xFFFFF0F0)
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF4D2020) else Color(0xFFFFCDD2))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(EmergencyRed.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = EmergencyRed,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "PERINGATAN DARURAT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                    color = EmergencyRed
                )
                Text(
                    "Hujan lebat berpotensi banjir di Surakarta utara.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color.White else Color.Black
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Tutup",
                    modifier = Modifier.size(16.dp),
                    tint = if (isDark) Color.Gray else Color.DarkGray
                )
            }
        }
    }
}


@Composable
fun EarthquakeCard(earthquake: EarthquakeInfo, isDark: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1A1A2E) else Color(0xFFF3F0FF)
        ),
        border = BorderStroke(
            0.5.dp,
            if (isDark) Color(0xFF3D3D6B) else Color(0xFFD0C8FF)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(SearchPurple.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Vibration,
                    contentDescription = null,
                    tint = SearchPurple,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Info Gempa BMKG",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = SearchPurple
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SearchPurple.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "M ${earthquake.magnitude}",
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = SearchPurple
                        )
                    }
                }
                Text(
                    text = earthquake.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color.White else Color(0xFF1A1A1A),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${earthquake.depth}  —  ${earthquake.time}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) Color.Gray else Color(0xFF888888)
                )
                if (earthquake.felt.isNotBlank() && earthquake.felt != "Tidak dirasakan") {
                    Text(
                        text = "Dirasakan: ${earthquake.felt}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = WarningOrange
                    )
                }
            }
        }
    }
}



@Composable
fun BmkgWarningBanner(warnings: List<BmkgWarning>, isDark: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF2A1A00) else Color(0xFFFFF8E1)
        ),
        border = BorderStroke(
            0.5.dp,
            if (isDark) Color(0xFF4D3600) else Color(0xFFFFE082)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = WarningOrange,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Peringatan Gempa Signifikan (BMKG)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black
                    ),
                    color = WarningOrange
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            warnings.take(3).forEach { warning ->
                val severityColor = when (warning.severity) {
                    WarningSeverity.DANGER -> EmergencyRed
                    WarningSeverity.WARNING -> WarningOrange
                    WarningSeverity.INFO -> MitigationBlue
                }
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(6.dp),
                        shape = CircleShape,
                        color = severityColor
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = warning.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color.White else Color(0xFF1A1A1A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = warning.time,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (isDark) Color.Gray else Color(0xFF888888)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewsCarouselSection(
    newsItems: List<NewsItem>,
    isLoading: Boolean,
    error: String?,
    isDark: Boolean,
    onRetry: () -> Unit
) {
    Column {
        SectionHeader(
            title = "Berita Terkini",
            subtitle = "Informasi bencana & kedaruratan real-time",
            isDark = isDark,
            actionText = if (newsItems.isNotEmpty()) "Lihat Semua" else null
        )
        Spacer(modifier = Modifier.height(8.dp))

        when {
            
            isLoading && newsItems.isEmpty() -> {
                NewsCarouselShimmer(isDark = isDark)
            }

            
            error != null && newsItems.isEmpty() -> {
                NewsErrorState(message = error, isDark = isDark, onRetry = onRetry)
            }

            
            newsItems.isEmpty() -> {
                NewsEmptyState(isDark = isDark)
            }

            
            else -> {
                val pagerState = rememberPagerState(pageCount = { newsItems.size })
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val news = newsItems[page]
                    NewsCard(
                        item = news,
                        isDark = isDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                val pageOffset = (
                                    (pagerState.currentPage - page) +
                                        pagerState.currentPageOffsetFraction
                                    ).absoluteValue
                                lerp(
                                    start = 0.95f,
                                    stop = 1f,
                                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                ).also { scale ->
                                    scaleX = scale
                                    scaleY = scale
                                }
                            }
                    )
                }

                
                if (newsItems.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(minOf(newsItems.size, 7)) { index ->
                            val selected = index == pagerState.currentPage
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(if (selected) 6.dp else 4.dp)
                                    .background(
                                        color = if (selected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}



@Composable
private fun NewsCarouselShimmer(isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isDark) DarkElevatedSurface else Color.White,
                RoundedCornerShape(20.dp)
            )
    ) {
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                
                ShimmerBox(modifier = Modifier.width(70.dp).height(18.dp), isDark = isDark)
                Spacer(modifier = Modifier.height(10.dp))
                
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(14.dp), isDark = isDark)
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.75f).height(14.dp), isDark = isDark)
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(14.dp), isDark = isDark)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBox(modifier = Modifier.width(100.dp).height(10.dp), isDark = isDark)
                ShimmerBox(modifier = Modifier.width(60.dp).height(10.dp), isDark = isDark)
            }
        }
    }
}



@Composable
private fun NewsErrorState(message: String, isDark: Boolean, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkElevatedSurface else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isDark) Color.Gray else Color(0xFF9E9E9E)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) Color.Gray else Color.DarkGray,
                textAlign = TextAlign.Center
            )
            OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Coba Lagi", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}



@Composable
private fun NewsEmptyState(isDark: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkElevatedSurface else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Newspaper,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = if (isDark) Color.Gray else Color(0xFF9E9E9E)
            )
            Text(
                text = "Belum ada berita terkini",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isDark) Color.White else Color(0xFF1A1A1A)
            )
            Text(
                text = "Tidak ada laporan bencana aktif saat ini",
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) Color.Gray else Color(0xFF888888),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NewsCard(item: NewsItem, isDark: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .height(160.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = {
                    if (item.link.isNotBlank()) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.link))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkElevatedSurface else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = if (!isDark) BorderStroke(0.5.dp, Color(0xFFE8ECF0)) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Black.copy(alpha = 0.80f)
                                )
                            )
                        )
                )
            } else {
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    item.categoryColor.copy(alpha = if (isDark) 0.15f else 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            
            val hasImage = !item.imageUrl.isNullOrBlank()
            val textColor = if (hasImage || isDark) Color.White else Color(0xFF1A1A1A)
            val subColor  = if (hasImage) Color.White.copy(alpha = 0.80f)
                            else if (isDark) Color.Gray else Color(0xFF888888)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    
                    Surface(
                        color = item.categoryColor.copy(alpha = if (hasImage) 0.85f else 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = if (!hasImage) BorderStroke(0.5.dp, item.categoryColor.copy(alpha = 0.4f)) else null
                    ) {
                        Text(
                            text = item.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (hasImage) Color.White else item.categoryColor
                        )
                    }

                    
                    if (item.isOfficial) {
                        Surface(
                            color = if (hasImage) Color.White.copy(alpha = 0.15f)
                                    else MitigationBlue.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = "Resmi",
                                    modifier = Modifier.size(9.dp),
                                    tint = if (hasImage) Color.White else MitigationBlue
                                )
                                Text(
                                    text = "RESMI",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp
                                    ),
                                    color = if (hasImage) Color.White else MitigationBlue
                                )
                            }
                        }
                    }

                    
                    if (!item.region.isNullOrBlank()) {
                        Text(
                            text = item.region,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = subColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 19.sp
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor
                )

                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    
                    if (item.source.isNotBlank()) {
                        Text(
                            text = item.source,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 9.sp
                            ),
                            color = item.categoryColor.copy(
                                alpha = if (hasImage) 0.9f else 1f
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "-",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = subColor.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = item.time,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = subColor
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (hasImage) Color.White.copy(alpha = 0.8f)
                               else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}



@Composable
fun SafeStateCard(isDark: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1B2E20) else Color(0xFFE8F5E9)
        ),
        border = BorderStroke(
            0.5.dp,
            if (isDark) Color(0xFF2E4C34) else Color(0xFFC8E6C9)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = "Tidak Ada Peringatan Darurat",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color(0xFF1A1A1A)
                )
                Text(
                    text = "Kondisi wilayah relatif aman saat ini",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color.LightGray else Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun ServiceMenuCard(item: DashboardMenuModel, isDark: Boolean, onClick: () -> Unit) {
    val accentColor = when (item.category) {
        MenuCategory.EMERGENCY -> EmergencyRed
        MenuCategory.VOLUNTEER -> VolunteerGreen
        MenuCategory.MITIGATION -> MitigationBlue
        MenuCategory.SEARCH -> SearchPurple
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkSurface else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = 0.5.dp,
            color = if (isDark) DarkElevatedSurface else Color(0xFFE8ECF0)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color(0xFF1A1A1A)
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) Color.Gray else Color(0xFF888888),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    isDark: Boolean,
    actionText: String? = null
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) Color.White else Color(0xFF1A1A1A)
            )
            if (actionText != null) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { }
                )
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) Color.Gray else Color(0xFF888888)
            )
        }
    }
}

@Composable
fun PulseIndicator(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = color, radius = size.minDimension / 2 * scale, alpha = alpha)
        }
        Surface(
            modifier = Modifier.size(8.dp),
            shape = CircleShape,
            color = color
        ) {}
    }
}

@Composable
fun EmergencyFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = EmergencyRed,
        contentColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp)),
        icon = {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        },
        text = {
            Text(
                text = "112",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardPreview() {
    val isDark = isSystemInDarkTheme()

    val mockUiState = DashboardUiState(
        menuItems = listOf(
            DashboardMenuModel(2, "Lapor Bencana", "Kirim laporan kejadian", Icons.Default.Report, MenuCategory.EMERGENCY),
            DashboardMenuModel(5, "Registrasi Relawan", "Daftar personil bantuan", Icons.Default.PersonAdd, MenuCategory.VOLUNTEER),
            DashboardMenuModel(10, "Panduan Mitigasi", "Tips & Prosedur PDF", Icons.AutoMirrored.Filled.MenuBook, MenuCategory.MITIGATION),
            DashboardMenuModel(7, "Cari Informasi", "Cari riwayat kejadian", Icons.Default.Search, MenuCategory.SEARCH)
        ),
        newsItems = listOf(
            NewsItem(
                id = "preview-1",
                title = "Banjir bandang melanda wilayah Sukoharjo, ratusan warga dievakuasi",
                time = "10 mnt lalu",
                publishedAt = System.currentTimeMillis() - 600_000,
                category = "DARURAT",
                categoryColor = EmergencyRed,
                source = "BMKG",
                severity = NewsSeverity.DARURAT,
                isOfficial = true,
                region = "Jawa Tengah"
            ),
            NewsItem(
                id = "preview-2",
                title = "Gempa bumi M 5.0 mengguncang Ternate, Maluku Utara",
                time = "3 jam lalu",
                publishedAt = System.currentTimeMillis() - 10_800_000,
                category = "WASPADA",
                categoryColor = WarningOrange,
                source = "Antara",
                severity = NewsSeverity.WASPADA,
                isOfficial = false,
                region = "Maluku"
            )
        ),
        weatherInfo = WeatherInfo(
            location = "Surakarta",
            condition = "Hujan Deras",
            temperature = "26°C",
            riskStatus = "Risiko Banjir Tinggi",
            riskColor = EmergencyRed,
            weatherCode = 65,
            humidity = "82%",
            windSpeed = "15 km/h"
        ),
        earthquakeInfo = EarthquakeInfo(
            magnitude = "5.2",
            location = "Tenggara Laut Jawa, Jawa Tengah",
            depth = "10 km",
            time = "21 Mei 2026 08:32 WIB",
            felt = "II-III MMI"
        ),
        bmkgWarnings = listOf(
            BmkgWarning(
                type = "Gempa Bumi",
                message = "M 5.2 – Tenggara Laut Jawa, Jawa Tengah",
                severity = WarningSeverity.WARNING,
                time = "21 Mei 2026 08:32 WIB"
            )
        ),
        showNotification = true,
        lastUpdated = System.currentTimeMillis()
    )

    DashboardContent(
        userName = "Supriyanto",
        uiState = mockUiState,
        isDark = isDark,
        onFeatureClick = {},
        onDismissNotification = {},
        onRetryLocation = {},
        onRetryNews = {},
        onOpenSettings = {}
    )
}
