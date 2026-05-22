package com.mahasiswa.sigma.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahasiswa.sigma.data.model.UserRole
import com.mahasiswa.sigma.ui.navigation.Route

@Composable
fun SigmaBottomBar(
    currentRoute: Route?,
    userRole: UserRole,
    onNavigateToHome: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToPosko: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToManageVolunteer: () -> Unit
) {
    // Determine visibility: Only show on core navigation screens
    val isVisible = currentRoute != null && (
            currentRoute is Route.Dashboard ||
            currentRoute is Route.Map ||
            currentRoute is Route.ShelterInfo ||
            currentRoute is Route.ManageShelter ||
            currentRoute is Route.ManageVolunteer ||
            currentRoute is Route.Profile
    )

    if (!isVisible) return

    // Floating Pill Container — overlaid directly on screen content, no background behind it
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 20.dp,
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Home
                BottomNavItem(
                    selected = currentRoute is Route.Dashboard,
                    onClick = onNavigateToHome,
                    icon = Icons.Default.Dashboard,
                    label = "Home"
                )

                // 2. Middle Left: Relawan (Admin) or Peta (Others)
                if (userRole == UserRole.BNPB) {
                    BottomNavItem(
                        selected = currentRoute is Route.ManageVolunteer,
                        onClick = onNavigateToManageVolunteer,
                        icon = Icons.Default.People,
                        label = "Relawan"
                    )
                } else {
                    BottomNavItem(
                        selected = currentRoute is Route.Map,
                        onClick = onNavigateToMap,
                        icon = Icons.Default.Map,
                        label = "Peta"
                    )
                }

                // 3. Middle Right: Posko
                val isPoskoSelected = currentRoute is Route.ShelterInfo || currentRoute is Route.ManageShelter
                BottomNavItem(
                    selected = isPoskoSelected,
                    onClick = onNavigateToPosko,
                    icon = if (userRole == UserRole.BNPB) Icons.Default.LocationOn else Icons.Default.HomeWork,
                    label = "Posko"
                )

                // 4. Profil
                BottomNavItem(
                    selected = currentRoute is Route.Profile,
                    onClick = onNavigateToProfile,
                    icon = Icons.Default.AccountCircle,
                    label = "Profil"
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "color"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 56.dp, height = 32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        else Color.Transparent
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier
                        .size(24.dp)
                        .scale(scale)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                color = contentColor,
                letterSpacing = 0.5.sp
            )
        }
    }
}
