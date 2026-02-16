package com.theflexproject.thunder.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class TmdbResponse(
    @Json(name = "page") val page: Int,
    @Json(name = "results") val results: List<TmdbMediaId>,
    @Json(name = "total_pages") val totalPages: Int,
    @Json(name = "total_results") val totalResults: Int
)

@JsonClass(generateAdapter = true)
data class TmdbMediaId(
    @Json(name = "id") val id: Int
)

interface TmdbApi {
    
    @GET("trending/movie/week")
    suspend fun getTrendingMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<TmdbResponse>

    @GET("trending/tv/week")
    suspend fun getTrendingTVShows(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<TmdbResponse>
    
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<TmdbResponse>

    @GET("tv/popular")
    suspend fun getPopularTVShows(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<TmdbResponse>

    @GET("movie/{movie_id}/similar")
    suspend fun getSimilarMovies(
        @retrofit2.http.Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<TmdbResponse>

    @GET("movie/{movie_id}/recommendations")
    suspend fun getRecommendationMovies(
        @retrofit2.http.Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<TmdbResponse>

    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(
        @retrofit2.http.Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): Response<com.theflexproject.thunder.model.Credits>

    @GET("tv/{tv_id}/similar")
    suspend fun getSimilarTVShows(
        @retrofit2.http.Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<TmdbResponse>

    @GET("tv/{tv_id}/recommendations")
    suspend fun getRecommendationTVShows(
        @retrofit2.http.Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<TmdbResponse>

    @GET("tv/{tv_id}/credits")
    suspend fun getTVShowCredits(
        @retrofit2.http.Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String
    ): Response<com.theflexproject.thunder.model.Credits>
}
