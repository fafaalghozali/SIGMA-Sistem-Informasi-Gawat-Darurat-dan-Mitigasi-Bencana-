package com.mahasiswa.sigma.data.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object VolunteerReportParser {
    private val gson = Gson()

    fun parse(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        val trimmed = json.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                gson.fromJson(json, type) ?: emptyMap()
            } catch (e: Exception) {
                parseLegacy(json)
            }
        }
        return parseLegacy(json)
    }

    private fun parseLegacy(text: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val lines = text.split("\n")
        
        for (line in lines) {
            val cleanLine = line.trim().removePrefix("-").trim()
            if (cleanLine.contains(":")) {
                val parts = cleanLine.split(":", limit = 2)
                val rawKey = parts[0].trim().lowercase()
                val value = parts[1].trim()
                
                val key = when {
                    rawKey.contains("total korban") || rawKey.contains("korban ditangani") -> "total_korban"
                    rawKey.contains("selamat") -> "selamat"
                    rawKey.contains("luka ringan") -> "luka_ringan"
                    rawKey.contains("luka berat") -> "luka_berat"
                    rawKey.contains("kritis") -> "kritis"
                    rawKey.contains("meninggal") -> "meninggal"
                    rawKey.contains("kebutuhan medis") -> "kebutuhan_medis"
                    
                    rawKey.contains("total dievakuasi") || rawKey.contains("dievakuasi") -> "total_dievakuasi"
                    rawKey.contains("masih dicari") || rawKey.contains("hilang") -> "masih_dicari"
                    rawKey.contains("lokasi evakuasi") -> "lokasi_evakuasi"
                    rawKey.contains("kendala") -> "kendala_di_lapangan"
                    rawKey.contains("status pencarian") -> "status_pencarian"
                    
                    rawKey.contains("jenis bantuan") -> "jenis_bantuan"
                    rawKey.contains("jumlah disalurkan") -> "jumlah_disalurkan"
                    rawKey.contains("stok tersisa") -> "stok_tersisa"
                    rawKey.contains("kebutuhan mendesak") || rawKey.contains("kebutuhan gudang") -> "kebutuhan_mendesak"
                    
                    rawKey.contains("jumlah porsi") -> "jumlah_porsi"
                    rawKey.contains("menu") -> "menu_hari_ini"
                    rawKey.contains("pengungsi") -> "pengungsi_dilayani"
                    rawKey.contains("bahan masak") || rawKey.contains("bahan") -> "kebutuhan_bahan"
                    
                    rawKey.contains("jumlah didampingi") || rawKey.contains("didampingi") -> "jumlah_didampingi"
                    rawKey.contains("psikologis") -> "kondisi_psikologis"
                    rawKey.contains("kasus khusus") -> "kasus_khusus"
                    rawKey.contains("rekomendasi") -> "rekomendasi"
                    
                    rawKey.contains("jumlah siswa") || rawKey.contains("siswa") -> "jumlah_siswa"
                    rawKey.contains("materi") -> "materi_pembelajaran"
                    rawKey.contains("edu-kits") || rawKey.contains("edukits") -> "kebutuhan_edu_kits"
                    
                    else -> rawKey.replace(" ", "_")
                }
                map[key] = value
            }
        }
        
        if (map.isEmpty() && text.isNotBlank()) {
            map["catatan_pelaporan_lapangan"] = text
        }
        return map
    }

    fun toJson(map: Map<String, String>): String {
        return try {
            gson.toJson(map)
        } catch (e: Exception) {
            "{}"
        }
    }
}
