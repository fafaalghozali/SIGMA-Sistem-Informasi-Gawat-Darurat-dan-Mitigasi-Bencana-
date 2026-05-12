package com.mahasiswa.sigma.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mahasiswa.sigma.data.model.ShelterMock

class ShelterInfoViewModel : ViewModel() {
    val shelters = listOf(
        ShelterMock("Stadion UNS", "1.2 km", "80/100", "Tersedia", -7.556303, 110.8580877, listOf("Sembako", "Air Mineral", "Selimut")),
        ShelterMock("Taman Cerdas Jebres", "1.5 km", "50/50", "Penuh", -7.5541321, 110.8536159, listOf("Popok Bayi", "Susu Formula", "Obat-obatan")),
        ShelterMock("Solo Techno Park", "2.2 km", "30/200", "Tersedia", -7.5560692, 110.8538666, listOf("Pakaian Layak Pakai", "Alat Mandi")),
        ShelterMock("SAR UNS", "0.8 km", "10/40", "Tersedia", -7.5615699, 110.8594894, listOf("Makanan Instan", "Tikar")),
        ShelterMock("Javanologi UNS", "0.7 KM", "127/250","Tersedia",-7.556998, 110.8598277, logisticsNeeded = listOf("Makanan Instan", "Alat Mandi", "Pakaian Layak Pakai")),
        ShelterMock("UNS Tower", "0.45 km","45/125","Tersedia",-7.5638533,110.8555975, logisticsNeeded = listOf("Susu Formula", "Obat-obatan", "Selimut")),
        ShelterMock("Asrama Mahasiswa UNS", "2.4 km","300/300","Penuh",-7.554193,110.865799, logisticsNeeded = listOf("Alat Mandi", "Sembako", "Sleeping Bag")),
        ShelterMock("Sekolah Vokasi UNS", "2.6 km","145/340","Tersedia",-7.559502,110.8383739, logisticsNeeded = listOf("Makanan Instan", "Obat-obatan", "Air Mineral"))
    )

    var selectedShelterName by mutableStateOf("")
    var selectedShelterLogistics by mutableStateOf<List<String>?>(null)
    var showLogisticsDialog by mutableStateOf(false)

    fun showLogistics(shelter: ShelterMock) {
        selectedShelterName = shelter.name
        selectedShelterLogistics = shelter.logisticsNeeded
        showLogisticsDialog = true
    }

    fun dismissLogisticsDialog() {
        showLogisticsDialog = false
    }
}
