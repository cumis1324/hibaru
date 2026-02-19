package com.theflexproject.thunder.repository

import com.theflexproject.thunder.database.MovieDao
import com.theflexproject.thunder.model.Movie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
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
        return kotlinx.coroutines.flow.flow { emit(movieDao.getAll()) }.flowOn(Dispatchers.IO)
    }
    
    fun getMovieById(id: Int): Flow<Movie?> {
        return kotlinx.coroutines.flow.flow { emit(movieDao.byId(id)) }.flowOn(Dispatchers.IO)
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
    
    suspend fun searchMovies(query: String): List<Movie> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        movieDao.getSearchQuery(query)
    }

    suspend fun saveAll(movies: List<Movie>) = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val gdIds = movies.mapNotNull { it.gd_id }
        if (gdIds.isNotEmpty()) {
            val existingMovies = movieDao.getMoviesByGdIds(gdIds)
            val existingMap = existingMovies.associateBy { it.gd_id }
            
            movies.forEach { movie ->
                val existing = existingMap[movie.gd_id]
                if (existing != null && !movie.gd_id.isNullOrBlank()) {
                    // Preserve local metadata during sync
                    if (movie.localPath.isNullOrEmpty()) {
                        movie.localPath = existing.localPath
                    }
                    if (movie.downloadId == 0L || movie.downloadId == -1L) {
                        movie.downloadId = existing.downloadId
                    }
                    if (movie.file_name.isNullOrEmpty()) {
                        movie.file_name = existing.file_name
                    }
                }
            }
        }
        movieDao.insert(*movies.toTypedArray())
    }

    suspend fun getMovieCount(): Int = kotlinx.coroutines.withContext(Dispatchers.IO) {
        movieDao.movieCount
    }

    suspend fun getTrendingMovies(limit: Int = 10, offset: Int = 0): List<Movie> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        movieDao.getTrending(limit, offset)
    }

    suspend fun getTopRatedMovies(limit: Int = 10, offset: Int = 0): List<Movie> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        movieDao.getTopRated(limit, offset)
    }

    suspend fun getRecentlyAddedMovies(limit: Int = 10, offset: Int = 0): List<Movie> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        movieDao.getrecentlyadded(limit, offset)
    }

    suspend fun getRecentReleases(limit: Int = 10, offset: Int = 0): List<Movie> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        movieDao.getrecentreleases(limit, offset)
    }

    suspend fun getIndonesianMovies(limit: Int = 10, offset: Int = 0): List<Movie> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        movieDao.getFilmIndo(limit, offset)
    }

    suspend fun getRecommendations(limit: Int = 10, offset: Int = 0): List<Movie> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        movieDao.getrecomendation(limit, offset)
    }

    suspend fun getWatchlist(): List<Movie> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        movieDao.getWatchlisted()
    }

    suspend fun updatePlayed(id: Int, dateTime: String) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        movieDao.updatePlayed(id, dateTime)
    }

    suspend fun getOldGoldMovies(limit: Int = 10, offset: Int = 0): List<Movie> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        movieDao.getOgMovies(limit, offset)
    }

    suspend fun getKoreanMovies(limit: Int = 10, offset: Int = 0): List<Movie> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        movieDao.getDrakor(limit, offset)
    }

    suspend fun deleteAll() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        movieDao.deleteAll()
    }

    suspend fun loadAllByIds(ids: List<String>): List<Movie> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        movieDao.loadAllByIds(ids)
    }
}
