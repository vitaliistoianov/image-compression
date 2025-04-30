package io.image.compression

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import io.image.compression.PhotoSelectorActivity.Companion.EXTRA_ORIGINAL_PHOTO_URI
import io.image.compression.ui.theme.ImageCompressionTheme

class PhotoSelectorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageCompressionTheme {
                PickPhotoScreen(
                    modifier = Modifier
                )
            }
        }
    }

    companion object {
        const val EXTRA_ORIGINAL_PHOTO_URI = "EXTRA_ORIGINAL_PHOTO_URI"
    }
}

@Composable
fun PickPhotoScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showContinue by rememberSaveable { mutableStateOf(false) }
    val result = remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        result.value = it
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Select photo to compress",
                modifier = modifier
            )
        }
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                launcher.launch(
                    PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }) {
                Text("Select photo")
            }
            Spacer(Modifier.padding(5.dp))
            Button(onClick = {
                Intent(context, PhotoCompressionActivity::class.java)
                    .also {
                        it.putExtra(EXTRA_ORIGINAL_PHOTO_URI, photoUri)
                        context.startActivity(it)
                    }
            }, enabled = showContinue) {
                Text("Continue")
            }
        }
        result.value?.let { image ->
            photoUri = image
            showContinue = true
        }
        photoUri?.let { image ->
            val painter = rememberAsyncImagePainter(
                model = ImageRequest
                    .Builder(LocalContext.current)
                    .data(data = image)
                    .build(),
                // For origin photo is best possible quality filtering, albeit also the slowest.
                // Typically this implies bicubic interpolation or better
                filterQuality = FilterQuality.High
            )
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}