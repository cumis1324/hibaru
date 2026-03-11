package com.theflexproject.thunder.data.sync

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.tvprovider.media.tv.Channel
import androidx.tvprovider.media.tv.ChannelLogoUtils
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.theflexproject.thunder.R
import com.theflexproject.thunder.repository.MovieRepository
import com.theflexproject.thunder.repository.TVShowRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class TvChannelSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TVShowRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "TvChannelSyncWorker"
        private const val CHANNEL_INTERNAL_ID = "nfgplus_trending_channel"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting TV Channel Sync (Multi-Channel)...")
            
            // 1. Sync Trending Movies Channel
            val movieChannelId = getOrCreateChannel("nfgplus Trending Movies", "nfgplus_movies_channel")
            if (movieChannelId != -1L) {
                syncMoviesToChannel(movieChannelId)
            }

            // 2. Sync Trending TV Shows Channel
            val tvChannelId = getOrCreateChannel("nfgplus Trending TV Shows", "nfgplus_tv_channel")
            if (tvChannelId != -1L) {
                syncTVShowsToChannel(tvChannelId)
            }

            Log.d(TAG, "TV Channel Sync Finished (Multi-Channel)")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing TV channels: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun syncMoviesToChannel(channelId: Long) {
        // Clear old programs
        applicationContext.contentResolver.delete(
            TvContractCompat.buildPreviewProgramsUriForChannel(channelId),
            null, null
        )

        val topMovies = movieRepository.getTrendingMovies(limit = 15)
        Log.d(TAG, "Adding ${topMovies.size} movies to channel $channelId")
        
        topMovies.forEach { movie ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("nfgplus://video/${movie.id}?isMovie=true")
            }
            
            val program = PreviewProgram.Builder()
                .setChannelId(channelId)
                .setType(TvContractCompat.PreviewPrograms.TYPE_MOVIE)
                .setTitle(movie.title)
                .setDescription(movie.overview)
                .setPosterArtUri(Uri.parse("https://image.tmdb.org/t/p/w500${movie.backdrop_path}"))
                .setIntentUri(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)))
                .setInternalProviderId("m_${movie.id}")
                .build()
            
            applicationContext.contentResolver.insert(TvContractCompat.PreviewPrograms.CONTENT_URI, program.toContentValues())
        }
    }

    private suspend fun syncTVShowsToChannel(channelId: Long) {
        // Clear old programs
        applicationContext.contentResolver.delete(
            TvContractCompat.buildPreviewProgramsUriForChannel(channelId),
            null, null
        )

        val topShows = tvShowRepository.getTrendingTVShows(limit = 15)
        Log.d(TAG, "Adding ${topShows.size} TV shows to channel $channelId")
        
        topShows.forEach { show ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("nfgplus://detail/${show.id}?type=tv")
            }
            
            val program = PreviewProgram.Builder()
                .setChannelId(channelId)
                .setType(TvContractCompat.PreviewPrograms.TYPE_TV_SERIES)
                .setTitle(show.name)
                .setDescription(show.overview)
                .setPosterArtUri(Uri.parse("https://image.tmdb.org/t/p/w500${show.backdrop_path}"))
                .setIntentUri(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)))
                .setInternalProviderId("t_${show.id}")
                .build()
            
            applicationContext.contentResolver.insert(TvContractCompat.PreviewPrograms.CONTENT_URI, program.toContentValues())
        }
    }

    private fun getOrCreateChannel(displayName: String, internalId: String): Long {
        val cursor = applicationContext.contentResolver.query(
            TvContractCompat.Channels.CONTENT_URI,
            arrayOf(TvContractCompat.Channels._ID, TvContractCompat.Channels.COLUMN_INTERNAL_PROVIDER_ID),
            null, null, null
        )
 
        var channelId = cursor?.use {
            val idIndex = it.getColumnIndex(TvContractCompat.Channels._ID)
            val intIdIndex = it.getColumnIndex(TvContractCompat.Channels.COLUMN_INTERNAL_PROVIDER_ID)
            var foundId = -1L
            if (idIndex != -1 && intIdIndex != -1) {
                while (it.moveToNext()) {
                    if (it.getString(intIdIndex) == internalId) {
                        foundId = it.getLong(idIndex)
                        break
                    }
                }
            }
            foundId
        } ?: -1L

        if (channelId == -1L) {
            val channel = Channel.Builder()
                .setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setDisplayName(displayName)
                .setInternalProviderId(internalId)
                .setAppLinkIntentUri(Uri.parse("nfgplus://home"))
                .setSearchable(true)
                .build()

            val uri = applicationContext.contentResolver.insert(TvContractCompat.Channels.CONTENT_URI, channel.toContentValues())
            if (uri != null) {
                channelId = android.content.ContentUris.parseId(uri)
                try {
                    val bitmap = android.graphics.BitmapFactory.decodeResource(applicationContext.resources, R.drawable.ic_nfgplus192)
                    if (bitmap != null) ChannelLogoUtils.storeChannelLogo(applicationContext, channelId, bitmap)
                } catch (e: Exception) { Log.e(TAG, "Failed logo", e) }
                TvContractCompat.requestChannelBrowsable(applicationContext, channelId)
            }
        } else {
            TvContractCompat.requestChannelBrowsable(applicationContext, channelId)
        }
        return channelId
    }
}
