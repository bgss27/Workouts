package com.fittrack.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fittrack.app.data.repository.ExerciseImageRepository
import kotlinx.coroutines.launch

@Composable
fun ExerciseImageGallery(
    exerciseName: String,
    modifier: Modifier = Modifier
) {
    var images by remember(exerciseName) { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember(exerciseName) { mutableStateOf(true) }
    var hasError by remember(exerciseName) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(exerciseName) {
        isLoading = true
        hasError = false
        scope.launch {
            val result = ExerciseImageRepository.getExerciseImages(exerciseName)
            images = result
            isLoading = false
            hasError = result.isEmpty()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Loading exercise images...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                hasError -> {
                    Text(
                        "No images available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                images.size == 1 -> {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(images[0])
                            .crossfade(true)
                            .build(),
                        contentDescription = exerciseName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                images.size > 1 -> {
                    val pagerState = rememberPagerState(pageCount = { images.size })
                    Column {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) { page ->
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(images[page])
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "$exerciseName - position ${page + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        // Page indicator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(images.size) { index ->
                                val color = if (index == pagerState.currentPage)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .size(6.dp)
                                        .then(Modifier)
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = color,
                                        modifier = Modifier.size(6.dp)
                                    ) {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
