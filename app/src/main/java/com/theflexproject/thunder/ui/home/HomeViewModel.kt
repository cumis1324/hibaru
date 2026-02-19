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
import kotlin.coroutines.resume

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
        loadContinueWatching()
        loadWatchlist()
    }

    fun loadContinueWatching() {
        viewModelScope.launch {
            android.util.Log.d(TAG, "Loading Continue Watching data from Firebase")
            val history = fetchHistoryFromFirebase()
            if (history.isEmpty()) {
                android.util.Log.d(TAG, "No history found in Firebase")
                return@launch
            }

            val continueWatchingItems = mutableListOf<com.theflexproject.thunder.model.MyMedia>()
            
            history.forEach { (tmdbId, _) ->
                val idInt = tmdbId.toIntOrNull() ?: return@forEach
                
                // 1. Check if it's a Movie ID
                val movieResult = movieRepository.loadAllByIds(listOf(tmdbId))
                if (movieResult.isNotEmpty()) {
                    continueWatchingItems.add(movieResult[0])
                } else {
                    // 2. Check if it's an Episode ID -> Resolve to TVShow
                    val show = tvShowRepository.getTVShowByEpisodeId(idInt)
                    if (show != null) {
                        continueWatchingItems.add(show)
                    }
                }
            }

            if (continueWatchingItems.isNotEmpty()) {
                val distinctItems = continueWatchingItems.distinctBy { 
                    when(it) {
                        is Movie -> "m_${it.id}"
                        is TVShow -> "t_${it.id}"
                        else -> it.toString()
                    }
                }
                
                android.util.Log.d(TAG, "Found ${distinctItems.size} items for Continue Watching")
                val section = HomeSection("continue_watching", "Continue Watching", distinctItems)
                
                _uiState.update { state ->
                    val newSections = state.sections.toMutableList()
                    newSections.removeAll { it.id == "continue_watching" }
                    // Add at position 0 (top)
                    newSections.add(0, section)
                    state.copy(sections = newSections)
                }
            }
        }
    }

    private suspend fun fetchHistoryFromFirebase(): List<Pair<String, Long>> = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        val manager = com.theflexproject.thunder.model.FirebaseManager()
        val user = manager.currentUser
        if (user == null) {
            continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }

        val databaseReference = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("History")
        val historyRef = databaseReference.child(user.uid)

        historyRef.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val list = mutableListOf<Pair<String, Long>>()
                val formatter = java.time.format.DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", java.util.Locale.ENGLISH)
                
                snapshot.children.forEach { child ->
                    val tmdbId = child.key ?: return@forEach
                    val lastPlayedStr = child.child("lastPlayed").getValue(String::class.java)
                    
                    try {
                        val timestamp = if (!lastPlayedStr.isNullOrEmpty()) {
                            java.time.ZonedDateTime.parse(lastPlayedStr, formatter).toInstant().toEpochMilli()
                        } else 0L
                        list.add(tmdbId to timestamp)
                    } catch (e: Exception) {
                        list.add(tmdbId to 0L)
                    }
                }
                // Sort by timestamp desc and take top 20
                continuation.resume(list.sortedByDescending { it.second }.take(20))
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                continuation.resume(emptyList())
            }
        })
    }

    fun loadWatchlist() {
        viewModelScope.launch {
            android.util.Log.d(TAG, "Loading Watchlist data from Firebase")
            val wishlistIds = fetchWatchlistFromFirebase()
            if (wishlistIds.isEmpty()) {
                android.util.Log.d(TAG, "No watchlist items found in Firebase")
                return@launch
            }

            val watchlistItems = mutableListOf<com.theflexproject.thunder.model.MyMedia>()
            
            wishlistIds.forEach { tmdbId ->
                val idInt = tmdbId.toIntOrNull() ?: return@forEach
                
                // 1. Check if it's a Movie ID
                val movieResult = movieRepository.loadAllByIds(listOf(tmdbId))
                if (movieResult.isNotEmpty()) {
                    watchlistItems.add(movieResult[0])
                } else {
                    // 2. Check if it's a TV Show ID
                    tvShowRepository.getTVShowById(idInt).collect { show ->
                        if (show != null) {
                            watchlistItems.add(show)
                        }
                    }
                }
            }

            if (watchlistItems.isNotEmpty()) {
                val distinctItems = watchlistItems.distinctBy { 
                    when(it) {
                        is Movie -> "m_${it.id}"
                        is TVShow -> "t_${it.id}"
                        else -> it.toString()
                    }
                }
                
                android.util.Log.d(TAG, "Found ${distinctItems.size} items for Watchlist")
                val section = HomeSection("watchlist", "My Watchlist", distinctItems)
                
                _uiState.update { state ->
                    val newSections = state.sections.toMutableList()
                    newSections.removeAll { it.id == "watchlist" }
                    
                    // Insert after continue_watching if it exists, otherwise at top
                    val continueWatchingIdx = newSections.indexOfFirst { it.id == "continue_watching" }
                    if (continueWatchingIdx != -1) {
                        newSections.add(continueWatchingIdx + 1, section)
                    } else {
                        newSections.add(0, section)
                    }
                    state.copy(sections = newSections)
                }
            }
        }
    }

    private suspend fun fetchWatchlistFromFirebase(): List<String> = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        val manager = com.theflexproject.thunder.model.FirebaseManager()
        val user = manager.currentUser
        if (user == null) {
            continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }

        val databaseReference = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("Favorit")
        val watchlistRef = databaseReference.child(user.uid)

        watchlistRef.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val list = mutableListOf<String>()
                snapshot.children.forEach { child ->
                    val tmdbId = child.key ?: return@forEach
                    val value = child.child("value").getValue(Int::class.java)
                    if (value == 1) {
                        list.add(tmdbId)
                    }
                }
                continuation.resume(list.reversed()) // Newest first? Firebase keys aren't necessarily sorted by time here
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                continuation.resume(emptyList())
            }
        })
    }

    fun loadHomeData() {
        viewModelScope.launch {
            android.util.Log.d(TAG, "Loading initial home data (isTVDevice: $isTVDevice)")
            _uiState.update { it.copy(isLoading = true, sections = it.sections.filter { s -> s.id == "continue_watching" || s.id == "watchlist" }) }
            sectionPages.clear()
            genreBatchIndex = 0
            
            try {
                val sections = mutableListOf<HomeSection>()
                val manager = com.theflexproject.thunder.model.FirebaseManager()
                val currentUser = manager.currentUser
                val isAdmin = currentUser?.uid?.let { com.theflexproject.thunder.Constants.isAdmin(it) } ?: false

                if (isAdmin) {
                    android.util.Log.d(TAG, "Admin detected, showing only Recently Added")
                    val recentMovies = movieRepository.getRecentlyAddedMovies(PAGE_SIZE, 0)
                    if (recentMovies.isNotEmpty()) {
                        sections.add(HomeSection("recent_movies", "Recently Added", recentMovies))
                        sectionPages["recent_movies"] = 0
                    }
                } else {
                    // Normal User logic - Multi-section loading
                    
                    // 1. Trending (HERO)
                    val cachedTrendingIds = syncPrefs.trendingItemsJson
                    val trendingItems: List<com.theflexproject.thunder.model.MyMedia> = if (!cachedTrendingIds.isNullOrEmpty()) {
                        val idList = cachedTrendingIds.split(",")
                        val movies = movieRepository.loadAllByIds(idList.filter { it.startsWith("m_") }.map { it.removePrefix("m_") })
                        val tvShows = tvShowRepository.loadAllTVShowsByIds(idList.filter { it.startsWith("t_") }.map { it.removePrefix("t_") })
                        val combined: List<com.theflexproject.thunder.model.MyMedia> = movies + tvShows
                        combined.sortedBy { item ->
                            val idStr = when(item) {
                                is Movie -> "m_${item.id}"
                                is TVShow -> "t_${item.id}"
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
                        sections.add(HomeSection("trending_hero", "Trending", trendingItems, com.theflexproject.thunder.ui.home.SectionType.HERO))
                    }

                    // 2. Curated Local Sections
                    
                    // Recently Added
                    val recentMovies = movieRepository.getRecentlyAddedMovies(PAGE_SIZE, 0)
                    if (recentMovies.isNotEmpty()) {
                        sections.add(HomeSection("recent_movies", "Recently Added", recentMovies))
                        sectionPages["recent_movies"] = 0
                    }

                    // New TV Shows
                    val newTv = tvShowRepository.getNewTVShows(PAGE_SIZE, 0)
                    if (newTv.isNotEmpty()) {
                        sections.add(HomeSection("new_tv", "New Series", newTv))
                        sectionPages["new_tv"] = 0
                    }

                    // Film Indonesia
                    val indoMovies = movieRepository.getIndonesianMovies(PAGE_SIZE, 0)
                    if (indoMovies.isNotEmpty()) {
                        sections.add(HomeSection("indo_movies", "Indonesian Movies", indoMovies))
                        sectionPages["indo_movies"] = 0
                    }

                    // Drama Korea (Movies + TV)
                    val drakorTv = tvShowRepository.getKoreanDramas(PAGE_SIZE, 0)
                    val drakorMovies = movieRepository.getKoreanMovies(PAGE_SIZE, 0)
                    val drakorCombined = (drakorTv + drakorMovies).sortedByDescending { 
                        when(it) {
                            is Movie -> it.popularity
                            is TVShow -> it.popularity
                            else -> 0.0
                        }
                    }
                    if (drakorCombined.isNotEmpty()) {
                        sections.add(HomeSection("drakor", "Korean Drama", drakorCombined.take(PAGE_SIZE)))
                        sectionPages["drakor"] = 0
                    }

                    // Top Rated
                    val topMovies = movieRepository.getTopRatedMovies(PAGE_SIZE, 0)
                    if (topMovies.isNotEmpty()) {
                        sections.add(HomeSection("top_movies", "Top Rated Movies", topMovies))
                        sectionPages["top_movies"] = 0
                    }
                    
                    val topTv = tvShowRepository.getTopRatedTVShows(PAGE_SIZE, 0)
                    if (topTv.isNotEmpty()) {
                        sections.add(HomeSection("top_tv", "Top Rated Series", topTv))
                        sectionPages["top_tv"] = 0
                    }

                    // Recommendations
                    val recom = movieRepository.getRecommendations(PAGE_SIZE, 0)
                    if (recom.isNotEmpty()) {
                        sections.add(HomeSection("recom", "Recommended for You", recom))
                        sectionPages["recom"] = 0
                    }

                    // Old Gold
                    val oldGold = movieRepository.getOldGoldMovies(PAGE_SIZE, 0)
                    if (oldGold.isNotEmpty()) {
                        sections.add(HomeSection("old_gold", "Nostalgic Classics", oldGold))
                        sectionPages["old_gold"] = 0
                    }
                }

                // 3. Prepare Genres for Infinite Scroll (Always available? or only for non-admin?)
                // The user said "ONLY section baru ditambahkan", so I'll skip genres for admin too.
                if (!isAdmin) {
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
                } else {
                    allGenresPool = emptyList()
                }

                _uiState.update { state ->
                    val finalSections = mutableListOf<HomeSection>()
                    if (!isAdmin) {
                        state.sections.find { it.id == "continue_watching" }?.let { finalSections.add(it) }
                        state.sections.find { it.id == "watchlist" }?.let { finalSections.add(it) }
                    }
                    finalSections.addAll(sections)
                    state.copy(sections = finalSections.toList(), isLoading = false, error = null)
                }
                
                if (!isAdmin) {
                    // Load first batch of genres immediately after curated stuff
                    loadNextGenreBatch()
                }

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
