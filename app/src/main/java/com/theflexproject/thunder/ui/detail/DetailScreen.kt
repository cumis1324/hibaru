package com.theflexproject.thunder.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.theflexproject.thunder.Constants.TMDB_BACKDROP_IMAGE_BASE_URL
import com.theflexproject.thunder.model.Cast
import com.theflexproject.thunder.model.Movie
import com.theflexproject.thunder.utils.UnityAdHelper
import com.theflexproject.thunder.utils.findActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    movieId: Int? = null,
    tvShowId: Int? = null,
    tvShow: com.theflexproject.thunder.model.TVShowInfo.TVShow? = null,
    season: com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails? = null,
    episode: com.theflexproject.thunder.model.TVShowInfo.Episode? = null,
    viewModel: DetailViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context.findActivity() as? androidx.fragment.app.FragmentActivity

    LaunchedEffect(movieId, tvShowId, tvShow, season, episode) {
        android.util.Log.d("DetailScreen", "LaunchedEffect: movieId=$movieId, tvShowId=$tvShowId, tvShow=$tvShow")
        if (movieId != null && movieId != -1 && movieId != 0) {
            android.util.Log.d("DetailScreen", "Loading MOVIE with id=$movieId")
            viewModel.loadMovieDetails(movieId)
        } else if (tvShowId != null && tvShowId != -1 && tvShowId != 0) {
            android.util.Log.d("DetailScreen", "Loading TV SHOW with id=$tvShowId")
            viewModel.loadTvShowByTvShowId(tvShowId)
        } else if (tvShow != null && season != null && episode != null) {
            android.util.Log.d("DetailScreen", "Loading TV SHOW with full details")
            viewModel.loadTvShowDetails(tvShow, season, episode)
        } else {
            android.util.Log.e("DetailScreen", "NO VALID ARGUMENTS! movieId=$movieId, tvShowId=$tvShowId")
        }
    }

    BoxWithConstraints {
        val rootModifier = if (!constraints.hasBoundedHeight) {
            Modifier.fillMaxSize().heightIn(max = 2000.dp)
        } else {
            Modifier.fillMaxSize()
        }

        Scaffold(
            modifier = rootModifier,
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = if (uiState.isMovie) uiState.movie?.title ?: "Movie" else uiState.tvShow?.name ?: "TV Show",
                            color = MaterialTheme.colorScheme.onSurface
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                if (uiState.isLoading) {
                    android.util.Log.d("DetailScreen", "Rendering LOADING")
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.error != null) {
                    android.util.Log.e("DetailScreen", "Rendering ERROR: ${uiState.error}")
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    android.util.Log.d("DetailScreen", "Rendering CONTENT: isMovie=${uiState.isMovie}")
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            DetailHeader(uiState)
                        }
                        item {
                            ActionButtons(
                                uiState = uiState,
                                onWatchlist = {
                                    activity?.let {
                                        com.theflexproject.thunder.player.PlayerUtils.watchlist(
                                            it,
                                            listOfNotNull(uiState.movie ?: uiState.episode),
                                            uiState.tvShow,
                                            uiState.season
                                        )
                                    }
                                },
                                onShare = {
                                    activity?.let {
                                        com.theflexproject.thunder.player.PlayerUtils.share(
                                            it,
                                            it,
                                            listOfNotNull(uiState.movie ?: uiState.episode),
                                            uiState.tvShow,
                                            uiState.season
                                        )
                                    }
                                },
                                onDownload = {
                                    activity?.let {
                                        com.theflexproject.thunder.player.PlayerUtils.download(
                                            it,
                                            uiState.sources,
                                            uiState.tvShow,
                                            uiState.season
                                        )
                                    }
                                }
                            )
                        }
                        item {
                            DetailInfo(
                                uiState = uiState,
                                onClick = {
                                    activity?.let {
                                        if (uiState.isMovie) {
                                            uiState.movie?.let { movie ->
                                                val bottomSheet = com.theflexproject.thunder.fragments.VideoDetailsBottomSheet(movie.id, true)
                                                bottomSheet.show(it.supportFragmentManager, "VideoDetailsBottomSheet")
                                            }
                                        } else {
                                            uiState.tvShow?.let { tv ->
                                                val bottomSheet = com.theflexproject.thunder.fragments.VideoDetailsBottomSheet(tv)
                                                bottomSheet.show(it.supportFragmentManager, "VideoDetailsBottomSheet")
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        
                        // Native Ad - integrated between content sections
                        item {
                            val ctx = androidx.compose.ui.platform.LocalContext.current
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { context ->
                                    val frameLayout = android.widget.FrameLayout(context).apply {
                                        layoutParams = android.view.ViewGroup.LayoutParams(
                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                                        )
                                    }
                                    val activity = ctx.findActivity()
                                    if (activity != null) {
                                        UnityAdHelper.loadBanner(activity, frameLayout)
                                    }
                                    frameLayout
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                                    .height(60.dp)
                            )
                        }
                        
                        if (uiState.isMovie) {
                            item {
                                CastSection(uiState.credits?.cast ?: emptyList())
                            }
                        } else {
                            items(uiState.seasons) { seasonItem ->
                                SeasonItem(
                                    season = seasonItem,
                                    isExpanded = uiState.expandedSeasonId == seasonItem.id.toLong(),
                                    episodes = if (uiState.expandedSeasonId == seasonItem.id.toLong()) uiState.expandedEpisodes else emptyList(),
                                    onToggle = { viewModel.toggleSeasonExpansion(seasonItem.id.toLong(), seasonItem.season_number) },
                                    onEpisodeClick = { ep ->
                                        android.util.Log.d("DetailScreen", "Episode clicked: id=${ep.id}, name=${ep.name}, seasonNumber=${ep.season_number}, episodeNumber=${ep.episode_number}")
                                        activity?.let {
                                            val bundle = android.os.Bundle().apply {
                                                putInt("videoId", ep.id.toInt())
                                                putBoolean("isMovie", false)
                                                putInt("episodeId", ep.id.toInt())
                                                // Pass TVShow and Season objects
                                                putParcelable("tvShow", uiState.tvShow)
                                                uiState.seasons.find { it.season_number == ep.season_number }?.let { seasonItem ->
                                                    putParcelable("season", seasonItem)
                                                }
                                            }
                                            android.util.Log.d("DetailScreen", "Navigating to PlayerFragment with bundle: videoId=${ep.id.toInt()}, isMovie=false, tvShow=${uiState.tvShow?.name}, season=${ep.season_number}")
                                            androidx.navigation.fragment.NavHostFragment.findNavController(
                                                it.supportFragmentManager.findFragmentById(com.theflexproject.thunder.R.id.nav_host_fragment)!!
                                            ).navigate(com.theflexproject.thunder.R.id.action_tvShowDetailsFragment_to_playerFragment, bundle)
                                        }
                                    }
                                )
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailHeader(uiState: DetailUiState) {
    val movie = uiState.movie
    val tvShow = uiState.tvShow
    val imagePath = if (uiState.isMovie) movie?.backdrop_path ?: movie?.poster_path else tvShow?.backdrop_path ?: tvShow?.poster_path
    
    AsyncImage(
        model = TMDB_BACKDROP_IMAGE_BASE_URL + imagePath,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun ActionButtons(
    uiState: DetailUiState,
    onWatchlist: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        item {
            Button(onClick = onWatchlist) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Watchlist")
            }
        }
        item {
            Button(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Share")
            }
        }
        item {
            Button(onClick = onDownload) {
                Icon(Icons.Default.ArrowForward, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Download")
            }
        }
    }
}

@Composable
fun DetailInfo(uiState: DetailUiState, onClick: () -> Unit) {
    val movie = uiState.movie
    val tvShow = uiState.tvShow
    val episode = uiState.episode
    
    val title = if (uiState.isMovie) movie?.title else "${tvShow?.name} - S${uiState.season?.season_number}E${episode?.episode_number}"
    val overview = if (uiState.isMovie) movie?.overview else tvShow?.overview

    Column(modifier = Modifier.clickable { onClick() }.padding(16.dp)) {
        Text(
            text = title ?: "",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Rating: ${if (uiState.isMovie) movie?.vote_average else tvShow?.vote_average}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = overview ?: "",
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 4,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Read More",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SeasonItem(
    season: com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails,
    isExpanded: Boolean,
    episodes: List<com.theflexproject.thunder.model.TVShowInfo.Episode>,
    onToggle: () -> Unit,
    onEpisodeClick: (com.theflexproject.thunder.model.TVShowInfo.Episode) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable { onToggle() },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = TMDB_BACKDROP_IMAGE_BASE_URL + season.poster_path,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp, 75.dp).clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Season ${season.season_number}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = season.name ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
        }
        
        androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
            Column {
                episodes.forEach { episode ->
                    EpisodeItem(episode, onEpisodeClick)
                }
            }
        }
    }
}

@Composable
fun EpisodeItem(
    episode: com.theflexproject.thunder.model.TVShowInfo.Episode,
    onClick: (com.theflexproject.thunder.model.TVShowInfo.Episode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                android.util.Log.d("EpisodeItem", "Episode card clicked: id=${episode.id}, name=${episode.name}")
                onClick(episode) 
            }
            .padding(16.dp, 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        AsyncImage(
            model = TMDB_BACKDROP_IMAGE_BASE_URL + episode.still_path,
            contentDescription = null,
            modifier = Modifier.size(80.dp, 45.dp).clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "E${episode.episode_number}: ${episode.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = episode.overview ?: "",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CastSection(cast: List<com.theflexproject.thunder.model.Cast>) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Cast",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow {
            items(cast) { person ->
                CastItem(person)
            }
        }
    }
}

@Composable
fun CastItem(person: com.theflexproject.thunder.model.Cast) {
    Column(
        modifier = Modifier.padding(end = 16.dp).width(80.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = "https://image.tmdb.org/t/p/w185${person.profilePath}",
            contentDescription = null,
            modifier = Modifier.size(80.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = person.name ?: "",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
