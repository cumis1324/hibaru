package com.theflexproject.thunder.repository

import com.theflexproject.thunder.Constants.TMDB_API_KEY
import com.theflexproject.thunder.database.MovieDao
import com.theflexproject.thunder.database.TVShowDao
import com.theflexproject.thunder.model.Movie
import com.theflexproject.thunder.model.TVShowInfo.TVShow
import com.theflexproject.thunder.network.TmdbApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbRepository @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val movieDao: MovieDao,
    private val tvShowDao: TVShowDao
) {
    companion object {
        private const val DEEP_PAGING_MAX_RETRIES = 5
        private const val MIN_ITEMS_PER_FETCH = 15
    }

    /**
     * Fetches trending movies from TMDB with "Deep Search".
     * If local results are sparse, it fetches subsequent pages automatically.
     */
    suspend fun getTrendingMoviesDeep(startPage: Int): Pair<List<Movie>, Int> = withContext(Dispatchers.IO) {
        val allItems = mutableListOf<Movie>()
        var currentPage = startPage
        var retries = 0

        while (allItems.size < MIN_ITEMS_PER_FETCH && retries < DEEP_PAGING_MAX_RETRIES) {
            val response = try {
                tmdbApi.getTrendingMovies(apiKey = TMDB_API_KEY, page = currentPage)
            } catch (e: Exception) {
                null
            }

            if (response?.isSuccessful == true) {
                val tmdbIds = response.body()?.results?.map { it.id.toString() } ?: emptyList()
                val localItems = getMoviesOrderedByIds(tmdbIds)
                allItems.addAll(localItems)
                
                if (localItems.isEmpty() && tmdbIds.isNotEmpty()) {
                    // We found items on TMDB but none locally, keep searching
                    currentPage++
                    retries++
                } else {
                    break // Stop if we found something or TMDB is empty
                }
            } else {
                break
            }
        }
        return@withContext Pair(allItems.distinctBy { it.id }, currentPage)
    }

    suspend fun getTrendingTVShowsDeep(startPage: Int): Pair<List<TVShow>, Int> = withContext(Dispatchers.IO) {
        val allItems = mutableListOf<TVShow>()
        var currentPage = startPage
        var retries = 0

        while (allItems.size < MIN_ITEMS_PER_FETCH && retries < DEEP_PAGING_MAX_RETRIES) {
            val response = try {
                tmdbApi.getTrendingTVShows(apiKey = TMDB_API_KEY, page = currentPage)
            } catch (e: Exception) {
                null
            }

            if (response?.isSuccessful == true) {
                val tmdbIds = response.body()?.results?.map { it.id.toString() } ?: emptyList()
                val localItems = getTVShowsOrderedByIds(tmdbIds)
                allItems.addAll(localItems)

                if (localItems.isEmpty() && tmdbIds.isNotEmpty()) {
                    currentPage++
                    retries++
                } else {
                    break
                }
            } else {
                break
            }
        }
        return@withContext Pair(allItems.distinctBy { it.id }, currentPage)
    }

    /**
     * Fetches trending movies from TMDB and returns those found in local DB, 
     * maintaining the original TMDB ranking order.
     */
    suspend fun getTrendingMoviesFromTmdb(page: Int = 1): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getTrendingMovies(apiKey = TMDB_API_KEY, page = page)
            if (response.isSuccessful) {
                val tmdbIds = response.body()?.results?.map { it.id.toString() } ?: emptyList()
                return@withContext getMoviesOrderedByIds(tmdbIds)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }

    /**
     * Fetches trending TV shows from TMDB and returns those found in local DB, 
     * maintaining the original TMDB ranking order.
     */
    suspend fun getTrendingTVShowsFromTmdb(page: Int = 1): List<TVShow> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getTrendingTVShows(apiKey = TMDB_API_KEY, page = page)
            if (response.isSuccessful) {
                val tmdbIds = response.body()?.results?.map { it.id.toString() } ?: emptyList()
                return@withContext getTVShowsOrderedByIds(tmdbIds)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }

    /**
     * Helper to fetch movies from DB and sort them in the same order as the input IDs.
     */
    private fun getMoviesOrderedByIds(tmdbIds: List<String>): List<Movie> {
        if (tmdbIds.isEmpty()) return emptyList()
        val localMovies = movieDao.loadAllByIds(tmdbIds)
        // Sort by the position in the original tmdbIds list
        return localMovies.sortedBy { movie -> tmdbIds.indexOf(movie.id.toString()) }
    }

    /**
     * Helper to fetch TV shows from DB and sort them in the same order as the input IDs.
     */
    private fun getTVShowsOrderedByIds(tmdbIds: List<String>): List<TVShow> {
        if (tmdbIds.isEmpty()) return emptyList()
        val localShows = tvShowDao.loadAllByIds(tmdbIds)
        // Sort by the position in the original tmdbIds list
        return localShows.sortedBy { series -> tmdbIds.indexOf(series.id.toString()) }
    }

    suspend fun getPopularMoviesFromTmdb(page: Int = 1): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getPopularMovies(apiKey = TMDB_API_KEY, page = page)
            if (response.isSuccessful) {
                val tmdbIds = response.body()?.results?.map { it.id.toString() } ?: emptyList()
                return@withContext getMoviesOrderedByIds(tmdbIds)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }

    suspend fun getPopularTVShowsFromTmdb(page: Int = 1): List<TVShow> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getPopularTVShows(apiKey = TMDB_API_KEY, page = page)
            if (response.isSuccessful) {
                val tmdbIds = response.body()?.results?.map { it.id.toString() } ?: emptyList()
                return@withContext getTVShowsOrderedByIds(tmdbIds)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }

    suspend fun getSimilarMovies(movieId: Int): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getSimilarMovies(movieId, TMDB_API_KEY)
            if (response.isSuccessful) {
                val tmdbIds = response.body()?.results?.map { it.id.toString() } ?: emptyList()
                return@withContext getMoviesOrderedByIds(tmdbIds)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }

    suspend fun getRecommendationMovies(movieId: Int): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getRecommendationMovies(movieId, TMDB_API_KEY)
            if (response.isSuccessful) {
                val tmdbIds = response.body()?.results?.map { it.id.toString() } ?: emptyList()
                return@withContext getMoviesOrderedByIds(tmdbIds)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }

    suspend fun getMovieCredits(movieId: Int): com.theflexproject.thunder.model.Credits? = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getMovieCredits(movieId, TMDB_API_KEY)
            if (response.isSuccessful) {
                return@withContext response.body()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    suspend fun getSimilarTVShows(tvId: Int): List<TVShow> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getSimilarTVShows(tvId, TMDB_API_KEY)
            if (response.isSuccessful) {
                val tmdbIds = response.body()?.results?.map { it.id.toString() } ?: emptyList()
                return@withContext getTVShowsOrderedByIds(tmdbIds)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }

    suspend fun getRecommendationTVShows(tvId: Int): List<TVShow> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getRecommendationTVShows(tvId, TMDB_API_KEY)
            if (response.isSuccessful) {
                val tmdbIds = response.body()?.results?.map { it.id.toString() } ?: emptyList()
                return@withContext getTVShowsOrderedByIds(tmdbIds)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }

    suspend fun getTVShowCredits(tvId: Int): com.theflexproject.thunder.model.Credits? = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getTVShowCredits(tvId, TMDB_API_KEY)
            if (response.isSuccessful) {
                return@withContext response.body()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    suspend fun getMoviesByGenre(genreId: Int): List<Movie> = withContext(Dispatchers.IO) {
        return@withContext movieDao.getMoviesByGenre(genreId.toString())
    }

    suspend fun getTvShowsByGenre(genreId: Int): List<TVShow> = withContext(Dispatchers.IO) {
        return@withContext tvShowDao.getTvSeriesByGenreId(genreId.toString())
    }
}
