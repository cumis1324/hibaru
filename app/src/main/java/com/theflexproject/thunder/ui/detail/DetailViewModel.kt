package com.theflexproject.thunder.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theflexproject.thunder.model.Credits
import com.theflexproject.thunder.model.Movie
import com.theflexproject.thunder.model.MyMedia
import com.theflexproject.thunder.model.TVShowInfo.Episode
import com.theflexproject.thunder.model.TVShowInfo.TVShow
import com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails
import com.theflexproject.thunder.repository.MovieRepository
import com.theflexproject.thunder.repository.TVShowRepository
import com.theflexproject.thunder.repository.TmdbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DetailUiState(
    val movie: Movie? = null,
    val tvShow: TVShow? = null,
    val season: TVShowSeasonDetails? = null,
    val episode: Episode? = null,
    val credits: Credits? = null,
    val seasons: List<TVShowSeasonDetails> = emptyList(),
    val expandedSeasonId: Long? = null,
    val expandedEpisodes: List<Episode> = emptyList(),
    val similarOrEpisodes: List<MyMedia> = emptyList(),
    val sources: List<MyMedia> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isMovie: Boolean = true
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TVShowRepository,
    private val tmdbRepository: TmdbRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadMovieDetails(movieId: Int) {
        android.util.Log.d("DetailViewModel", "loadMovieDetails: movieId=$movieId")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isMovie = true) }
            try {
                movieRepository.getMovieById(movieId).collect { movie ->
                    android.util.Log.d("DetailViewModel", "loadMovieDetails: COLLECTED movie=$movie")
                    movie?.let {
                        android.util.Log.d("DetailViewModel", "loadMovieDetails: Fetching TMDB details for it.id=${it.id}")
                        val credits = tmdbRepository.getMovieCredits(it.id)
                        val similar = tmdbRepository.getSimilarMovies(it.id)
                        val sources = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            com.theflexproject.thunder.utils.DetailsUtils.getSourceList(null, it.id)
                        }
                        android.util.Log.d("DetailViewModel", "loadMovieDetails: EVERYTHING LOADED for ${it.title}")
                        _uiState.update { state ->
                            state.copy(
                                movie = it,
                                credits = credits,
                                similarOrEpisodes = similar,
                                sources = sources as List<MyMedia>,
                                isLoading = false
                            )
                        }
                    } ?: _uiState.update { it.copy(isLoading = false, error = "Movie not found") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadTvShowDetails(tvShow: TVShow, season: TVShowSeasonDetails, episode: Episode) {
        android.util.Log.d("DetailViewModel", "loadTvShowDetails: tvShow=${tvShow.name}, season=${season.season_number}, episode=${episode.name}")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isMovie = false) }
            try {
                val credits = tmdbRepository.getTVShowCredits(tvShow.id.toInt())
                val sources = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.theflexproject.thunder.utils.DetailsUtils.getEpisodeSource(null, episode.id.toInt())
                }
                // Fetch episodes for the current season
                tvShowRepository.getEpisodesBySeason(tvShow.id.toLong(), season.season_number).collect { episodes ->
                    _uiState.update { state ->
                        state.copy(
                            tvShow = tvShow,
                            season = season,
                            episode = episode,
                            credits = credits,
                            similarOrEpisodes = episodes,
                            sources = sources as List<MyMedia>,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadTvShowByTvShowId(tvShowId: Int) {
        android.util.Log.d("DetailViewModel", "loadTvShowByTvShowId: tvShowId=$tvShowId")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isMovie = false) }
            try {
                tvShowRepository.getTVShowById(tvShowId).collect { tvShow ->
                    android.util.Log.d("DetailViewModel", "loadTvShowByTvShowId: COLLECTED tvShow=$tvShow")
                    tvShow?.let {
                        val credits = tmdbRepository.getTVShowCredits(it.id)
                        // Fetch seasons
                        val seasonsList = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            com.theflexproject.thunder.database.DatabaseClient.getInstance(context)
                                .appDatabase.tvShowSeasonDetailsDao().findByShowId(it.id.toLong())
                        }
                android.util.Log.d("DetailViewModel", "loadTvShowByTvShowId: SEASONS LOADED count=${seasonsList.size}")
                        _uiState.update { state ->
                            state.copy(
                                tvShow = it,
                                credits = credits,
                                seasons = seasonsList,
                                isLoading = false
                            )
                        }
                    } ?: _uiState.update { it.copy(isLoading = false, error = "TV Show not found") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun toggleSeasonExpansion(seasonId: Long, seasonNumber: Int) {
        val currentState = _uiState.value
        if (currentState.expandedSeasonId == seasonId) {
            _uiState.update { it.copy(expandedSeasonId = null, expandedEpisodes = emptyList()) }
        } else {
            viewModelScope.launch {
                try {
                    val episodes = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.theflexproject.thunder.utils.DetailsUtils.getListEpisode(context, currentState.tvShow?.id ?: 0, seasonNumber)
                    }
                    _uiState.update { it.copy(expandedSeasonId = seasonId, expandedEpisodes = episodes as List<Episode>) }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    fun downloadEpisode(activity: android.app.Activity, episode: Episode, tvShow: TVShow?, season: TVShowSeasonDetails?) {
        viewModelScope.launch {
            try {
                val sources = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.theflexproject.thunder.utils.DetailsUtils.getEpisodeSource(context, episode.id.toInt())
                }
                com.theflexproject.thunder.player.PlayerUtils.download(
                    activity,
                    sources as List<MyMedia>,
                    tvShow,
                    season
                )
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
