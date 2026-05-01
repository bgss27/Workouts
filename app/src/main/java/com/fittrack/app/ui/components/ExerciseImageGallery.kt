package com.fittrack.app.ui.components

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.fittrack.app.data.repository.ExerciseImageRepository
import com.fittrack.app.data.repository.ExerciseMedia

@Composable
fun ExerciseImageGallery(
    exerciseName: String,
    modifier: Modifier = Modifier
) {
    var media by remember(exerciseName) { mutableStateOf<ExerciseMedia?>(null) }
    var isLoading by remember(exerciseName) { mutableStateOf(true) }
    var hasError by remember(exerciseName) { mutableStateOf(false) }

    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    LaunchedEffect(exerciseName) {
        isLoading = true
        hasError = false
        val result = ExerciseImageRepository.getExerciseMedia(exerciseName)
        media = result
        isLoading = false
        hasError = result == null
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Loading exercise animation...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                    hasError || media == null -> {
                        Text(
                            "No animation available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                    else -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(media!!.gifUrl)
                                .crossfade(true)
                                .build(),
                            imageLoader = imageLoader,
                            contentDescription = "$exerciseName demonstration",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // Show target muscles from API if available
            media?.let { m ->
                if (m.target.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Target:", style = MaterialTheme.typography.labelSmall)
                        AssistChip(
                            onClick = {},
                            label = { Text(m.target.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                        )
                        m.secondaryMuscles.take(2).forEach { muscle ->
                            AssistChip(
                                onClick = {},
                                label = { Text(muscle.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }
    }
}
