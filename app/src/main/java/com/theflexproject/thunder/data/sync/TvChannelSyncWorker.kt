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
            Log.d(TAG, "Starting TV Channel Sync...")
            
            // 1. Get or Create Channel
            val channelId = getOrCreateChannel()
            if (channelId == -1L) {
                Log.e(TAG, "Failed to create/find TV channel")
                return@withContext Result.failure()
            }

            // 2. Clear old programs from this channel
            applicationContext.contentResolver.delete(
                TvContractCompat.buildPreviewProgramsUriForChannel(channelId),
                null, 
                null
            )

            // 3. Fetch top movies and shows (example: limit to top 10 each)
            val topMovies = movieRepository.getTrendingMovies(limit = 10)
            val topShows = tvShowRepository.getTrendingTVShows(limit = 10)

            // 4. Add Movies to Channel
            android.util.Log.d(TAG, "Adding ${topMovies.size} movies to channel...")
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
                    .setInternalProviderId(movie.id.toString())
                    .build()
                
                val uri = applicationContext.contentResolver.insert(TvContractCompat.PreviewPrograms.CONTENT_URI, program.toContentValues())
                android.util.Log.d(TAG, "Inserted Movie: ${movie.title}, uri: $uri")
            }

            // 5. Add TV Shows to Channel
            android.util.Log.d(TAG, "Adding ${topShows.size} TV shows to channel...")
            topShows.forEach { show ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("nfgplus://video/${show.id}?isMovie=false")
                }
                
                val program = PreviewProgram.Builder()
                    .setChannelId(channelId)
                    .setType(TvContractCompat.PreviewPrograms.TYPE_TV_SERIES)
                    .setTitle(show.name)
                    .setDescription(show.overview)
                    .setPosterArtUri(Uri.parse("https://image.tmdb.org/t/p/w500${show.backdrop_path}"))
                    .setIntentUri(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)))
                    .setInternalProviderId(show.id.toString())
                    .build()
                
                val uri = applicationContext.contentResolver.insert(TvContractCompat.PreviewPrograms.CONTENT_URI, program.toContentValues())
                android.util.Log.d(TAG, "Inserted TV Show: ${show.name}, uri: $uri")
            }

            Log.d(TAG, "TV Channel Sync Finished successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing TV channel", e)
            Result.retry()
        }
    }

    private fun getOrCreateChannel(): Long {
        // Check if channel exists
        val cursor = applicationContext.contentResolver.query(
            TvContractCompat.Channels.CONTENT_URI,
            arrayOf(
                TvContractCompat.Channels._ID,
                TvContractCompat.Channels.COLUMN_INTERNAL_PROVIDER_ID
            ),
            null,
            null,
            null
        )
 
        var channelId = cursor?.use {
            val idIndex = it.getColumnIndex(TvContractCompat.Channels._ID)
            val internalIdIndex = it.getColumnIndex(TvContractCompat.Channels.COLUMN_INTERNAL_PROVIDER_ID)
            
            var foundId = -1L
            if (idIndex != -1 && internalIdIndex != -1) {
                while (it.moveToNext()) {
                    if (it.getString(internalIdIndex) == CHANNEL_INTERNAL_ID) {
                        foundId = it.getLong(idIndex)
                        break
                    }
                }
            }
            foundId
        } ?: -1L

        if (channelId == -1L) {
            // Create channel
            val channel = Channel.Builder()
                .setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setDisplayName("nfgplus Trending")
                .setDescription("Popular movies and TV shows on nfgplus")
                .setInternalProviderId(CHANNEL_INTERNAL_ID)
                .setAppLinkIntentUri(Uri.parse("nfgplus://home"))
                .setSearchable(true)
                .build()

            val uri = applicationContext.contentResolver.insert(TvContractCompat.Channels.CONTENT_URI, channel.toContentValues())
            if (uri != null) {
                channelId = android.content.ContentUris.parseId(uri)
                // Set channel logo
                android.util.Log.d(TAG, "Setting channel logo for $channelId")
                try {
                    val bitmap = android.graphics.BitmapFactory.decodeResource(applicationContext.resources, com.theflexproject.thunder.R.drawable.ic_nfgplus192)
                    if (bitmap != null) {
                        ChannelLogoUtils.storeChannelLogo(applicationContext, channelId, bitmap)
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to set channel logo", e)
                }
                
                // Request channel to be browsable (visible by default if user permits)
                android.util.Log.d(TAG, "Requesting channel browsable: $channelId")
                TvContractCompat.requestChannelBrowsable(applicationContext, channelId)
            }
        } else {
            android.util.Log.d(TAG, "Using existing channelId: $channelId")
        }
        
        return channelId
    }
}
