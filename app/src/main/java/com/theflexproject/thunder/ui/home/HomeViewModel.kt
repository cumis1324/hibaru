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
import javax.inject.Inject

data class HomeUiState(
    val sections: List<HomeSection> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TVShowRepository,
    private val tmdbRepository: TmdbRepository
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
        private const val PAGE_SIZE = 20
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val sectionPages = mutableMapOf<String, Int>()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            android.util.Log.d(TAG, "Loading initial home data")
            _uiState.update { it.copy(isLoading = true) }
            sectionPages.clear()
            try {
                val sections = mutableListOf<HomeSection>()

                // 1. Trending Movies (TMDB - HERO)
                val (trendingMovies, moviePage) = tmdbRepository.getTrendingMoviesDeep(1)
                android.util.Log.d(TAG, "Fetched ${trendingMovies.size} trending movies from TMDB")
                if (trendingMovies.isNotEmpty()) {
                    sections.add(HomeSection("trending_movies", "Trending Movies", trendingMovies, SectionType.HERO))
                    sectionPages["trending_movies"] = moviePage
                }

                // 2. Trending TV Shows (TMDB)
                val (trendingTV, tvPage) = tmdbRepository.getTrendingTVShowsDeep(1)
                android.util.Log.d(TAG, "Fetched ${trendingTV.size} trending TV shows from TMDB")
                if (trendingTV.isNotEmpty()) {
                    sections.add(HomeSection("trending_tv", "Trending TV Shows", trendingTV))
                    sectionPages["trending_tv"] = tvPage
                }

                // 3. Recently Added (Local)
                val recentlyAdded = movieRepository.getRecentlyAddedMovies(limit = PAGE_SIZE, offset = 0)
                android.util.Log.d(TAG, "Fetched ${recentlyAdded.size} recently added movies")
                if (recentlyAdded.isNotEmpty()) {
                    sections.add(HomeSection("recent", "Recently Added", recentlyAdded))
                    sectionPages["recent"] = 0
                }

                // 4. Series (Local)
                val series = tvShowRepository.getNewTVShows(limit = PAGE_SIZE, offset = 0)
                android.util.Log.d(TAG, "Fetched ${series.size} new TV shows (Series)")
                if (series.isNotEmpty()) {
                    sections.add(HomeSection("series", "Series", series))
                    sectionPages["series"] = 0
                }
                
                // 5. Korean Dramas (Local)
                val drakor = tvShowRepository.getKoreanDramas(limit = PAGE_SIZE, offset = 0)
                android.util.Log.d(TAG, "Fetched ${drakor.size} Korean Dramas")
                if (drakor.isNotEmpty()) {
                    sections.add(HomeSection("drakor", "Korean Dramas", drakor))
                    sectionPages["drakor"] = 0
                }

                // 6. Indonesian Movies (Local)
                val indoMovies = movieRepository.getIndonesianMovies(limit = PAGE_SIZE, offset = 0)
                android.util.Log.d(TAG, "Fetched ${indoMovies.size} Indonesian Movies")
                if (indoMovies.isNotEmpty()) {
                    sections.add(HomeSection("indo", "Indonesian Movies", indoMovies))
                    sectionPages["indo"] = 0
                }

                // 7. Top Rated Movies (Local)
                val topRatedMovies = movieRepository.getTopRatedMovies(limit = PAGE_SIZE, offset = 0)
                android.util.Log.d(TAG, "Fetched ${topRatedMovies.size} top rated movies")
                if (topRatedMovies.isNotEmpty()) {
                    sections.add(HomeSection("top_rated_movies", "Top Rated Movies", topRatedMovies))
                    sectionPages["top_rated_movies"] = 0
                }

                // 8. Top Rated TV Shows (Local)
                val topRatedTV = tvShowRepository.getTopRatedTVShows(limit = PAGE_SIZE, offset = 0)
                android.util.Log.d(TAG, "Fetched ${topRatedTV.size} top rated TV shows")
                if (topRatedTV.isNotEmpty()) {
                    sections.add(HomeSection("top_rated_tv", "Top Rated TV shows", topRatedTV))
                    sectionPages["top_rated_tv"] = 0
                }

                // 9. Recommendations (Local)
                val recommendations = movieRepository.getRecommendations(limit = PAGE_SIZE, offset = 0)
                android.util.Log.d(TAG, "Fetched ${recommendations.size} recommendations")
                if (recommendations.isNotEmpty()) {
                    sections.add(HomeSection("recommendations", "Recommendations", recommendations))
                    sectionPages["recommendations"] = 0
                }

                // 10. Old Gold Movies (Local)
                val oldGold = movieRepository.getOldGoldMovies(limit = PAGE_SIZE, offset = 0)
                android.util.Log.d(TAG, "Fetched ${oldGold.size} old gold movies")
                if (oldGold.isNotEmpty()) {
                    sections.add(HomeSection("old_gold", "Old Gold", oldGold))
                    sectionPages["old_gold"] = 0
                }

                _uiState.update { it.copy(sections = sections, isLoading = false, error = null) }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading home data", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun loadMore(sectionId: String) {
        val currentSections = _uiState.value.sections
        val sectionIndex = currentSections.indexOfFirst { it.id == sectionId }
        if (sectionIndex == -1 || currentSections[sectionIndex].isLoadingMore) return

        val nextPage = (sectionPages[sectionId] ?: 0) + 1
        android.util.Log.d(TAG, "Requesting more items for section: $sectionId, page: $nextPage")
        
        viewModelScope.launch {
            // Set loading more state
            val updatedSections = currentSections.toMutableList()
            updatedSections[sectionIndex] = updatedSections[sectionIndex].copy(isLoadingMore = true)
            _uiState.update { it.copy(sections = updatedSections) }

            try {
                val newItems = when (sectionId) {
                    "trending_movies" -> {
                        val (items, newPage) = tmdbRepository.getTrendingMoviesDeep(nextPage)
                        sectionPages[sectionId] = newPage
                        items
                    }
                    "trending_tv" -> {
                        val (items, newPage) = tmdbRepository.getTrendingTVShowsDeep(nextPage)
                        sectionPages[sectionId] = newPage
                        items
                    }
                    "recent" -> movieRepository.getRecentlyAddedMovies(limit = PAGE_SIZE, offset = nextPage * PAGE_SIZE)
                    "series" -> tvShowRepository.getNewTVShows(limit = PAGE_SIZE, offset = nextPage * PAGE_SIZE)
                    "drakor" -> tvShowRepository.getKoreanDramas(limit = PAGE_SIZE, offset = nextPage * PAGE_SIZE)
                    "indo" -> movieRepository.getIndonesianMovies(limit = PAGE_SIZE, offset = nextPage * PAGE_SIZE)
                    "top_rated_movies" -> movieRepository.getTopRatedMovies(limit = PAGE_SIZE, offset = nextPage * PAGE_SIZE)
                    "top_rated_tv" -> tvShowRepository.getTopRatedTVShows(limit = PAGE_SIZE, offset = nextPage * PAGE_SIZE)
                    "recommendations" -> movieRepository.getRecommendations(limit = PAGE_SIZE, offset = nextPage * PAGE_SIZE)
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
                                is com.theflexproject.thunder.model.Movie -> "m_${it.id}"
                                is com.theflexproject.thunder.model.TVShowInfo.TVShow -> "t_${it.id}"
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
