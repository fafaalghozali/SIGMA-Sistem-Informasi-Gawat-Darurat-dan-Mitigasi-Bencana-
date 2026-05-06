package com.mahasiswa.sigma.data.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import com.mahasiswa.sigma.data.model.DashboardMenuModel
import com.mahasiswa.sigma.data.model.NewsItem
import com.mahasiswa.sigma.data.model.UserRole

class DashboardRepository {
    fun getMenuItems(userRole: UserRole): List<DashboardMenuModel> {
        val baseMenu = mutableListOf(
            DashboardMenuModel(1, "Peta Bencana", "Zona bahaya", Icons.Default.Map),
            DashboardMenuModel(2, "Lapor Bencana", "Kirim laporan", Icons.Default.Report),
            DashboardMenuModel(3, "Info Posko", "Titik pengungsian", Icons.Default.HomeWork),
            DashboardMenuModel(10, "Panduan Bencana", "Tips mitigasi PDF", Icons.AutoMirrored.Filled.MenuBook),
            DashboardMenuModel(5, "Registrasi Relawan", "Daftar relawan", Icons.Default.PersonAdd),
            DashboardMenuModel(7, "Cari Bencana", "Search & Filter", Icons.Default.Search)
        )

        if (userRole == UserRole.BNPB) {
            baseMenu.add(DashboardMenuModel(6, "Verifikasi Laporan", "Validasi data", Icons.Default.VerifiedUser))
        }
        return baseMenu
    }

    fun getNewsItems(isDark: Boolean): List<NewsItem> {
        return listOf(
            NewsItem(1, "Banjir bandang melanda wilayah Sukoharjo", "10 min ago", "INFO", if (isDark) Color(0xFF1B2C42) else Color(0xFFE3F2FD)),
            NewsItem(2, "Gempa bumi M 5,0 SR di Daerah Ternate, Maluku Utara hingga Rektorat UNS", "3 hours ago", "DARURAT", if (isDark) Color(0xFF422222) else Color(0xFFFFEBEE)),
            NewsItem(3, "Prakiraan cuaca: Hujan lebat esok hari di Soloraya", "1 hour ago", "WASPADA", if (isDark) Color(0xFF423422) else Color(0xFFFFF3E0)),
            NewsItem(4, "Penyaluran bantuan logistik di posko pengungsian terkenda jembatan terputus", "2 hours ago", "INFO", if (isDark) Color(0xFF1B2C42) else Color(0xFFE3F2FD))
        )
    }
}
