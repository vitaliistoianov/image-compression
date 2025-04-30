package io.image.compression

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import io.image.compression.PhotoCompressionActivity.Companion.EXTRA_COMPRESSED_PHOTO_BYTE_ARRAY
import io.image.compression.PhotoCompressionActivity.Companion.EXTRA_COMPRESSED_PHOTO_SIZE
import io.image.compression.PhotoSelectorActivity.Companion.EXTRA_ORIGINAL_PHOTO_URI
import io.image.compression.ext.findActivity
import io.image.compression.ui.theme.ImageCompressionTheme

class PhotoComparisonActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageCompressionTheme {
                val context = LocalContext.current
                val originalPhotoUri =
                    context.findActivity()?.intent?.getParcelableExtra<Uri>(
                        EXTRA_ORIGINAL_PHOTO_URI
                    )
                val compressedPhotoSize =
                    context.findActivity()?.intent?.getIntExtra(
                        EXTRA_COMPRESSED_PHOTO_SIZE,
                        0
                    )
                val compressedPhotoByteArray =
                    context.findActivity()?.intent?.getByteArrayExtra(
                        EXTRA_COMPRESSED_PHOTO_BYTE_ARRAY
                    )
                var originalPhotoName by rememberSaveable { mutableStateOf("") }
                var originalPhotoBytesSize by rememberSaveable { mutableLongStateOf(0L) }
                originalPhotoUri?.let { returnUri ->
                    LocalContext.current.contentResolver.query(
                        returnUri,
                        null,
                        null,
                        null,
                        null
                    )
                }?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    cursor.moveToFirst()
                    originalPhotoName = cursor.getString(nameIndex)
                    originalPhotoBytesSize = cursor.getLong(sizeIndex)
                }
                PhotoComparisonScreen(
                    originalPhotoUri = originalPhotoUri,
                    originalPhotoName,
                    originalPhotoBytesSize,
                    compressedPhotoSize,
                    compressedPhotoByteArray
                )
            }
        }
    }
}

@Composable
fun PhotoComparisonScreen(
    originalPhotoUri: Uri?,
    originalPhotoName: String,
    originalPhotoBytesSize: Long,
    compressedPhotoSize: Int?,
    compressedPhotoByteArray: ByteArray?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val columnModifier = modifier
        .padding(20.dp)
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
    ) {
        Row(
            columnModifier,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Compare your photos",
                modifier = modifier
            )
        }
        Text(
            modifier = columnModifier,
            text = "Before: $originalPhotoName, ${originalPhotoBytesSize / 1024} kb"
        )
        originalPhotoUri?.let {
            val painter = rememberAsyncImagePainter(
                ImageRequest
                    .Builder(context)
                    .data(data = originalPhotoUri)
                    .build()
            )
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .padding(16.dp)
            )
        }

        Spacer(Modifier.padding(5.dp))
        compressedPhotoSize?.let {
            Text(
                modifier = columnModifier, text = "After ${compressedPhotoSize / 1024} kb"
            )
        }
        compressedPhotoByteArray?.let {
            val painter = rememberAsyncImagePainter(
                ImageRequest
                    .Builder(context)
                    .data(data = compressedPhotoByteArray)
                    .build()
            )
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .padding(16.dp)
            )
        }
    }
}