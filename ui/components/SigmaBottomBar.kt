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
    val isMainScreen = currentRoute != null && (
            currentRoute is Route.Dashboard ||
            currentRoute is Route.Map ||
            currentRoute is Route.ShelterInfo ||
            currentRoute is Route.ManageShelter ||
            currentRoute is Route.ManageVolunteer ||
            currentRoute is Route.Profile
    )

    if (!isMainScreen) return

    // Main Floating Surface
    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp) // Real floating margin
            .height(72.dp)
            .fillMaxWidth(),
        shape = CircleShape, // Modern Pill Shape
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, 
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Home
            BottomNavItem(
                selected = currentRoute is Route.Dashboard,
                onClick = onNavigateToHome,
                icon = if (currentRoute is Route.Dashboard) Icons.Default.Home else Icons.Default.Home,
                label = "Home"
            )

            // 2. Role-Based Center Item
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

            // 3. Posko
            val isPosko = currentRoute is Route.ShelterInfo || currentRoute is Route.ManageShelter
            BottomNavItem(
                selected = isPosko,
                onClick = onNavigateToPosko,
                icon = if (userRole == UserRole.BNPB) Icons.Default.LocationOn else Icons.Default.HomeWork,
                label = "Posko"
            )

            // 4. Profil
            BottomNavItem(
                selected = currentRoute is Route.Profile,
                onClick = onNavigateToProfile,
                icon = Icons.Default.Person,
                label = "Profil"
            )
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
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "color"
    )
    val scale by animateFloatAsState(
        if (selected) 1.2f else 1.0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
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
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier
                    .size(24.dp)
                    .scale(scale)
            )
            if (selected) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}
