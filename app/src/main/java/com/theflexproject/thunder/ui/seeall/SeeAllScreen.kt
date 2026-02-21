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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.theflexproject.thunder.Constants.TMDB_IMAGE_BASE_URL
import com.theflexproject.thunder.model.Movie
import com.theflexproject.thunder.model.TVShowInfo.TVShow
import com.theflexproject.thunder.ui.home.HomeViewModel

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

    // Trigger load more when reaching the end
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && section != null) {
                    if (lastVisibleIndex >= section.items.size - 5 && !section.isLoadingMore && section.hasMore) {
                        viewModel.loadMore(sectionId)
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sectionTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
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
    val imagePath = when (item) {
        is Movie -> item.poster_path
        is TVShow -> item.poster_path
        else -> null
    }
    
    val title = when (item) {
        is Movie -> item.title
        is TVShow -> item.name
        else -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item) }
            .padding(4.dp)
    ) {
        Card(
            modifier = Modifier
                .aspectRatio(2f / 3f)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            AsyncImage(
                model = TMDB_IMAGE_BASE_URL + imagePath,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title ?: "",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
