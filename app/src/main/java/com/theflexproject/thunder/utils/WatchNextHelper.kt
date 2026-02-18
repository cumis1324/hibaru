package com.theflexproject.thunder.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import androidx.tvprovider.media.tv.PreviewProgram
import com.theflexproject.thunder.model.Movie
import com.theflexproject.thunder.model.TVShowInfo.Episode

object WatchNextHelper {

    /**
     * Adds or updates a program in the "Watch Next" row.
     */
    fun updateWatchNextProgram(context: Context, movie: Movie?, episode: Episode?, positionMs: Long, durationMs: Long) {
        val programId = movie?.id?.toLong() ?: episode?.id?.toLong() ?: return
        android.util.Log.d("WatchNext", "Updating program: id=$programId, pos=$positionMs, dur=$durationMs")
        val title = movie?.title ?: episode?.name ?: "Unknown"
        val posterPath = movie?.backdrop_path ?: episode?.still_path ?: ""
        val posterUrl = "https://image.tmdb.org/t/p/w500$posterPath"
        
        // Build intent for deep linking back to the player/detail
        // Note: You should verify the intent filter in AndroidManifest.xml matches this
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("nfgplus://video/$programId?isMovie=${movie != null}")
        }

        val builder = WatchNextProgram.Builder()
            .setType(TvContractCompat.WatchNextPrograms.TYPE_MOVIE)
            .setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
            .setLastEngagementTimeUtcMillis(System.currentTimeMillis())
            .setLastPlaybackPositionMillis(positionMs.toInt())
            .setDurationMillis(durationMs.toInt())
            .setTitle(title)
            .setPosterArtUri(Uri.parse(posterUrl))
            .setIntentUri(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)))
            .setInternalProviderId(programId.toString())

        val program = builder.build()

        // Check if program already exists to either update or insert
        val existingProgramId = getWatchNextProgramId(context, programId.toString())
        if (existingProgramId != -1L) {
            android.util.Log.d("WatchNext", "Found existing program rowId: $existingProgramId, updating...")
            TvContractCompat.buildWatchNextProgramUri(existingProgramId).let { uri ->
                val rows = context.contentResolver.update(uri, program.toContentValues(), null, null)
                android.util.Log.d("WatchNext", "Update result: $rows rows affected")
            }
        } else {
            android.util.Log.d("WatchNext", "Program not found, inserting new...")
            val uri = context.contentResolver.insert(TvContractCompat.WatchNextPrograms.CONTENT_URI, program.toContentValues())
            android.util.Log.d("WatchNext", "Insert result: $uri")
        }
    }

    /**
     * Finds the row ID of an existing Watch Next program by its internal provider ID.
     */
    private fun getWatchNextProgramId(context: Context, internalId: String): Long {
        val cursor = context.contentResolver.query(
            TvContractCompat.WatchNextPrograms.CONTENT_URI,
            arrayOf(
                TvContractCompat.WatchNextPrograms._ID,
                TvContractCompat.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID
            ),
            null,
            null,
            null
        )
        return cursor?.use {
            val idIndex = it.getColumnIndex(TvContractCompat.WatchNextPrograms._ID)
            val internalIdIndex = it.getColumnIndex(TvContractCompat.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID)
            
            if (idIndex != -1 && internalIdIndex != -1) {
                while (it.moveToNext()) {
                    if (it.getString(internalIdIndex) == internalId) {
                        return@use it.getLong(idIndex)
                    }
                }
            }
            -1L
        } ?: -1L
    }

    /**
     * Removes a program from the "Watch Next" row (e.g., when finished).
     */
    fun removeFromWatchNext(context: Context, internalId: String) {
        val existingProgramId = getWatchNextProgramId(context, internalId)
        if (existingProgramId != -1L) {
            context.contentResolver.delete(
                TvContractCompat.buildWatchNextProgramUri(existingProgramId),
                null,
                null
            )
        }
    }
}
