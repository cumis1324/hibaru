package com.theflexproject.thunder.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theflexproject.thunder.repository.MovieRepository
import com.theflexproject.thunder.repository.TVShowRepository
import com.theflexproject.thunder.repository.TmdbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.theflexproject.thunder.model.Movie
import com.theflexproject.thunder.model.TVShowInfo.TVShow
import javax.inject.Inject

data class HomeUiState(
    val sections: List<HomeSection> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMoreGenres: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val nfgPlusApi: com.theflexproject.thunder.network.NFGPlusApi,
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TVShowRepository,
    private val tmdbRepository: TmdbRepository,
    private val syncPrefs: com.theflexproject.thunder.data.sync.SyncPrefs
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
        private const val PAGE_SIZE = 20
        private const val GENRE_BATCH_SIZE = 5
    }

    private val isTVDevice: Boolean by lazy {
        val uiModeManager = context.getSystemService(android.content.Context.UI_MODE_SERVICE) as? android.app.UiModeManager
        val isTelevision = uiModeManager?.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        val packageManager = context.packageManager
        val hasLeanback = packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
        val hasNoTouch = !packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TOUCHSCREEN)
        isTelevision || hasLeanback || hasNoTouch
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val sectionPages = mutableMapOf<String, Int>()
    private var allGenresPool = listOf<com.theflexproject.thunder.network.dto.GenreDto>()
    private var genreBatchIndex = 0

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            android.util.Log.d(TAG, "Loading initial home data (isTVDevice: $isTVDevice)")
            _uiState.update { it.copy(isLoading = true, sections = emptyList()) }
            sectionPages.clear()
            genreBatchIndex = 0
            
            try {
                val sections = mutableListOf<HomeSection>()

                // 1. Trending (HERO)
                val cachedTrendingIds = syncPrefs.trendingItemsJson
                val trendingItems: List<com.theflexproject.thunder.model.MyMedia> = if (!cachedTrendingIds.isNullOrEmpty()) {
                    val idList = cachedTrendingIds.split(",")
                    val movies = movieRepository.loadAllByIds(idList.filter { it.startsWith("m_") }.map { it.removePrefix("m_") })
                    val tvShows = tvShowRepository.loadAllTVShowsByIds(idList.filter { it.startsWith("t_") }.map { it.removePrefix("t_") })
                    val combined: List<com.theflexproject.thunder.model.MyMedia> = movies + tvShows
                    combined.sortedBy { item ->
                        val idStr = when(item) {
                            is com.theflexproject.thunder.model.Movie -> "m_${item.id}"
                            is com.theflexproject.thunder.model.TVShowInfo.TVShow -> "t_${item.id}"
                            else -> ""
                        }
                        idList.indexOf(idStr)
                    }
                } else {
                    val (tMovies, _) = tmdbRepository.getTrendingMoviesDeep(1)
                    val (tTv, _) = tmdbRepository.getTrendingTVShowsDeep(1)
                    (tMovies + tTv).shuffled()
                }
                if (trendingItems.isNotEmpty()) {
                    sections.add(HomeSection("trending_hero", "Trending", trendingItems, SectionType.HERO))
                }

                // 2. Curated Local Sections
                
                // Recently Added
                val recentMovies = movieRepository.getRecentlyAddedMovies(PAGE_SIZE, 0)
                if (recentMovies.isNotEmpty()) {
                    sections.add(HomeSection("recent_movies", "Baru Saja Ditambahkan", recentMovies))
                    sectionPages["recent_movies"] = 0
                }

                // New TV Shows
                val newTv = tvShowRepository.getNewTVShows(PAGE_SIZE, 0)
                if (newTv.isNotEmpty()) {
                    sections.add(HomeSection("new_tv", "Series Terbaru", newTv))
                    sectionPages["new_tv"] = 0
                }

                // Film Indonesia
                val indoMovies = movieRepository.getIndonesianMovies(PAGE_SIZE, 0)
                if (indoMovies.isNotEmpty()) {
                    sections.add(HomeSection("indo_movies", "Film Indonesia", indoMovies))
                    sectionPages["indo_movies"] = 0
                }

                // Drama Korea
                val drakor = tvShowRepository.getKoreanDramas(PAGE_SIZE, 0)
                if (drakor.isNotEmpty()) {
                    sections.add(HomeSection("drakor", "Drama Korea", drakor))
                    sectionPages["drakor"] = 0
                }

                // Top Rated
                val topMovies = movieRepository.getTopRatedMovies(PAGE_SIZE, 0)
                if (topMovies.isNotEmpty()) {
                    sections.add(HomeSection("top_movies", "Film Rating Tertinggi", topMovies))
                    sectionPages["top_movies"] = 0
                }
                
                val topTv = tvShowRepository.getTopRatedTVShows(PAGE_SIZE, 0)
                if (topTv.isNotEmpty()) {
                    sections.add(HomeSection("top_tv", "Series Rating Tertinggi", topTv))
                    sectionPages["top_tv"] = 0
                }

                // Recommendations
                val recom = movieRepository.getRecommendations(PAGE_SIZE, 0)
                if (recom.isNotEmpty()) {
                    sections.add(HomeSection("recom", "Rekomendasi Untukmu", recom))
                    sectionPages["recom"] = 0
                }

                // Old Gold
                val oldGold = movieRepository.getOldGoldMovies(PAGE_SIZE, 0)
                if (oldGold.isNotEmpty()) {
                    sections.add(HomeSection("old_gold", "Nostalgia Film Klasik", oldGold))
                    sectionPages["old_gold"] = 0
                }

                // 3. Prepare Genres for Infinite Scroll
                val cachedGenresJson = syncPrefs.cachedGenresJson
                allGenresPool = if (!cachedGenresJson.isNullOrEmpty()) {
                    try {
                        val gson = com.google.gson.Gson()
                        val type = object : com.google.gson.reflect.TypeToken<List<com.theflexproject.thunder.network.dto.GenreDto>>() {}.type
                        gson.fromJson<List<com.theflexproject.thunder.network.dto.GenreDto>>(cachedGenresJson, type)
                    } catch (e: Exception) { emptyList() }
                } else {
                    val genreResp = nfgPlusApi.getGenres()
                    if (genreResp.isSuccessful) genreResp.body()?.genres ?: emptyList() else emptyList()
                }

                _uiState.update { it.copy(sections = sections.toList(), isLoading = false, error = null) }
                
                // Load first batch of genres immediately after curated stuff
                loadNextGenreBatch()

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading home data", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun loadNextGenreBatch() {
        if (genreBatchIndex >= allGenresPool.size || _uiState.value.isLoadingMoreGenres) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreGenres = true) }
            
            val nextBatch = allGenresPool.drop(genreBatchIndex).take(GENRE_BATCH_SIZE)
            val newGenreSections = mutableListOf<HomeSection>()

            nextBatch.forEach { genre ->
                val movies = tmdbRepository.getMoviesByGenre(genre.id)
                val tv = tmdbRepository.getTvShowsByGenre(genre.id)
                val combined = (movies + tv).distinctBy { item ->
                    when(item) {
                        is Movie -> "m_${item.id}"
                        is TVShow -> "t_${item.id}"
                        else -> item.toString()
                    }
                }.sortedByDescending { item ->
                    when(item) {
                        is Movie -> item.popularity
                        is TVShow -> item.popularity
                        else -> 0.0
                    }
                }

                if (combined.isNotEmpty()) {
                    newGenreSections.add(HomeSection("genre_${genre.id}", genre.name, combined))
                    // Genres use simple local fetch for now, no pagination implemented for them yet
                }
            }

            genreBatchIndex += GENRE_BATCH_SIZE
            _uiState.update { it.copy(
                sections = it.sections + newGenreSections,
                isLoadingMoreGenres = false
            ) }
        }
    }

    fun loadMore(sectionId: String) {
        val currentSections = _uiState.value.sections
        val sectionIndex = currentSections.indexOfFirst { it.id == sectionId }
        if (sectionIndex == -1 || currentSections[sectionIndex].isLoadingMore) return

        val nextPage = (sectionPages[sectionId] ?: 0) + 1
        android.util.Log.d(TAG, "Requesting more items for section: $sectionId, page: $nextPage, offset: ${nextPage * PAGE_SIZE}")
        
        viewModelScope.launch {
            // Set loading more state
            val updatedSections = currentSections.toMutableList()
            updatedSections[sectionIndex] = updatedSections[sectionIndex].copy(isLoadingMore = true)
            _uiState.update { it.copy(sections = updatedSections) }

            try {
                val newItems = when (sectionId) {
                    "trending_hero" -> {
                        val (mMovies, mMoviePage) = tmdbRepository.getTrendingMoviesDeep((sectionPages["trending_movies"] ?: 1) + 1)
                        val (mTv, mTvPage) = tmdbRepository.getTrendingTVShowsDeep((sectionPages["trending_tv"] ?: 1) + 1)
                        sectionPages["trending_movies"] = mMoviePage
                        sectionPages["trending_tv"] = mTvPage
                        (mMovies + mTv).shuffled()
                    }
                    "recent_movies" -> movieRepository.getRecentlyAddedMovies(limit = PAGE_SIZE, offset = nextPage * PAGE_SIZE)
                    "new_tv" -> tvShowRepository.getNewTVShows(limit = PAGE_SIZE, offset = nextPage * PAGE_SIZE)
                    "indo_movies" -> movieRepository.getIndonesianMovies(limit = PAGE_SIZE, offset = nextPage * PAGE_SIZE)
                    "drakor" -> tvShowRepository.getKoreanDramas(limit = PAGE_SIZE, offset = nextPage * PAGE_SIZE)
                    "top_movies" -> movieRepository.getTopRatedMovies(limit = PAGE_SIZE, offset = nextPage * PAGE_SIZE)
                    "top_tv" -> tvShowRepository.getTopRatedTVShows(limit = PAGE_SIZE, offset = nextPage * PAGE_SIZE)
                    "recom" -> movieRepository.getRecommendations(limit = PAGE_SIZE, offset = nextPage * PAGE_SIZE)
                    "old_gold" -> movieRepository.getOldGoldMovies(limit = PAGE_SIZE, offset = nextPage * PAGE_SIZE)
                    else -> emptyList()
                }

                android.util.Log.d(TAG, "Received ${newItems.size} new items for section: $sectionId")

                if (newItems.isNotEmpty()) {
                    // Update page tracking for non-TMDB sections (TMDB handled inside when)
                    if (sectionId != "trending_movies" && sectionId != "trending_tv") {
                        sectionPages[sectionId] = nextPage
                    }
                    
                    val finalSections = _uiState.value.sections.toMutableList()
                    val idx = finalSections.indexOfFirst { it.id == sectionId }
                    if (idx != -1) {
                        val section = finalSections[idx]
                        val combinedItems = (section.items + newItems).distinctBy { 
                            when(it) {
                                is Movie -> "m_${it.id}"
                                is TVShow -> "t_${it.id}"
                                else -> it.toString()
                            }
                        }
                        android.util.Log.d(TAG, "Section $sectionId now has ${combinedItems.size} total items")
                        finalSections[idx] = section.copy(
                            items = combinedItems,
                            isLoadingMore = false
                        )
                        _uiState.update { it.copy(sections = finalSections) }
                    }
                } else {
                    android.util.Log.d(TAG, "No more items found for section: $sectionId")
                    resetLoadingState(sectionId)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading more items for $sectionId", e)
                resetLoadingState(sectionId)
            }
        }
    }

    private fun resetLoadingState(sectionId: String) {
        val finalSections = _uiState.value.sections.toMutableList()
        val idx = finalSections.indexOfFirst { it.id == sectionId }
        if (idx != -1) {
            finalSections[idx] = finalSections[idx].copy(isLoadingMore = false)
            _uiState.update { it.copy(sections = finalSections) }
        }
    }
}
