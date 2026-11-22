package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.local.ClipEntity
import com.example.engine.VerticalFrameGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ClipExportResult(
    val downloadUri: Uri,
    val shareUri: Uri,
    val fileName: String,
    val displayLocation: String
)

class ClipExportHelper(
    private val context: Context,
    private val frameGenerator: VerticalFrameGenerator = VerticalFrameGenerator()
) {

    suspend fun exportVerticalShort(clip: ClipEntity): Result<ClipExportResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = frameGenerator.renderFrameBitmap(clip)
                val safeSlug = clip.title
                    .lowercase()
                    .replace(Regex("[^a-z0-9]+"), "_")
                    .trim('_')
                    .take(28)
                    .ifEmpty { "gtavi_clip" }
                val fileName = "${safeSlug}_${clip.id.coerceAtLeast(1)}.png"

                val cacheDir = File(context.cacheDir, "clips").apply { mkdirs() }
                val shareFile = File(cacheDir, fileName)
                FileOutputStream(shareFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                val shareUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    shareFile
                )

                val (downloadUri, locationLabel) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "image/png")
                        put(
                            MediaStore.Downloads.RELATIVE_PATH,
                            "${Environment.DIRECTORY_DOWNLOADS}/GTAVIClipping"
                        )
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: throw IllegalStateException("Could not create Download entry.")
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    } ?: throw IllegalStateException("Could not open output stream.")
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    uri to "Downloads/GTAVIClipping/$fileName"
                } else {
                    val dir = File(
                        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                        "GTAVIClipping"
                    ).apply { mkdirs() }
                    val outFile = File(dir, fileName)
                    FileOutputStream(outFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    Uri.fromFile(outFile) to "App Downloads/GTAVIClipping/$fileName"
                }

                bitmap.recycle()
                ClipExportResult(
                    downloadUri = downloadUri,
                    shareUri = shareUri,
                    fileName = fileName,
                    displayLocation = locationLabel
                )
            }
        }
}
