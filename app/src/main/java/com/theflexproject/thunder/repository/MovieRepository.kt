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
        // DAO returns List, not Flow. 
        // We need to wrap it or change DAO to return Flow.
        // For now, let's assume DAO returns List and we emit it? 
        // Or better, since we are in sync phase, maybe we don't need Flows yet?
        // But Repository signature says Flow.
        // I'll change Repository to suspend fun returning List? 
        // Or just use flow { emit(dao.getAll()) }
        // BUT DAO access on main thread is bad.
        // Let's change Repository to suspend functions for now to match DAO (which returns List).
        // Wait, current DAO signatures are: List<Movie> getAll().
        // So I must change Repository signatures.
        return kotlinx.coroutines.flow.flow { emit(movieDao.getAll()) }
    }
    
    fun getMovieById(id: Int): Flow<Movie?> {
        return kotlinx.coroutines.flow.flow { emit(movieDao.byId(id)) }
    }
    
    suspend fun insertMovie(movie: Movie) {
        movieDao.insert(movie)
    }
    
    suspend fun updateMovie(movie: Movie) {
        // MovieDao has no update, use insert (REPLACE)
        movieDao.insert(movie)
    }
    
    suspend fun deleteMovie(movie: Movie) {
        movieDao.delete(movie)
    }
    
    suspend fun searchMovies(query: String): List<Movie> {
        return movieDao.getSearchQuery(query)
    }

    suspend fun saveAll(movies: List<Movie>) {
        movieDao.insert(*movies.toTypedArray())
    }

    suspend fun getMovieCount(): Int {
        return movieDao.movieCount
    }

    suspend fun deleteAll() {
        movieDao.deleteAll()
    }
}
