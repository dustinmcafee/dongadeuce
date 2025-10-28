package com.commandermtg.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.commandermtg.utils.ImageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage

/**
 * State of card image loading
 */
sealed class ImageLoadState {
    object Loading : ImageLoadState()
    data class Success(val image: ImageBitmap) : ImageLoadState()
    object Error : ImageLoadState()
}

/**
 * Composable that displays a card image with async loading and caching
 */
@Composable
fun CardImage(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    var loadState by remember(imageUrl) { mutableStateOf<ImageLoadState>(ImageLoadState.Loading) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrEmpty()) {
            loadState = ImageLoadState.Error
            return@LaunchedEffect
        }

        loadState = ImageLoadState.Loading

        try {
            val cachedFile = ImageCache.getCachedImage(imageUrl)
            if (cachedFile != null && cachedFile.exists()) {
                val imageBitmap = withContext(Dispatchers.IO) {
                    val bytes = cachedFile.readBytes()
                    SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                }
                loadState = ImageLoadState.Success(imageBitmap)
            } else {
                loadState = ImageLoadState.Error
            }
        } catch (e: Exception) {
            println("Error loading image from $imageUrl: ${e.message}")
            loadState = ImageLoadState.Error
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (val state = loadState) {
            is ImageLoadState.Loading -> {
                // Loading placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            is ImageLoadState.Success -> {
                // Display loaded image
                Image(
                    bitmap = state.image,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }

            is ImageLoadState.Error -> {
                // Error fallback - show card back placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "?",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

/**
 * Smaller card image for compact display (e.g., in lists)
 */
@Composable
fun CardImageThumbnail(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    CardImage(
        imageUrl = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier.size(width = 60.dp, height = 84.dp),
        contentScale = ContentScale.Crop
    )
}
