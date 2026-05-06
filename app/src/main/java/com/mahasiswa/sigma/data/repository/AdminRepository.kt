package com.mahasiswa.sigma.data.repository

import com.mahasiswa.sigma.data.model.PendingReport

class AdminRepository {
    fun getPendingReports(): List<PendingReport> {
        return listOf(
            PendingReport("1", "Banjir Bandang", "Andi (Masyarakat)", "Air setinggi 1 meter di jalan utama.", "10 menit yang lalu"),
            PendingReport("2", "Kebakaran Hutan", "Budi (Relawan)", "Titik api terlihat di lereng bukit.", "30 menit yang lalu"),
            PendingReport("3", "Tanah Longsor", "Citra (Masyarakat)", "Akses jalan terputus akibat longsoran.", "1 jam yang lalu")
        )
    }

    fun verifyReport(reportId: String) {
        // Implementasi verifikasi (misal update ke API/Database)
    }

    fun rejectReport(reportId: String) {
        // Implementasi penolakan
    }
}
