package com.theflexproject.thunder.ui.seeall

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.theflexproject.thunder.Constants.TMDB_IMAGE_BASE_URL
import com.theflexproject.thunder.model.Movie
import com.theflexproject.thunder.model.MyMedia
import com.theflexproject.thunder.model.TVShowInfo.TVShow
import com.theflexproject.thunder.ui.home.HomeViewModel
import com.theflexproject.thunder.model.TVShowInfo.Episode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeeAllScreen(
    sectionId: String,
    sectionTitle: String,
    viewModel: HomeViewModel,
    onBackClick: () -> Unit,
    onItemClick: (com.theflexproject.thunder.model.MyMedia) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val section = uiState.sections.find { it.id == sectionId }
    val gridState = rememberLazyGridState()


    // Scroll-aware pagination trigger
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null) {
                    val currentSection = viewModel.uiState.value.sections.find { it.id == sectionId }
                    if (currentSection != null && !currentSection.isLoadingMore && currentSection.hasMore) {
                        if (lastVisibleIndex >= currentSection.items.size - 5) {
                            viewModel.loadMore(sectionId)
                        }
                    }
                }
            }
    }



    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(sectionTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (section == null || section.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Text("No content available or loading...", textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(110.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(section.items) { index, item ->
                        MediaGridItem(item, onItemClick)
                    }
                    
                    if (section.isLoadingMore) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaGridItem(
    item: com.theflexproject.thunder.model.MyMedia,
    onClick: (com.theflexproject.thunder.model.MyMedia) -> Unit
) {
    val title = when (item) {
        is Movie -> if (item.original_language == "id") item.original_title ?: item.title else item.title
        is TVShow -> item.name
        is Episode -> item.name
        else -> ""
    }

    val subtitle: String? = when (item) {
        is Episode -> "${item.show_name ?: ""} · S${item.season_number}E${item.episode_number}"
        else -> null
    }

    val imagePath = when (item) {
        is Movie -> item.poster_path
        is TVShow -> item.poster_path
        is Episode -> item.show_poster_path
        else -> null
    }

    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick(item) }
    ) {
        // Scale animation for TV pop effect
        val scale by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (isFocused) 1.05f else 1f,
            label = "scale"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .drawWithContent {
                    drawContent()
                    if (isFocused) {
                        // Outer Glow (6dp #CCFFFFFF)
                        drawRoundRect(
                            color = Color.DarkGray.copy(alpha = 0.8f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                            style = Stroke(width = 6.dp.toPx())
                        )
                        // Inner Sharp Border (2dp White, slight inset)
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
        ) {
            Card(
                modifier = Modifier
                    .aspectRatio(2f / 3f)
                    .fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                border = null,
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isFocused) 12.dp else 4.dp
                )
            ) {
                coil.compose.AsyncImage(
                    model = TMDB_IMAGE_BASE_URL + imagePath,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title ?: "",
            style = MaterialTheme.typography.labelMedium.copy(
                shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 4f)
            ),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    shadow = Shadow(color = Color.Black, offset = Offset(1f, 1f), blurRadius = 2f)
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
