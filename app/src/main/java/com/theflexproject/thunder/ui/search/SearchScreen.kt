package com.theflexproject.thunder.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.theflexproject.thunder.model.MyMedia
import com.theflexproject.thunder.ui.seeall.MediaGridItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onItemClick: (MyMedia) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .statusBarsPadding()
                .fillMaxSize()
        ) {
            // Search Bar with safe padding
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.onQueryChanged(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search movies & shows...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                shape = MaterialTheme.shapes.extraLarge
            )

            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    query.isNotEmpty() && uiState.searchResults.isEmpty() && !uiState.isLoading -> {
                        Text(
                            text = "No results found for \"$query\"",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    query.isEmpty() -> {
                        Text(
                            text = "Type to search...",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(110.dp),
                            contentPadding = PaddingValues(bottom = 16.dp, start = 8.dp, end = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.searchResults) { item ->
                                MediaGridItem(item = item, onClick = onItemClick)
                            }
                        }
                    }
                }
            }
        }
    }
}
