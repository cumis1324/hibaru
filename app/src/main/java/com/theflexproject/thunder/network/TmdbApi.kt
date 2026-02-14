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
}
