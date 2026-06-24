package com.mahasiswa.sigma.data.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import com.mahasiswa.sigma.data.model.DashboardMenuModel
import com.mahasiswa.sigma.data.model.MenuCategory
import com.mahasiswa.sigma.data.model.UserRole

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor() {

    fun getMenuItems(userRole: UserRole): List<DashboardMenuModel> {
        return when (userRole) {
            UserRole.BNPB -> {
                listOf(
                    DashboardMenuModel(1, "Monitoring Peta", "Pantau sebaran titik bencana", Icons.Default.Map, MenuCategory.MITIGATION),
                    DashboardMenuModel(14, "Laporan Relawan", "Pantau laporan tugas relawan", Icons.Default.Assignment, MenuCategory.EMERGENCY),
                    DashboardMenuModel(13, "Berita Resmi", "Kelola berita resmi aplikasi", Icons.Default.Article, MenuCategory.OTHERS),
                    DashboardMenuModel(15, "Data Pengguna", "Kelola data pengguna terdaftar", Icons.Default.People, MenuCategory.OTHERS),
                    DashboardMenuModel(7, "Arsip Bencana", "Riwayat kejadian bencana", Icons.Default.Search, MenuCategory.SEARCH),
                    DashboardMenuModel(10, "Panduan BNPB", "SOP & Regulasi kebencanaan", Icons.AutoMirrored.Filled.MenuBook, MenuCategory.MITIGATION)
                )
            }
            UserRole.RELAWAN -> {
                listOf(
                    DashboardMenuModel(2,  "Kirim Laporan Tugas",  "Kirim laporan tugas ke BNPB",       Icons.Default.Report,                MenuCategory.EMERGENCY),
                    DashboardMenuModel(14, "Riwayat Laporan",      "Lihat laporan tugas yang sudah dikirim", Icons.Default.Assignment,        MenuCategory.EMERGENCY),
                    DashboardMenuModel(7,  "Cari Info Bencana",    "Cari riwayat & info kejadian",      Icons.Default.Search,                MenuCategory.SEARCH),
                    DashboardMenuModel(10, "Panduan Mitigasi",     "Buku saku menghadapi bencana",      Icons.AutoMirrored.Filled.MenuBook,  MenuCategory.MITIGATION)
                )
            }
            else -> {
                listOf(
                    DashboardMenuModel(2, "Lapor Bencana", "Laporkan bencana yang terjadi", Icons.Default.Report, MenuCategory.EMERGENCY),
                    DashboardMenuModel(5, "Registrasi Relawan", "Daftar sebagai relawan", Icons.Default.PersonAdd, MenuCategory.VOLUNTEER),
                    DashboardMenuModel(10, "Panduan Mitigasi", "Buku saku menghadapi bencana", Icons.AutoMirrored.Filled.MenuBook, MenuCategory.MITIGATION),
                    DashboardMenuModel(7, "Cari Informasi Bencana", "Cari riwayat & info bencana", Icons.Default.Search, MenuCategory.SEARCH)
                )
            }
        }
    }
}
