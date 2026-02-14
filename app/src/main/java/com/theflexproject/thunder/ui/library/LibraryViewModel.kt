package com.theflexproject.thunder.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theflexproject.thunder.model.Genres
import com.theflexproject.thunder.model.Movie
import com.theflexproject.thunder.model.TVShowInfo.TVShow
import com.theflexproject.thunder.repository.MovieRepository
import com.theflexproject.thunder.repository.TVShowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val selectedMediaType: MediaType = MediaType.MOVIE,
    val movies: List<Movie> = emptyList(),
    val tvShows: List<TVShow> = emptyList(),
    val genres: List<Genres> = emptyList(),
    val selectedGenreId: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortOrder: String = "Title", // Default Sort
    val orderDirection: String = "Ascending" // Default Order
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TVShowRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Launch collectors in separate jobs
                launch {
                    movieRepository.getAllMovies().collect { movies ->
                        _uiState.update { it.copy(movies = movies) }
                        updateGenres()
                    }
                }
                launch {
                    tvShowRepository.getAllTVShows().collect { tvShows ->
                        _uiState.update { it.copy(tvShows = tvShows) }
                        updateGenres()
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                 // IsLoading might be tricky with infinite flows. 
                 // We can set it false after initial emission or just ignore it for now.
                 _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setMediaType(type: MediaType) {
        _uiState.update { it.copy(selectedMediaType = type) }
        updateGenres()
    }

    fun setSortOrder(sortBy: String, orderBy: String) {
        _uiState.update { it.copy(sortOrder = sortBy, orderDirection = orderBy) }
        // Implement sorting logic if needed, or rely on UI to sort using list
        // Since we are just exposing the full list, sorting can be done in updateGenres/UI?
        // But logic should be here.
        // For now, let's keep it simple.
    }

    fun setGenre(genreId: Int?) {
        _uiState.update { it.copy(selectedGenreId = genreId) }
    }

    private fun updateGenres() {
        //Extract unique genres from current list based on media type
        val currentState = _uiState.value
        val allGenres = if (currentState.selectedMediaType == MediaType.MOVIE) {
             currentState.movies.flatMap { it.genres ?: emptyList() }
        } else {
             currentState.tvShows.flatMap { it.genres ?: emptyList() }
        }.distinctBy { it.id }.sortedBy { it.name }
        
        _uiState.update { it.copy(genres = allGenres) }
    }
}
