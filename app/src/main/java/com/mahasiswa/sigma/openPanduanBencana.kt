package com.mahasiswa.sigma

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PDFHelper {
    fun openPanduanBencana(context: Context) {
        val fileName = "panduan_bencana.pdf" // Pastikan nama file sama dengan yang di assets
        try {
            // 1. Ambil file dari assets
            val inputStream = context.assets.open(fileName)

            // 2. Simpan sementara ke folder cache
            val tempFile = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(tempFile)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            // 3. Dapatkan URI menggunakan FileProvider (SIGMA.app.main)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                tempFile
            )

            // 4. Kirim Intent ke PDF Viewer eksternal
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            }
            context.startActivity(Intent.createChooser(intent, "Buka Panduan dengan:"))

        } catch (e: Exception) {
            e.printStackTrace()
            // Tips: Tambahkan Toast di sini jika file tidak ditemukan
        }
    }
}