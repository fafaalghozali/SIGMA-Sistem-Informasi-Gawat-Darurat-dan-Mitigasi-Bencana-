package com.mahasiswa.sigma

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object PdfUtils {
    private const val PDF_ASSET_NAME = "[sma.kemendikdasmen.go.id]-buku-saku-siaga-bencana-bnpb-2019.pdf"

    fun openPdfFromAssets(context: Context) {
        try {
            val file = File(context.cacheDir, "panduan_bencana.pdf")
            if (!file.exists()) {
                context.assets.open(PDF_ASSET_NAME).use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            }

            val chooser = Intent.createChooser(intent, "Buka Panduan Bencana")
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(chooser)
            } else {
                Toast.makeText(context, "Tidak ada aplikasi PDF viewer yang terinstal", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuka PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
