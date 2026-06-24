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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.nativeCanvas
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
import androidx.hilt.navigation.compose.hiltViewModel
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
    userEmail: String = "",
    onFeatureClick: (Int) -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToProfile: () -> Unit,
    onNavigateToSearchDisaster: (String?, String?) -> Unit = { _, _ -> },
    onNavigateToReportDetail: (LocalDisasterReport) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
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
        if (userRole == UserRole.RELAWAN && userEmail.isNotBlank()) {
            viewModel.loadVolunteerStatus(userEmail)
        }
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
        userRole = userRole,
        userEmail = userEmail,
        onVolunteerStatusChange = { newStatus ->
            viewModel.updateVolunteerAvailability(userEmail, newStatus)
        },
        onFeatureClick = onFeatureClick,
        onNavigateToSearchDisaster = onNavigateToSearchDisaster,
        onNavigateToReportDetail = onNavigateToReportDetail,
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
    userRole: UserRole,
    userEmail: String,
    onVolunteerStatusChange: (String) -> Unit,
    onFeatureClick: (Int) -> Unit,
    onNavigateToSearchDisaster: (String?, String?) -> Unit,
    onNavigateToReportDetail: (LocalDisasterReport) -> Unit,
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
            if (userRole == UserRole.BNPB) {
                AdminDashboardContent(
                    userName = userName,
                    uiState = uiState,
                    isDark = isDark,
                    onFeatureClick = onFeatureClick,
                    onNavigateToSearchDisaster = onNavigateToSearchDisaster,
                    onNavigateToReportDetail = onNavigateToReportDetail,
                    padding = padding
                )
            } else {
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

                    if (userRole == UserRole.RELAWAN) {
                        item(span = { GridItemSpan(2) }) {
                            AvailabilityStatusCard(
                                currentStatus = uiState.volunteerStatus,
                                onStatusChange = onVolunteerStatusChange,
                                isDark = isDark
                            )
                        }
                    }

                    item(span = { GridItemSpan(2) }) {
                        val severeWeatherCodes = listOf(65, 67, 75, 77, 82, 86, 95, 96, 99)
                        val activeBmkg = uiState.bmkgWarnings.firstOrNull()
                        val isSevereWeather = uiState.weatherInfo?.weatherCode in severeWeatherCodes
                        val localAlert = uiState.localDisasterAlert

                        AnimatedVisibility(
                            visible = uiState.showNotification,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            if (localAlert != null) {
                                val alertColor = when (localAlert.status.uppercase()) {
                                    "AWAS" -> EmergencyRed
                                    "SIAGA_1", "SIAGA 1" -> WarningOrange
                                    "SIAGA_2", "SIAGA 2" -> WarningOrange
                                    else -> WarningOrange
                                }
                                val displayStatus = when (localAlert.status.uppercase()) {
                                    "AWAS" -> "Awas"
                                    "SIAGA_1", "SIAGA 1" -> "Siaga 1"
                                    "SIAGA_2", "SIAGA 2" -> "Siaga 2"
                                    "PENDING" -> "Pending"
                                    "RESOLVED" -> "Resolved"
                                    else -> localAlert.status
                                }
                                EmergencyAlertCard(
                                    title = "LAPORAN BENCANA DI WILAYAH ANDA",
                                    message = "${localAlert.title} - ${localAlert.location.take(60)}",
                                    alertColor = alertColor,
                                    onDismiss = onDismissNotification,
                                    onClick = {
                                        onNavigateToSearchDisaster(localAlert.title, displayStatus)
                                    },
                                    isDark = isDark
                                )
                            } else if (activeBmkg != null) {
                                val alertColor = when (activeBmkg.severity) {
                                    com.mahasiswa.sigma.data.model.WarningSeverity.DANGER -> EmergencyRed
                                    com.mahasiswa.sigma.data.model.WarningSeverity.WARNING -> WarningOrange
                                    else -> MitigationBlue
                                }
                                EmergencyAlertCard(
                                    title = "PERINGATAN BMKG",
                                    message = activeBmkg.message,
                                    alertColor = alertColor,
                                    onDismiss = onDismissNotification,
                                    onClick = {
                                        onNavigateToSearchDisaster(uiState.userCityName.ifBlank { null }, "Semua")
                                    },
                                    isDark = isDark
                                )
                            } else if (isSevereWeather) {
                                EmergencyAlertCard(
                                    title = "CUACA EKSTREM",
                                    message = "Potensi cuaca buruk: ${uiState.weatherInfo?.condition} di wilayah Anda.",
                                    alertColor = WarningOrange,
                                    onDismiss = onDismissNotification,
                                    onClick = {
                                        onNavigateToSearchDisaster(uiState.userCityName.ifBlank { null }, "Semua")
                                    },
                                    isDark = isDark
                                )
                            } else {
                                SafeStateCard(
                                    onDismiss = onDismissNotification,
                                    onClick = {
                                        onNavigateToSearchDisaster(null, "Semua")
                                    },
                                    isDark = isDark
                                )
                            }
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
                            onRetry = onRetryNews,
                            onViewAll = { onFeatureClick(13) }
                        )
                    }
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
                                Surface(
                                    modifier = Modifier.size(6.dp),
                                    shape = CircleShape,
                                    color = weather.riskColor
                                ) {}
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
fun EmergencyAlertCard(
    title: String,
    message: String,
    alertColor: Color,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    isDark: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) alertColor.copy(alpha = 0.15f) else alertColor.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, if (isDark) alertColor.copy(alpha = 0.3f) else alertColor.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(alertColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = alertColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = alertColor
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color.White else Color.Black
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
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
                    verticalAlignment = Alignment.CenterVertically) {
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
    onRetry: () -> Unit,
    onViewAll: () -> Unit = {}
) {
    Column {
        SectionHeader(
            title = "Berita Terkini",
            subtitle = "Informasi bencana & kedaruratan real-time",
            isDark = isDark,
            actionText = if (newsItems.isNotEmpty()) "Lihat Semua" else null,
            onAction = onViewAll
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
            .clip(RoundedCornerShape(20.dp))
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
fun SafeStateCard(onDismiss: () -> Unit, onClick: () -> Unit, isDark: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1B2E20) else Color(0xFFE8F5E9)
        ),
        border = BorderStroke(
            0.5.dp,
            if (isDark) Color(0xFF2E4C34) else Color(0xFFC8E6C9)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 32.dp),
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

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (isDark) Color.Gray else Color.DarkGray
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
            .clip(RoundedCornerShape(20.dp))
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
    actionText: String? = null,
    onAction: (() -> Unit)? = null
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
                    modifier = Modifier.clickable { onAction?.invoke() }
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
        userRole = UserRole.MASYARAKAT,
        userEmail = "",
        onVolunteerStatusChange = {},
        onFeatureClick = {},
        onNavigateToSearchDisaster = { _, _ -> },
        onNavigateToReportDetail = {},
        onDismissNotification = {},
        onRetryLocation = {},
        onRetryNews = {},
        onOpenSettings = {}
    )
}

@Composable
fun AvailabilityStatusCard(
    currentStatus: String,
    onStatusChange: (String) -> Unit,
    isDark: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                       else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Status Ketersediaan",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color.White else Color(0xFF1A1A1A)
                        )
                        Text(
                            text = "Tentukan kesiapan tugas Anda",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color.Gray else Color(0xFF888888)
                        )
                    }
                }

                val isAvailable = currentStatus == "Tersedia" || currentStatus == "Accepted" || currentStatus == "available"
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAvailable) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = if (isAvailable) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAvailable) "Tersedia" else "Tidak Tersedia",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isAvailable) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val isAvailable = currentStatus == "Tersedia" || currentStatus == "Accepted" || currentStatus == "available"

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onStatusChange("available") },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAvailable) {
                        if (isDark) Color(0xFF2E7D32).copy(alpha = 0.2f) else Color(0xFFE8F5E9)
                    } else Color.Transparent,
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (isAvailable) Color(0xFF2E7D32)
                                else if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE8ECF0)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isAvailable) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = "Tersedia",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isAvailable) Color(0xFF2E7D32)
                                        else if (isDark) Color.LightGray else Color.DarkGray
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onStatusChange("unavailable") },
                    shape = RoundedCornerShape(12.dp),
                    color = if (!isAvailable) {
                        if (isDark) Color(0xFFD32F2F).copy(alpha = 0.2f) else Color(0xFFFFEBEE)
                    } else Color.Transparent,
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (!isAvailable) Color(0xFFD32F2F)
                                else if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE8ECF0)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isAvailable) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = "Tidak Tersedia",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (!isAvailable) Color(0xFFD32F2F)
                                        else if (isDark) Color.LightGray else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}

data class DayTrendData(
    val label: String,
    val total: Int,
    val pending: Int,
    val verified: Int
)

@Composable
fun AdminDashboardContent(
    userName: String,
    uiState: DashboardUiState,
    isDark: Boolean,
    onFeatureClick: (Int) -> Unit,
    onNavigateToSearchDisaster: (String?, String?) -> Unit,
    onNavigateToReportDetail: (LocalDisasterReport) -> Unit,
    padding: PaddingValues
) {
    var selectedTimeRange by remember { mutableStateOf("Semua") }
    var isTotalReportsExpanded by remember { mutableStateOf(false) }

    val filteredReports = remember(uiState.allReports, selectedTimeRange) {
        val now = System.currentTimeMillis()
        val sdfFull = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        uiState.allReports.filter { report ->
            val reportTime = try {
                report.createdAt?.let {
                    val cleanDate = it.replace("T", " ")
                    val parsedDate = if (cleanDate.contains(".")) cleanDate.substringBefore(".") else cleanDate
                    sdfFull.parse(parsedDate)?.time
                }
            } catch (_: Exception) { null } ?: 0L

            when (selectedTimeRange) {
                "Hari ini" -> (now - reportTime) <= 24 * 60 * 60 * 1000L
                "7 Hari" -> (now - reportTime) <= 7 * 24 * 60 * 60 * 1000L
                "30 Hari" -> (now - reportTime) <= 30 * 24 * 60 * 60 * 1000L
                else -> true
            }
        }
    }

    val pendingReports = remember(filteredReports) {
        filteredReports.filter { it.status.uppercase() == "PENDING" }
    }

    val verifiedCount = remember(filteredReports) {
        filteredReports.count { it.status.uppercase() in listOf("VERIFIED", "RESOLVED", "AWAS", "SIAGA_1", "SIAGA 1", "SIAGA_2", "SIAGA 2") }
    }

    val verifiedThisWeekCount = remember(uiState.allReports) {
        val now = System.currentTimeMillis()
        val sdfFull = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        uiState.allReports.count { report ->
            val isVerified = report.status.uppercase() in listOf("VERIFIED", "RESOLVED", "AWAS", "SIAGA_1", "SIAGA 1", "SIAGA_2", "SIAGA 2")
            if (!isVerified) return@count false
            val reportTime = try {
                report.createdAt?.let {
                    val cleanDate = it.replace("T", " ")
                    val parsedDate = if (cleanDate.contains(".")) cleanDate.substringBefore(".") else cleanDate
                    sdfFull.parse(parsedDate)?.time
                }
            } catch (_: Exception) { null } ?: 0L
            (now - reportTime) <= 7 * 24 * 60 * 60 * 1000L
        }
    }

    val chartData = remember(filteredReports) {
        val sdfDay = java.text.SimpleDateFormat("dd MMM", java.util.Locale("id"))
        val sdfFull = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val now = System.currentTimeMillis()

        (6 downTo 0).map { i ->
            val timeInMillis = now - i * 24 * 60 * 60 * 1000L
            val dateObj = java.util.Date(timeInMillis)
            val dayLabel = sdfDay.format(dateObj)

            val cal = java.util.Calendar.getInstance()
            cal.time = dateObj
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = cal.timeInMillis

            val reportsInDay = filteredReports.filter { r ->
                val reportTime = try {
                    r.createdAt?.let {
                        val cleanDate = it.replace("T", " ")
                        val parsedDate = if (cleanDate.contains(".")) cleanDate.substringBefore(".") else cleanDate
                        sdfFull.parse(parsedDate)?.time
                    }
                } catch (_: Exception) { null } ?: 0L
                reportTime in startOfDay..(startOfDay + 24 * 60 * 60 * 1000L - 1)
            }

            val total = reportsInDay.size
            val pending = reportsInDay.count { it.status.uppercase() == "PENDING" }
            val verified = reportsInDay.count { it.status.uppercase() in listOf("VERIFIED", "RESOLVED", "AWAS", "SIAGA_1", "SIAGA 1", "SIAGA_2", "SIAGA 2") }

            DayTrendData(dayLabel, total, pending, verified)
        }
    }

    val peakData = remember(chartData) {
        chartData.maxByOrNull { it.total }
    }
    val peakText = if (peakData != null && peakData.total > 0) {
        "Puncak: ${peakData.label} (${peakData.total} laporan)"
    } else {
        "Puncak: Tidak ada data"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 16.dp,
            bottom = 160.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Header Statistik
        item {
            Column {
                Text(
                    text = "Ringkasan Laporan",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color(0xFF1A1A1A)
                )
                Text(
                    text = "Statistik laporan bencana",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color.Gray else Color(0xFF757575)
                )
            }
        }

        // Time Range Chips Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Hari ini", "7 Hari", "30 Hari", "Semua").forEach { range ->
                    val isSelected = selectedTimeRange == range
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTimeRange = range },
                        label = { Text(range, fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // TOTAL LAPORAN Card (Expandable)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isTotalReportsExpanded = !isTotalReportsExpanded }
                    .animateContentSize(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) DarkElevatedSurface else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(
                    0.5.dp,
                    if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE8ECF0)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "TOTAL LAPORAN",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isDark) Color.LightGray else Color(0xFF757575)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${filteredReports.size}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 36.sp
                                ),
                                color = if (isDark) Color.White else Color(0xFF1A1A1A)
                            )
                        }
                        Button(
                            onClick = { onFeatureClick(6) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1565C0), // Sigma Blue
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kelola Laporan",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Seluruh laporan bencana masuk ke sistem (Ketuk detail)",
                        fontSize = 11.sp,
                        color = if (isDark) Color.Gray else Color(0xFF9E9E9E)
                    )

                    if (isTotalReportsExpanded) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE8ECF0))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Grid statistics breakdown
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                BreakdownItem(
                                    modifier = Modifier.weight(1f),
                                    label = "PENDING",
                                    value = "${pendingReports.size}",
                                    color = Color(0xFFE65100),
                                    subtext = "Menunggu verifikasi"
                                )
                                BreakdownItem(
                                    modifier = Modifier.weight(1f),
                                    label = "VERIFIED",
                                    value = "$verifiedCount",
                                    color = Color(0xFF2E7D32),
                                    subtext = "Sudah diverifikasi"
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                BreakdownItem(
                                    modifier = Modifier.weight(1f),
                                    label = "AWAS",
                                    value = "${filteredReports.count { it.status.uppercase() == "AWAS" }}",
                                    color = Color(0xFFB71C1C),
                                    subtext = "Laporan aktif"
                                )
                                BreakdownItem(
                                    modifier = Modifier.weight(1f),
                                    label = "SIAGA 1",
                                    value = "${filteredReports.count { it.status.uppercase() in listOf("SIAGA_1", "SIAGA 1") }}",
                                    color = Color(0xFFE65100),
                                    subtext = "Laporan aktif"
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                    BreakdownItem(
                                        modifier = Modifier.weight(1f),
                                        label = "SIAGA 2",
                                        value = "${filteredReports.count { it.status.uppercase() in listOf("SIAGA_2", "SIAGA 2") }}",
                                        color = Color(0xFFF9A825),
                                        subtext = "Laporan aktif"
                                    )
                                    BreakdownItem(
                                        modifier = Modifier.weight(1f),
                                        label = "DITOLAK",
                                        value = "${filteredReports.count { it.status.uppercase() in listOf("DECLINE", "DECLINED", "DITOLAK") }}",
                                        color = Color(0xFF757575),
                                        subtext = "Laporan ditolak"
                                    )
                                }
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    BreakdownItem(
                                        modifier = Modifier.weight(1f),
                                        label = "RELAWAN",
                                        value = "${uiState.allVolunteers.size} aktif",
                                        color = Color(0xFF1565C0),
                                        subtext = "${uiState.allVolunteers.count { it.status.uppercase() in listOf("ACCEPTED", "APPROVED", "TERSEDIA") }} tersedia · ${uiState.allVolunteers.count { it.status.uppercase() == "PENDING" }} pending"
                                    )
                                }
                            }
                        }
                    }
                }
            }

        // Section: Menunggu Verifikasi
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Menunggu Verifikasi",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color(0xFF1A1A1A)
                )
                Text(
                    text = "${pendingReports.size} laporan pending",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color.Gray else Color(0xFF757575)
                )
            }
        }

        if (pendingReports.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) DarkElevatedSurface else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Semua laporan sudah ditangani",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isDark) Color.Gray else Color(0xFF888888)
                        )
                    }
                }
            }
        } else {
            items(pendingReports.take(3), key = { it.id ?: 0 }) { report ->
                val timestamp = try {
                    report.createdAt?.let {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        val cleanDate = it.replace("T", " ")
                        val parsedDate = if (cleanDate.contains(".")) cleanDate.substringBefore(".") else cleanDate
                        sdf.parse(parsedDate)?.time
                    }
                } catch (_: Exception) { null } ?: System.currentTimeMillis()

                val localReport = LocalDisasterReport(
                    id = report.id?.toString() ?: "",
                    title = report.title,
                    description = report.description,
                    location = report.location,
                    reporter = report.reporterName,
                    status = report.status,
                    latitude = report.latitude,
                    longitude = report.longitude,
                    timestamp = timestamp,
                    photoUrl = report.photoUrl,
                    disasterType = report.disasterType
                )
                PendingVerificationCard(
                    report = report,
                    isDark = isDark,
                    onDetailClick = {
                        onNavigateToReportDetail(localReport)
                    }
                )
            }
            if (pendingReports.size > 3) {
                item {
                    TextButton(
                        onClick = { onFeatureClick(6) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lihat ${pendingReports.size - 3} Laporan Pending Lainnya", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Verified summary labels
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "$verifiedCount diverifikasi total",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color.LightGray else Color.DarkGray
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFF1565C0),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "$verifiedThisWeekCount diverifikasi minggu ini",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color.LightGray else Color.DarkGray
                    )
                }
            }
        }

        // Section: Tren Laporan
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Tren Laporan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color(0xFF1A1A1A)
                )
                Text(
                    text = peakText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color.Gray else Color(0xFF757575)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) DarkElevatedSurface else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(
                    0.5.dp,
                    if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE8ECF0)
                )
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    ReportTrendChart(data = chartData, isDark = isDark)
                }
            }
        }

        // Section: Layanan Utama (Admin Menu Grid)
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = "Layanan Utama",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color(0xFF1A1A1A)
                )
                Text(
                    text = "Fitur darurat & bantuan mitigasi",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color.Gray else Color(0xFF757575)
                )
            }
        }

        // Menu Grid items pair by pair
        val menuChunks = uiState.menuItems.chunked(2)
        items(menuChunks) { chunk ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                chunk.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        ServiceMenuCard(item, isDark) { onFeatureClick(item.id) }
                    }
                }
                if (chunk.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BreakdownItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color,
    subtext: String
) {
    Card(
        modifier = modifier.padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.06f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                fontSize = 9.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ReportTrendChart(
    data: List<DayTrendData>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(data) {
        val maxTotal = data.maxOfOrNull { it.total } ?: 0
        val maxPending = data.maxOfOrNull { it.pending } ?: 0
        val maxVerified = data.maxOfOrNull { it.verified } ?: 0
        maxOf(maxTotal, maxPending, maxVerified, 5) // Minimum scale of 5 reports
    }

    val gridLineColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE8ECF0)
    val textPaintColor = if (isDark) android.graphics.Color.GRAY else android.graphics.Color.parseColor("#757575")

    Column(modifier = modifier) {
        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChartLegendItem(color = Color(0xFF1565C0), label = "Total", isDashed = false)
            Spacer(modifier = Modifier.width(12.dp))
            ChartLegendItem(color = Color(0xFF2E7D32), label = "Verified", isDashed = false)
            Spacer(modifier = Modifier.width(12.dp))
            ChartLegendItem(color = Color(0xFFE65100), label = "Pending", isDashed = true)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 8.dp)
        ) {
            val width = size.width
            val height = size.height

            val labelAreaWidth = 24.dp.toPx()
            val xAxisHeight = 24.dp.toPx()

            val graphWidth = width - labelAreaWidth
            val graphHeight = height - xAxisHeight

            // Draw Y Grid lines & Labels
            val yLines = 5
            for (i in 0..yLines) {
                val ratio = i.toFloat() / yLines
                val y = graphHeight * (1 - ratio)

                // Grid line
                drawLine(
                    color = gridLineColor,
                    start = Offset(labelAreaWidth, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )

                // Label
                val labelVal = (maxVal * ratio).toInt()
                drawContext.canvas.nativeCanvas.drawText(
                    labelVal.toString(),
                    8.dp.toPx(),
                    y + 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = textPaintColor
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.LEFT
                    }
                )
            }

            if (data.isNotEmpty()) {
                val stepX = graphWidth / (data.size - 1)

                // Draw X Labels
                data.forEachIndexed { idx, day ->
                    val x = labelAreaWidth + idx * stepX
                    drawContext.canvas.nativeCanvas.drawText(
                        day.label,
                        x,
                        height - 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = textPaintColor
                            textSize = 9.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }

                // Draw Lines helper
                fun drawTrendLine(
                    color: Color,
                    isDashed: Boolean,
                    selector: (DayTrendData) -> Int
                ) {
                    val composePath = androidx.compose.ui.graphics.Path()
                    data.forEachIndexed { idx, day ->
                        val x = labelAreaWidth + idx * stepX
                        val valRatio = selector(day).toFloat() / maxVal
                        val y = graphHeight * (1 - valRatio)

                        if (idx == 0) {
                            composePath.moveTo(x, y)
                        } else {
                            composePath.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = composePath,
                        color = color,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = if (isDashed) {
                                androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    intervals = floatArrayOf(10f, 10f),
                                    phase = 0f
                                )
                            } else null
                        )
                    )

                    // Draw circles at data points
                    data.forEachIndexed { idx, day ->
                        val x = labelAreaWidth + idx * stepX
                        val valRatio = selector(day).toFloat() / maxVal
                        val y = graphHeight * (1 - valRatio)

                        drawCircle(
                            color = color,
                            radius = 3.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = if (isDark) DarkSurface else Color.White,
                            radius = 1.5.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }

                // Draw Total (Blue)
                drawTrendLine(Color(0xFF1565C0), false) { it.total }

                // Draw Verified (Green)
                drawTrendLine(Color(0xFF2E7D32), false) { it.verified }

                // Draw Pending (Orange dashed)
                drawTrendLine(Color(0xFFE65100), true) { it.pending }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(color: Color, label: String, isDashed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(width = 16.dp, height = 4.dp)) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2.dp.toPx(),
                pathEffect = if (isDashed) {
                    androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        intervals = floatArrayOf(6f, 6f),
                        phase = 0f
                    )
                } else null
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PendingVerificationCard(
    report: com.mahasiswa.sigma.data.model.DisasterReportDto,
    isDark: Boolean,
    onDetailClick: () -> Unit
) {
    val statusColor = Color(0xFFE65100) // Orange for Pending
    val timeAgo = report.createdAt?.let { formatTimeAgo(it) } ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkElevatedSurface else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            0.5.dp,
            if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE8ECF0)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDark) Color.White else Color(0xFF1A1A1A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = report.location,
                            fontSize = 12.sp,
                            color = if (isDark) Color.LightGray else Color.DarkGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Pending",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE8ECF0))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = if (isDark) Color.Gray else Color(0xFF888888),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = timeAgo,
                        fontSize = 12.sp,
                        color = if (isDark) Color.Gray else Color(0xFF888888)
                    )
                }

                Button(
                    onClick = onDetailClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Detail", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatTimeAgo(dateStr: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val cleanDate = dateStr.replace("T", " ")
        val parsedDate = if (cleanDate.contains(".")) cleanDate.substringBefore(".") else cleanDate
        val date = sdf.parse(parsedDate) ?: return dateStr
        val now = System.currentTimeMillis()
        val diff = now - date.time

        val minutes = diff / (1000 * 60)
        val hours = diff / (1000 * 60 * 60)
        val days = diff / (1000 * 60 * 60 * 24)
        val weeks = days / 7
        val months = days / 30

        when {
            minutes < 1 -> "Baru saja"
            minutes < 60 -> "$minutes menit yang lalu"
            hours < 24 -> "$hours jam yang lalu"
            days < 7 -> "$days hari yang lalu"
            weeks < 4 -> "$weeks minggu yang lalu"
            months < 12 -> "$months bulan yang lalu"
            else -> java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id")).format(date)
        }
    } catch (e: Exception) {
        dateStr
    }
}

