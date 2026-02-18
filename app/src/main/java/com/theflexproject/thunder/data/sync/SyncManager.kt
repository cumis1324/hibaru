package com.theflexproject.thunder.data.sync

import android.util.Log
import com.theflexproject.thunder.network.NFGPlusApi
import com.theflexproject.thunder.network.dto.toMovies
import com.theflexproject.thunder.network.dto.toTVShows
import com.theflexproject.thunder.network.dto.toEpisodes
import com.theflexproject.thunder.network.dto.toSeasonDetails
import com.theflexproject.thunder.repository.MovieRepository
import com.theflexproject.thunder.repository.TVShowRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val api: NFGPlusApi,
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TVShowRepository,
    private val tmdbRepository: com.theflexproject.thunder.repository.TmdbRepository,
    private val syncPrefs: SyncPrefs
) {
    companion object {
        private const val TAG = "SyncManager"
        // ... (pre-populated db time stays)
        private const val PRE_POPULATED_DB_TIME = 1770963377000L
        
        // Demo Admin UIDs
        private val ADMIN_USER_UIDS = listOf(
            "M20Oxpp64gZ480Lqus4afv6x2n63",
            "k6skp0IByvQ7LF1NrvBRRHprVIg1"
        )
    }

    private val auth by lazy { com.google.firebase.auth.FirebaseAuth.getInstance() }

    suspend fun syncAll(): Unit = withContext(Dispatchers.IO) {
        if (!syncPrefs.isSyncEnabled) {
            return@withContext
        }

        try {
            // Check Demo Mode
            val currentUser = auth.currentUser
            val isDemoUser = currentUser != null && ADMIN_USER_UIDS.contains(currentUser.uid)
            val previousMode = syncPrefs.isDemoMode
            
            if (isDemoUser != previousMode) {
                forceResetSync(false) // Reset DB but don't recurse syncAll immediately
                syncPrefs.isDemoMode = isDemoUser
                syncPrefs.lastSyncTime = 0L // Ensure fresh sync
            }

          
            
            syncMovies(isDemoUser)
            syncTVShows(isDemoUser)
            
            // PRE-CALCULATE TRENDING & GENRES
            syncTrending()
            syncGenres()
            
            syncPrefs.lastSyncTime = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
        }
    }

    /**
     * Fetches genres from the API and caches them in SyncPrefs for offline/instant access.
     */
    private suspend fun syncGenres() {
        Log.d(TAG, ">>> Start Genre Synchronization")
        try {
            val response = api.getGenres()
            if (response.isSuccessful) {
                val genres = response.body()?.genres ?: emptyList()
                if (genres.isNotEmpty()) {
                    val gson = com.google.gson.Gson()
                    val json = gson.toJson(genres)
                    syncPrefs.cachedGenresJson = json
                    Log.d(TAG, "<<< Genre Synchronization Finished. Saved ${genres.size} genres.")
                }
            } else {
                Log.e(TAG, "Failed to sync genres: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing genres", e)
        }
    }

    /**
     * Pre-calculates the trending list during sync to avoid heavy computation/API calls on Home.
     */
    private suspend fun syncTrending() {
        Log.d(TAG, ">>> Start Trending Pre-calculation")
        try {
            val (trendingMovies, _) = tmdbRepository.getTrendingMoviesDeep(1)
            val (trendingTV, _) = tmdbRepository.getTrendingTVShowsDeep(1)
            
            // Combine and format IDs (e.g., m_123, t_456)
            val movieIds = trendingMovies.map { "m_${it.id}" }
            val tvIds = trendingTV.map { "t_${it.id}" }
            
            val combinedIds = (movieIds + tvIds).shuffled()
            
            // Convert to a simple CSV string to avoid GSON dependency for now
            val idsString = combinedIds.joinToString(",")
            syncPrefs.trendingItemsJson = idsString
            
            Log.d(TAG, "<<< Trending Pre-calculation Finished. Found ${combinedIds.size} items.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync trending items", e)
        }
    }

    suspend fun forceResetSync(performSync: Boolean = true): Unit = withContext(Dispatchers.IO) {
        Log.d(TAG, "Force resetting sync...")
        movieRepository.deleteAll()
        tvShowRepository.deleteAllEpisodes()
        // tvShowDao.deleteAll() // If needed, but usually we just want to clear files/episodes
        syncPrefs.lastSyncTime = 0L
        if (performSync) syncAll()
    }

    private fun getLastSyncString(): String {
        var lastSync = syncPrefs.lastSyncTime
        
        // If lastSync is 0 (fresh install), use the pre-populated DB timestamp
        if (lastSync == 0L) {
            lastSync = PRE_POPULATED_DB_TIME
            Log.d(TAG, "[INIT] Fresh install detected. Using baseline timestamp: $lastSync")
        } else {
            Log.d(TAG, "[SYNC] Resinuming from last successful sync: $lastSync")
        }
        
        return lastSync.toString()
    }

    private suspend fun syncMovies(isDemoMode: Boolean) {
        Log.d(TAG, ">>> Start Movie Sync (Demo: $isDemoMode)")
        val lastSync = getLastSyncString()
        
        var offset = 0
        val limit = 50
        var hasMore = true
        var totalSaved = 0
        
        while (hasMore) {
            Log.d(TAG, "Requesting Movies: offset=$offset, limit=$limit, updated_after=$lastSync")
            val response = api.getMovies(limit = limit, offset = offset, updatedAfter = lastSync, demoMode = isDemoMode) 
            if (response.isSuccessful) {
                val dtos = response.body()?.movies ?: emptyList()
                Log.d(TAG, "API Response: Found ${dtos.size} movies")
                
                if (dtos.isNotEmpty()) {
                    val entities = dtos.toMovies()
                    movieRepository.saveAll(entities)
                    totalSaved += entities.size
                    
                    // Detailed item logging
                    entities.forEach { movie -> 
                        Log.d(TAG, "  [MOVIE] Saved: '${movie.title}' (TMDB: ${movie.id}, RoomPK: ${movie.fileidForDB}, Disabled: ${movie.disabled})")
                    }
                    
                    Log.d(TAG, "Batch Saved: ${entities.size} movies. Cumulative total: $totalSaved")
                    
                    if (dtos.size < limit) {
                        hasMore = false
                    } else {
                        offset += limit
                    }
                } else {
                    hasMore = false
                }
            } else {
                Log.e(TAG, "HTTP Error during Movie Sync (offset $offset): ${response.code()} ${response.message()}")
                hasMore = false
            }
        }
        Log.d(TAG, "<<< Movie Sync Finished. Total processed: $totalSaved")
    }

    private suspend fun syncTVShows(isDemoMode: Boolean) {
        Log.d(TAG, ">>> Start TV Show Sync (Demo: $isDemoMode)")
        val lastSync = getLastSyncString()
        
        var offset = 0
        val limit = 50
        var hasMore = true
        var totalShowsSaved = 0
        
        while (hasMore) {
            Log.d(TAG, "Requesting TV Shows: offset=$offset, limit=$limit, updated_after=$lastSync")
            val response = api.getTVShows(limit = limit, offset = offset, updatedAfter = lastSync, demoMode = isDemoMode)
            if (response.isSuccessful) {
                val dtos = response.body()?.tvshows ?: emptyList()
                Log.d(TAG, "API Response: Found ${dtos.size} shows")
                
                if (dtos.isNotEmpty()) {
                    val entities = dtos.toTVShows()
                    tvShowRepository.saveAllTVShows(entities)
                    totalShowsSaved += entities.size
                    Log.d(TAG, "Batch Saved: ${entities.size} shows. Cumulative total: $totalShowsSaved")
                    
                    // Sync Additional Data for each show
                    entities.forEach { show ->
                         try {
                             Log.d(TAG, "Syncing Sub-Resources for Show [${show.name}] (ID: ${show.id})")
                             
                             // Sync Episodes
                             val episodesResponse = api.getEpisodes(show.id, demoMode = isDemoMode)
                             if (episodesResponse.isSuccessful) {
                                 val episodeDtos = episodesResponse.body()?.episodes ?: emptyList()
                                 if (episodeDtos.isNotEmpty()) {
                                     val episodeEntities = episodeDtos.toEpisodes()
                                     episodeEntities.forEach { it.show_id = show.id.toLong() }
                                     tvShowRepository.saveAllEpisodes(episodeEntities)
                                     Log.d(TAG, "  -> Saved ${episodeEntities.size} episodes")
                                 } else {
                                     Log.d(TAG, "  -> No episodes found")
                                 }
                             } else {
                                 Log.w(TAG, "  -> Failed to fetch episodes: ${episodesResponse.code()}")
                             }
                             
                             // Sync Seasons
                             val seasonsResponse = api.getSeasons(show.id, demoMode = isDemoMode)
                             if (seasonsResponse.isSuccessful) {
                                 val seasonDtos = seasonsResponse.body()?.seasons ?: emptyList()
                                 if (seasonDtos.isNotEmpty()) {
                                     val seasonEntities = seasonDtos.toSeasonDetails()
                                     seasonEntities.forEach { it.show_id = show.id.toLong() }
                                     tvShowRepository.saveAllSeasonDetails(seasonEntities)
                                     Log.d(TAG, "  -> Saved ${seasonEntities.size} seasons")
                                 } else {
                                     Log.d(TAG, "  -> No seasons found")
                                 }
                             } else {
                                 Log.w(TAG, "  -> Failed to fetch seasons: ${seasonsResponse.code()}")
                             }
                         } catch (e: Exception) {
                             Log.e(TAG, "Error syncing sub-resources for ${show.name}", e)
                         }
                    }
                    
                    if (dtos.size < limit) {
                        hasMore = false
                    } else {
                        offset += limit
                    }
                } else {
                    hasMore = false
                }
            } else {
                Log.e(TAG, "HTTP Error during TV Sync (offset $offset): ${response.code()} ${response.message()}")
                hasMore = false
            }
        }
        Log.d(TAG, "<<< TV Show Sync Finished. Total processed: $totalShowsSaved")
    }
}
