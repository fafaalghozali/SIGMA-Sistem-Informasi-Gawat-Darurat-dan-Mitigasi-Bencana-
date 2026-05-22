package com.mahasiswa.sigma.data.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import com.mahasiswa.sigma.data.model.DashboardMenuModel
import com.mahasiswa.sigma.data.model.MenuCategory
import com.mahasiswa.sigma.data.model.UserRole

/**
 * Provides static dashboard configuration data (menu items per user role).
 * News data is no longer managed here — see [NewsRepository] for dynamic news.
 */
class DashboardRepository {

    fun getMenuItems(userRole: UserRole): List<DashboardMenuModel> {
        return if (userRole == UserRole.BNPB) {
            listOf(
                DashboardMenuModel(6, "Verifikasi Laporan", "Validasi laporan dari publik", Icons.Default.VerifiedUser, MenuCategory.EMERGENCY),
                DashboardMenuModel(1, "Monitoring Peta", "Pantau sebaran titik bencana", Icons.Default.Map, MenuCategory.MITIGATION),
                DashboardMenuModel(7, "Arsip Bencana", "Riwayat kejadian bencana", Icons.Default.Search, MenuCategory.SEARCH),
                DashboardMenuModel(10, "Panduan BNPB", "SOP & Regulasi kebencanaan", Icons.AutoMirrored.Filled.MenuBook, MenuCategory.MITIGATION)
            )
        } else {
            listOf(
                DashboardMenuModel(2, "Lapor Bencana", "Kirim laporan kejadian cepat", Icons.Default.Report, MenuCategory.EMERGENCY),
                DashboardMenuModel(5, "Registrasi Relawan", "Daftar sebagai personil bantuan", Icons.Default.PersonAdd, MenuCategory.VOLUNTEER),
                DashboardMenuModel(10, "Panduan Mitigasi", "Tips & Prosedur keselamatan", Icons.AutoMirrored.Filled.MenuBook, MenuCategory.MITIGATION),
                DashboardMenuModel(7, "Cari Informasi", "Cari riwayat & info bencana", Icons.Default.Search, MenuCategory.SEARCH)
            )
        }
    }
}
