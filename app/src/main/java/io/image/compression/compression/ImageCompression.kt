package io.image.compression.compression

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ImageCompressor(
    private val context: Context
) {
    suspend fun compressImage(
        contentUri: Uri,
        compressionLevel: Int
    ): ByteArray? {
        return withContext(Dispatchers.IO) {
            val mimeType = context.contentResolver.getType(contentUri)
            val inputBytes = context
                .contentResolver
                .openInputStream(contentUri)
                ?.use { inputStream ->
                    inputStream.readBytes()
                } ?: return@withContext null

            withContext(Dispatchers.Default) {
                val bitmap = BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.size)

                val compressFormat = when (mimeType) {
                    "image/png" -> Bitmap.CompressFormat.PNG
                    "image/jpeg" -> Bitmap.CompressFormat.JPEG
                    "image/webp" -> if (Build.VERSION.SDK_INT >= 30) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else Bitmap.CompressFormat.WEBP

                    else -> Bitmap.CompressFormat.JPEG
                }

                var outputBytes: ByteArray
                val quality = 100 - compressionLevel

                Log.d("IMAGE_COMPRESSOR", "quality: $quality")

                ByteArrayOutputStream().use { outputStream ->
                    bitmap.compress(compressFormat, quality, outputStream)
                    outputBytes = outputStream.toByteArray()
                    quality
                }

                outputBytes
            }
        }
    }
}