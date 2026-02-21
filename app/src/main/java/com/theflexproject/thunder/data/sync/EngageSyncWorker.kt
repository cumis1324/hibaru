package com.theflexproject.thunder.data.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.engage.service.AppEngagePublishClient
import com.google.android.engage.video.datamodel.MovieEntity
import com.google.android.engage.video.datamodel.TvShowEntity
import com.google.android.engage.common.datamodel.RecommendationCluster
import com.google.android.engage.common.datamodel.Image
import com.google.android.engage.service.PublishRecommendationClustersRequest
import com.theflexproject.thunder.repository.MovieRepository
import com.theflexproject.thunder.repository.TVShowRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class EngageSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TVShowRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "EngageSyncWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting Engage SDK Sync (v1.5.1)...")
            val publishClient = AppEngagePublishClient(applicationContext)

            // Publish Recommendations (Trending Content)
            publishTrendingRecommendations(publishClient)

            Log.d(TAG, "Engage SDK Sync Finished successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing Engage SDK", e)
            Result.retry()
        }
    }

    private suspend fun publishTrendingRecommendations(publishClient: AppEngagePublishClient) {
        val topMovies = movieRepository.getTrendingMovies(limit = 10)
        val topShows = tvShowRepository.getTrendingTVShows(limit = 10)

        val clusterBuilder = RecommendationCluster.Builder()
            .setTitle("nfgplus Trending")

        var entityCount = 0

        // Add Movies
        topMovies.forEach { movie ->
            val builder = MovieEntity.Builder()
                .setEntityId((movie.id ?: 0).toString())
                .setName(movie.title ?: "Unknown Movie")
                .setPlayBackUri(Uri.parse("nfgplus://video/${movie.id}?isMovie=true"))


            movie.backdrop_path?.let { path ->
                builder.addPosterImage(
                    Image.Builder()
                        .setImageUri(Uri.parse("https://image.tmdb.org/t/p/w500$path"))
                        .build()
                )
            }

            clusterBuilder.addEntity(builder.build())
            entityCount++
        }

        // Add TV Shows
        topShows.forEach { show ->
            val builder = TvShowEntity.Builder()
                .setEntityId((show.id ?: 0).toString())
                .setName(show.name ?: "Unknown Show")
                .setPlayBackUri(Uri.parse("nfgplus://video/${show.id}?isMovie=false"))


            show.backdrop_path?.let { path ->
                builder.addPosterImage(
                    Image.Builder()
                        .setImageUri(Uri.parse("https://image.tmdb.org/t/p/w500$path"))
                        .build()
                )
            }

            clusterBuilder.addEntity(builder.build())
            entityCount++
        }

        if (entityCount > 0) {
            val cluster = clusterBuilder.build()
            Log.d(TAG, "Publishing $entityCount entities to Trending Recommendation Cluster")
            val request = PublishRecommendationClustersRequest.Builder()
                .addRecommendationCluster(cluster)
                .build()
            publishClient.publishRecommendationClusters(request)
        }
    }
}
