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
        return tvShowDao.getAllTVShows()
    }
    
    fun getTVShowById(id: Int): Flow<TVShow?> {
        return tvShowDao.getTVShowById(id)
    }
    
    suspend fun insertTVShow(tvShow: TVShow) {
        tvShowDao.insert(tvShow)
    }
    
    suspend fun updateTVShow(tvShow: TVShow) {
        tvShowDao.update(tvShow)
    }
    
    suspend fun deleteTVShow(tvShow: TVShow) {
        tvShowDao.delete(tvShow)
    }
    
    // Episode operations
    fun getEpisodesByShowId(showId: Long): Flow<List<Episode>> {
        return episodeDao.getEpisodesByShowId(showId)
    }
    
    fun getEpisodesBySeason(showId: Long, seasonNumber: Int): Flow<List<Episode>> {
        return episodeDao.getEpisodesBySeason(showId, seasonNumber)
    }
    
    suspend fun insertEpisode(episode: Episode) {
        episodeDao.insert(episode)
    }
    
    // Season details operations
    fun getSeasonDetails(showId: Long, seasonNumber: Int): Flow<TVShowSeasonDetails?> {
        return tvShowSeasonDetailsDao.getSeasonDetails(showId, seasonNumber)
    }
    
    suspend fun insertSeasonDetails(seasonDetails: TVShowSeasonDetails) {
        tvShowSeasonDetailsDao.insert(seasonDetails)
    }
}
