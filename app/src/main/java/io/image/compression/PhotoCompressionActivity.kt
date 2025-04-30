package io.image.compression

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
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
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import io.image.compression.PhotoCompressionActivity.Companion.EXTRA_COMPRESSED_PHOTO_BYTE_ARRAY
import io.image.compression.PhotoCompressionActivity.Companion.EXTRA_COMPRESSED_PHOTO_SIZE
import io.image.compression.PhotoSelectorActivity.Companion.EXTRA_ORIGINAL_PHOTO_URI
import io.image.compression.compression.ImageCompressor
import io.image.compression.ext.findActivity
import io.image.compression.ui.theme.ImageCompressionTheme
import kotlinx.coroutines.launch

class PhotoCompressionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageCompressionTheme {
                PhotoCompressionScreen(
                    modifier = Modifier
                )
            }
        }
    }

    companion object {
        const val EXTRA_COMPRESSED_PHOTO_SIZE = "EXTRA_COMPRESSED_PHOTO_SIZE"
        const val EXTRA_COMPRESSED_PHOTO_BYTE_ARRAY = "EXTRA_COMPRESSED_PHOTO_BYTE_ARRAY"
    }
}

@Composable
fun PhotoCompressionScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val compressor = remember { ImageCompressor(context) }
    var compressedByteArray by rememberSaveable { mutableStateOf<ByteArray?>(ByteArray(0)) }
    var sliderCompressionLevelPosition by rememberSaveable { mutableFloatStateOf(0f) }
    val originalPhotoUri =
        LocalContext.current.findActivity()?.intent?.getParcelableExtra<Uri>(
            EXTRA_ORIGINAL_PHOTO_URI
        )
    var compressedBytesSize by rememberSaveable { mutableIntStateOf(0) }
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

    val columnModifier = modifier
        .padding(20.dp, 1.dp, 20.dp, 1.dp)
        .fillMaxWidth()
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
                text = "Compress your photo",
                modifier = modifier
            )
        }

        ImagePreview(compressedByteArray, originalPhotoUri)
        Text(
            modifier = columnModifier,
            text = "Compression: ${sliderCompressionLevelPosition.toInt()} %"
        )
        CompressionDetails(
            compressedBytesSize,
            originalPhotoBytesSize,
            columnModifier
        )

        Slider(
            modifier = columnModifier,
            value = sliderCompressionLevelPosition,
            valueRange = 0f..100f,
            onValueChange = {
                sliderCompressionLevelPosition = it
                originalPhotoUri?.let {
                    scope.launch {
                        compressedByteArray = compressor.compressImage(
                            contentUri = originalPhotoUri,
                            sliderCompressionLevelPosition.toInt()
                        )
                        Log.d(
                            "COMPRESSION", "Compression size: ${compressedByteArray?.size} bytes," +
                                    " original size: $originalPhotoBytesSize bytes"
                        )
                        compressedBytesSize = compressedByteArray?.size ?: 0;
                    }
                }
            },
            onValueChangeFinished = {
                Log.d("COMPRESSION", "sliderPosition = $sliderCompressionLevelPosition")
            }
        )
        Spacer(Modifier.padding(5.dp))
        Button(modifier = columnModifier, onClick = {
            val intent = Intent(context, PhotoComparisonActivity::class.java)
            val originPhotoUriExtra = context.findActivity()?.intent?.getParcelableExtra<Uri>(
                EXTRA_ORIGINAL_PHOTO_URI
            )
            intent.also {
                it.putExtra(
                    EXTRA_ORIGINAL_PHOTO_URI,
                    originPhotoUriExtra
                )
                it.putExtra(
                    EXTRA_COMPRESSED_PHOTO_SIZE,
                    compressedBytesSize
                )
                it.putExtra(
                    EXTRA_COMPRESSED_PHOTO_BYTE_ARRAY,
                    compressedByteArray
                )
                context.startActivity(it)
            }
        }) {
            Text("Continue")
        }
    }
}

@Composable
fun CompressionDetails(
    compressedBytesSize: Int,
    originalPhotoBytesSize: Long,
    columnModifier: Modifier
) {
    var compressedSizeDelta by rememberSaveable { mutableLongStateOf(0) }
    var compressedSizePercentDelta by rememberSaveable { mutableFloatStateOf(0F) }

    if (compressedBytesSize < originalPhotoBytesSize) compressedSizeDelta =
        -(originalPhotoBytesSize - compressedBytesSize) / 1024
    if (compressedBytesSize in 1..<originalPhotoBytesSize)
        compressedSizePercentDelta =
            (-(1 - compressedBytesSize.toFloat() / originalPhotoBytesSize.toFloat()) * 100)

    Text(
        modifier = columnModifier,
        text = "Result size/%: $compressedSizeDelta kb / ${compressedSizePercentDelta.toInt()} %"
    )
}

@Composable
fun ImagePreview(compressedByteArray: ByteArray?, originalPhotoUri: Uri?) {
    val painter: Painter = if (compressedByteArray != null && compressedByteArray.isNotEmpty()) {
        rememberAsyncImagePainter(
            ImageRequest
                .Builder(LocalContext.current)
                .data(data = compressedByteArray)
                .build()
        )
    } else {
        rememberAsyncImagePainter(
            ImageRequest
                .Builder(LocalContext.current)
                .data(data = originalPhotoUri)
                .build()
        )
    }
    Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}