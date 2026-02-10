package com.theflexproject.thunder.network

import retrofit2.Response
import retrofit2.http.*

interface NFGPlusApi {
    
    // Movies
    @GET("api/movies")
    suspend fun getMovies(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("search") search: String? = null
    ): Response<MoviesResponse>
    
    @GET("api/movies/{id}")
    suspend fun getMovieById(@Path("id") id: Int): Response<MovieDto>
    
    @POST("api/movies")
    suspend fun createMovie(@Body movie: MovieDto): Response<ApiResponse<Int>>
    
    @PUT("api/movies/{id}")
    suspend fun updateMovie(
        @Path("id") id: Int,
        @Body movie: MovieDto
    ): Response<ApiResponse<Unit>>
    
    @DELETE("api/movies/{id}")
    suspend fun deleteMovie(@Path("id") id: Int): Response<ApiResponse<Unit>>
    
    // TV Shows
    @GET("api/tvshows")
    suspend fun getTVShows(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<TVShowsResponse>
    
    @GET("api/tvshows/{id}")
    suspend fun getTVShowById(@Path("id") id: Int): Response<TVShowDto>
    
    @GET("api/tvshows/{id}/seasons")
    suspend fun getSeasons(@Path("id") showId: Int): Response<SeasonsResponse>
    
    @GET("api/tvshows/{id}/episodes")
    suspend fun getEpisodes(
        @Path("id") showId: Int,
        @Query("season") seasonNumber: Int? = null
    ): Response<EpisodesResponse>
    
    // Episodes
    @GET("api/episodes/{id}")
    suspend fun getEpisodeById(@Path("id") id: Int): Response<EpisodeDto>
    
    @PUT("api/episodes/{id}")
    suspend fun updateEpisode(
        @Path("id") id: Int,
        @Body episode: EpisodeDto
    ): Response<ApiResponse<Unit>>
    
    // Genres
    @GET("api/genres")
    suspend fun getGenres(): Response<GenresResponse>
    
    // Health check
    @GET("api/health")
    suspend fun healthCheck(): Response<Map<String, String>>
}
