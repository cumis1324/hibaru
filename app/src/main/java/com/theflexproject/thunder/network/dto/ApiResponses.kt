package com.theflexproject.thunder.network.dto

/**
 * API Response DTOs for NFGPlus Cloudflare D1 API
 * These models match the backend API response structure
 */

// Movies Response
data class MoviesResponse(
    val movies: List<MovieDto>,
    val count: Int
)

data class MovieDto(
    val id: Int,
    val tmdb_id: Int,
    val title: String,
    val original_title: String?,
    val overview: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val logo_path: String?,
    val release_date: String?,
    val runtime: Int?,
    val vote_average: Float?,
    val vote_count: Int?,
    val popularity: Float?,
    val adult: Int,
    val video: Int,
    val budget: Int?,
    val revenue: Int?,
    val status: String?,
    val tagline: String?,
    val homepage: String?,
    val imdb_id: String?,
    val original_language: String?,
    val disabled: Int?,
    val add_to_list: Int?,
    val played: Int?,
    val index_id: Int?,
    val gd_id: String?,
    val file_name: String?,
    val mime_type: String?,
    val modified_time: String?,
    val size: String?,
    val url_string: String?,
    val created_at: String?,
    val updated_at: String?
)

// TV Shows Response
data class TVShowsResponse(
    val tvshows: List<TVShowDto>,
    val count: Int? = null
)

data class TVShowDto(
    val id: Int,
    val tmdb_id: Int,
    val name: String,
    val original_name: String?,
    val overview: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val logo_path: String?,
    val first_air_date: String?,
    val last_air_date: String?,
    val number_of_seasons: Int?,
    val number_of_episodes: Int?,
    val vote_average: Float?,
    val vote_count: Int?,
    val popularity: Float?,
    val adult: Int,
    val in_production: Int,
    val status: String?,
    val type: String?,
    val tagline: String?,
    val homepage: String?,
    val original_language: String?,
    val add_to_list: Int?,
    val created_at: String?,
    val updated_at: String?
)

// Episodes Response
data class EpisodesResponse(
    val episodes: List<EpisodeDto>
)

data class EpisodeDto(
    val id: Int,
    val tmdb_id: Int?,
    val show_id: Int,
    val season_number: Int,
    val episode_number: Int,
    val name: String?,
    val overview: String?,
    val still_path: String?,
    val air_date: String?,
    val runtime: Int?,
    val vote_average: Float?,
    val vote_count: Int?,
    val production_code: String?,
    val played: Int?,
    val disabled: Int?,
    val index_id: Int?,
    val gd_id: String?,
    val file_name: String?,
    val mime_type: String?,
    val modified_time: String?,
    val size: String?,
    val url_string: String?,
    val created_at: String?
)

// Genres Response
data class GenresResponse(
    val genres: List<GenreDto>
)

data class GenreDto(
    val id: Int,
    val name: String
)

// Health Check Response
data class HealthResponse(
    val status: String,
    val timestamp: String
)

// Seasons Response
data class SeasonsResponse(
    val seasons: List<SeasonDto>
)

data class SeasonDto(
    val id: Int,
    val show_id: Int,
    val season_number: Int,
    val name: String?,
    val overview: String?,
    val poster_path: String?,
    val air_date: String?,
    val episode_count: Int?
)

// Generic API Response
data class ApiResponse<T>(
    val data: T?,
    val error: String?
)
