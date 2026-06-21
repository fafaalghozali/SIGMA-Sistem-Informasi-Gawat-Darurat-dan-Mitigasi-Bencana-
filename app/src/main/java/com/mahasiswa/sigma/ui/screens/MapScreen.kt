package com.mahasiswa.sigma.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import com.mahasiswa.sigma.data.model.ShelterMapItem
import com.mahasiswa.sigma.ui.viewmodel.MapViewModel
import java.text.SimpleDateFormat
import java.util.*

private fun shelterMarkerBitmap(fillColor: Int, isSelected: Boolean): BitmapDescriptor {
    val size = 120
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(60, 0, 0, 0)
        maskFilter = android.graphics.BlurMaskFilter(6f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }

    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.argb(180, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = if (isSelected) 5f else 3f
    }
    val doorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(200, 255, 255, 255)
        style = Paint.Style.FILL
    }
    val windowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(220, 255, 255, 255)
        style = Paint.Style.FILL
    }

    val cx = size / 2f
    val roofPeak = 10f
    val roofLeft = 8f
    val roofRight = size - 8f
    val roofBottom = 52f
    val bodyTop = 46f
    val bodyLeft = 18f
    val bodyRight = size - 18f
    val bodyBottom = 88f
    val pinTipY = size - 6f

    val pinPath = Path().apply {
        moveTo(cx, pinTipY)
        lineTo(bodyLeft + 4f, bodyBottom)
        lineTo(bodyRight - 4f, bodyBottom)
        close()
    }
    canvas.drawPath(pinPath, shadowPaint)

    val roofPath = Path().apply {
        moveTo(cx, roofPeak)
        lineTo(roofRight, roofBottom)
        lineTo(roofLeft, roofBottom)
        close()
    }
    canvas.drawPath(roofPath, bodyPaint)
    canvas.drawPath(roofPath, strokePaint)

    val bodyRect = RectF(bodyLeft, bodyTop, bodyRight, bodyBottom)
    canvas.drawRoundRect(bodyRect, 6f, 6f, bodyPaint)
    canvas.drawRoundRect(bodyRect, 6f, 6f, strokePaint)

    canvas.drawPath(pinPath, bodyPaint)
    canvas.drawPath(pinPath, strokePaint)

    val doorW = 16f
    val doorH = 22f
    val doorLeft = cx - doorW / 2
    val doorTop = bodyBottom - doorH
    val doorRect = RectF(doorLeft, doorTop, doorLeft + doorW, bodyBottom)
    canvas.drawRoundRect(doorRect, 3f, 3f, doorPaint)

    val winSize = 13f
    val winTop = bodyTop + 8f
    val winBottom = winTop + winSize
    canvas.drawRoundRect(RectF(bodyLeft + 8f, winTop, bodyLeft + 8f + winSize, winBottom), 2f, 2f, windowPaint)
    canvas.drawRoundRect(RectF(bodyRight - 8f - winSize, winTop, bodyRight - 8f, winBottom), 2f, 2f, windowPaint)

    if (isSelected) {
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(80, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        canvas.drawRoundRect(bodyRect, 6f, 6f, glowPaint)
    }

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private fun formatStatus(status: String): String = when (status.uppercase()) {
    "AWAS" -> "Awas"
    "SIAGA_1", "SIAGA 1" -> "Siaga 1"
    "SIAGA_2", "SIAGA 2" -> "Siaga 2"
    "PENDING" -> "Pending"
    "RESOLVED" -> "Resolved"
    else -> status.replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBack: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(uiState.cameraTarget, 14f)
    }

    LaunchedEffect(uiState.selectedReport) {
        uiState.selectedReport?.let { report ->
            if (report.latitude != 0.0 && report.longitude != 0.0) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(LatLng(report.latitude, report.longitude), 15f)
                )
            }
        }
    }

    LaunchedEffect(uiState.selectedShelter) {
        uiState.selectedShelter?.let { shelter ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(shelter.latitude, shelter.longitude), 16f)
            )
        }
    }

    val shelterAvailableColor = Color(0xFF1565C0).toArgb()
    val shelterFullColor = Color(0xFFB71C1C).toArgb()
    val shelterSelectedColor = Color(0xFF0D47A1).toArgb()

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = false),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
        ) {
            if (uiState.showShelterLayer) {
                uiState.shelters.forEach { shelter ->
                    val isSelected = uiState.selectedShelter?.name == shelter.name
                    val fillColor = when {
                        isSelected -> shelterSelectedColor
                        shelter.status == "Penuh" -> shelterFullColor
                        else -> shelterAvailableColor
                    }
                    val icon = remember(shelter.name, isSelected, shelter.status) {
                        shelterMarkerBitmap(fillColor, isSelected)
                    }
                    Marker(
                        state = MarkerState(position = LatLng(shelter.latitude, shelter.longitude)),
                        title = shelter.name,
                        snippet = "Kapasitas: ${shelter.capacity} · ${shelter.status}",
                        icon = icon,
                        anchor = androidx.compose.ui.geometry.Offset(0.5f, 1f),
                        onClick = {
                            viewModel.selectShelter(shelter)
                            false
                        }
                    )
                }
            }

            if (uiState.showReportLayer) {
                uiState.verifiedReports.forEach { report ->
                    val isSelected = uiState.selectedReport?.id == report.id
                    val hue = when (report.status.uppercase()) {
                        "AWAS" -> BitmapDescriptorFactory.HUE_RED
                        "SIAGA_1", "SIAGA 1" -> BitmapDescriptorFactory.HUE_ORANGE
                        "SIAGA_2", "SIAGA 2" -> BitmapDescriptorFactory.HUE_YELLOW
                        "RESOLVED" -> BitmapDescriptorFactory.HUE_GREEN
                        "PENDING" -> BitmapDescriptorFactory.HUE_BLUE
                        else -> BitmapDescriptorFactory.HUE_AZURE
                    }
                    Marker(
                        state = MarkerState(position = LatLng(report.latitude, report.longitude)),
                        title = report.title,
                        snippet = "Status: ${formatStatus(report.status)}",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            if (isSelected) BitmapDescriptorFactory.HUE_MAGENTA else hue
                        ),
                        onClick = {
                            viewModel.selectReport(report)
                            false
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 2.dp
            ) {
                Text(
                    "Peta Bencana Interaktif",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 12.dp, top = 60.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LayerToggleButton(
                icon = Icons.Default.Home,
                label = "Posko",
                active = uiState.showShelterLayer,
                activeColor = Color(0xFF1565C0),
                onClick = { viewModel.toggleShelterLayer() }
            )
            LayerToggleButton(
                icon = Icons.Default.Warning,
                label = "Laporan",
                active = uiState.showReportLayer,
                activeColor = Color(0xFFB71C1C),
                onClick = { viewModel.toggleReportLayer() }
            )
        }

        MapLegend(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 12.dp, bottom = 96.dp)
        )

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        uiState.selectedReport?.let { report ->
            ReportInfoSheet(
                report = report,
                onDismiss = { viewModel.dismissBottomSheet() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        uiState.selectedShelter?.let { shelter ->
            ShelterMapInfoSheet(
                shelter = shelter,
                onDismiss = { viewModel.dismissBottomSheet() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (!uiState.isLoading && uiState.verifiedReports.isEmpty() && uiState.showReportLayer) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
            ) {
                Text(
                    "Belum ada laporan terverifikasi dengan koordinat",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun LayerToggleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (active) activeColor.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(16.dp),
                tint = if (active) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MapLegend(modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Keterangan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowDropUp,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isExpanded) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LegendItem(color = Color(0xFF1565C0), label = "Posko Tersedia", isHouse = true)
                    LegendItem(color = Color(0xFFB71C1C), label = "Posko Penuh", isHouse = true)
                    LegendItem(color = Color(0xFFB71C1C), label = "Laporan Awas")
                    LegendItem(color = Color(0xFFE65100), label = "Laporan Siaga 1")
                    LegendItem(color = Color(0xFFF9A825), label = "Laporan Siaga 2")
                    LegendItem(color = Color(0xFF1565C0), label = "Laporan Pending")
                    LegendItem(color = Color(0xFF2E7D32), label = "Laporan Resolved")
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, isHouse: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isHouse) {
            Icon(
                Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = color
            )
        } else {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Text(label, fontSize = 10.sp)
    }
}

@Composable
private fun ReportInfoSheet(
    report: LocalDisasterReport,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (report.status.uppercase()) {
        "AWAS" -> Color(0xFFB71C1C)
        "SIAGA_1", "SIAGA 1" -> Color(0xFFE65100)
        "SIAGA_2", "SIAGA 2" -> Color(0xFFF9A825)
        "RESOLVED" -> Color(0xFF2E7D32)
        "PENDING" -> Color(0xFF1565C0)
        else -> Color(0xFF78909C)
    }

    Surface(
        modifier = modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
                    Text("Laporan Bencana", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(report.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.15f)) {
                Text(formatStatus(report.status), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = statusColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(report.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), maxLines = 3)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(report.location, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(report.reporter, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id")).format(Date(report.timestamp)),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun ShelterMapInfoSheet(
    shelter: ShelterMapItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val parts = shelter.capacity.split("/")
    val occupied = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
    val total = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 1
    val available = total - occupied
    val occupancyPercent = (occupied.toFloat() / total * 100).toInt().coerceIn(0, 100)

    val isFull = shelter.status == "Penuh"
    val occupancyColor = when {
        isFull || occupancyPercent >= 90 -> Color(0xFFB71C1C)
        occupancyPercent >= 70           -> Color(0xFFE65100)
        else                             -> Color(0xFF2E7D32)
    }
    val shelterBlue = Color(0xFF1565C0)

    Surface(
        modifier = modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = shelterBlue, modifier = Modifier.size(20.dp))
                    Text("Posko Pengungsian", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(shelter.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isFull) Color(0xFFB71C1C).copy(alpha = 0.15f) else Color(0xFF2E7D32).copy(alpha = 0.15f)
                ) {
                    Text(
                        shelter.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = if (isFull) Color(0xFFB71C1C) else Color(0xFF2E7D32),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                Text(shelter.address, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Kapasitas", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("${shelter.capacity} terisi ($occupancyPercent%)", fontSize = 12.sp, color = occupancyColor, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { occupancyPercent / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = occupancyColor,
                trackColor = occupancyColor.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(6.dp), color = shelterBlue.copy(alpha = 0.12f)) {
                Text(
                    if (isFull) "Tidak ada tempat tersisa" else "Tersedia: $available tempat",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = if (isFull) Color(0xFFB71C1C) else shelterBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
            if (!shelter.logistics.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Logistik:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    shelter.logistics.take(3).forEach { item ->
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)) {
                            Text(item, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
            if (!shelter.contactPhone.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(shelter.contactPhone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        }
    }
}
