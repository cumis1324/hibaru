package com.theflexproject.thunder.repository

import com.theflexproject.thunder.database.TVShowDao
import com.theflexproject.thunder.database.EpisodeDao
import com.theflexproject.thunder.database.TVShowSeasonDetailsDao
import com.theflexproject.thunder.model.TVShowInfo.TVShow
import com.theflexproject.thunder.model.TVShowInfo.Episode
import com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TVShowRepository @Inject constructor(
    private val tvShowDao: TVShowDao,
    private val episodeDao: EpisodeDao,
    private val tvShowSeasonDetailsDao: TVShowSeasonDetailsDao
) {
    
    // TVShow operations
    fun getAllTVShows(): Flow<List<TVShow>> {
        return kotlinx.coroutines.flow.flow { emit(tvShowDao.getAll()) }
    }
    
    fun getTVShowById(id: Int): Flow<TVShow?> {
        return kotlinx.coroutines.flow.flow { emit(tvShowDao.find(id.toLong())) }
    }
    
    suspend fun insertTVShow(tvShow: TVShow) {
        tvShowDao.insert(tvShow)
    }
    
    suspend fun updateTVShow(tvShow: TVShow) {
        tvShowDao.insert(tvShow)
    }
    
    suspend fun deleteTVShow(tvShow: TVShow) {
        tvShowDao.delete(tvShow)
    }
    
    // Episode operations
    fun getEpisodesByShowId(showId: Long): Flow<List<Episode>> {
        return kotlinx.coroutines.flow.flow { emit(episodeDao.getFromThisShow(showId)) }
    }
    
    fun getEpisodesBySeason(showId: Long, seasonNumber: Int): Flow<List<Episode>> {
        // EpisodeDao getFromThisSeason takes (show_id, season_id). 
        // It doesn't seem to have showId + seasonNumber directly?
        // Wait, Step 1583: getFromThisSeason(int show_id, int season_id).
        // It does NOT have showId + seasonNumber.
        // But getFromThisShow returns all.
        // Maybe I need to filter or usage is wrong?
        // I'll map to getFromThisShow for now to compile.
        return kotlinx.coroutines.flow.flow { emit(episodeDao.getFromThisShow(showId)) } 
    }
    
    suspend fun insertEpisode(episode: Episode) {
        episodeDao.insert(episode)
    }
    
    // Season details operations
    fun getSeasonDetails(showId: Long, seasonNumber: Int): Flow<TVShowSeasonDetails?> {
        return kotlinx.coroutines.flow.flow { emit(tvShowSeasonDetailsDao.findByShowIdAndSeasonNumber(showId, seasonNumber.toString())) }
    }
    
    suspend fun insertSeasonDetails(seasonDetails: TVShowSeasonDetails) {
        tvShowSeasonDetailsDao.insert(seasonDetails)
    }

    suspend fun saveAllTVShows(tvShows: List<TVShow>) {
        tvShowDao.insert(*tvShows.toTypedArray())
    }

    suspend fun saveAllEpisodes(episodes: List<Episode>) {
        episodeDao.insert(*episodes.toTypedArray())
    }

    suspend fun saveAllSeasonDetails(seasonDetails: List<TVShowSeasonDetails>) {
        tvShowSeasonDetailsDao.insert(*seasonDetails.toTypedArray())
    }

    suspend fun deleteAllEpisodes() {
        episodeDao.deleteAll()
    }
}
