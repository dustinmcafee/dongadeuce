package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * Composable that displays a card image with async loading
 */
@Composable
fun CardImage(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current

    // Cache the ImageRequest to avoid rebuilding on every recomposition
    val imageRequest = remember(imageUrl, context) {
        if (imageUrl.isNullOrEmpty()) null
        else ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .build()
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (imageRequest == null) {
            CardPlaceholder()
        } else {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                error = {
                    CardPlaceholder()
                }
            )
        }
    }
}

@Composable
private fun CardPlaceholder() {
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
