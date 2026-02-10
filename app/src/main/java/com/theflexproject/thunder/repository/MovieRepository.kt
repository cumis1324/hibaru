package com.theflexproject.thunder.repository

import com.theflexproject.thunder.database.MovieDao
import com.theflexproject.thunder.model.Movie
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepository @Inject constructor(
    private val movieDao: MovieDao
) {
    
    fun getAllMovies(): Flow<List<Movie>> {
        return movieDao.getAllMovies()
    }
    
    fun getMovieById(id: Int): Flow<Movie?> {
        return movieDao.getMovieById(id)
    }
    
    suspend fun insertMovie(movie: Movie) {
        movieDao.insert(movie)
    }
    
    suspend fun updateMovie(movie: Movie) {
        movieDao.update(movie)
    }
    
    suspend fun deleteMovie(movie: Movie) {
        movieDao.delete(movie)
    }
    
    suspend fun searchMovies(query: String): List<Movie> {
        return movieDao.searchMovies(query)
    }
}
