package com.theflexproject.thunder.network

import com.squareup.moshi.Json

// API Response Models matching Cloudflare API

data class MoviesResponse(
    val movies: List<MovieDto>,
    val count: Int
)

data class MovieDto(
    val id: Int,
    @Json(name = "tmdb_id") val tmdbId: Int?,
    val title: String,
    @Json(name = "original_title") val originalTitle: String?,
    val overview: String?,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "backdrop_path") val backdropPath: String?,
    @Json(name = "logo_path") val logoPath: String?,
    @Json(name = "release_date") val releaseDate: String?,
    val runtime: Int?,
    @Json(name = "vote_average") val voteAverage: Double?,
    @Json(name = "vote_count") val voteCount: Int?,
    val popularity: Double?,
    val adult: Boolean?,
    val status: String?,
    val tagline: String?,
    val homepage: String?,
    @Json(name = "imdb_id") val imdbId: String?,
    @Json(name = "original_language") val originalLanguage: String?,
    val genres: List<GenreDto>? = null
)

data class TVShowsResponse(
    val tvshows: List<TVShowDto>
)

data class TVShowDto(
    val id: Int,
    @Json(name = "tmdb_id") val tmdbId: Int?,
    val name: String,
    @Json(name = "original_name") val originalName: String?,
    val overview: String?,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "backdrop_path") val backdropPath: String?,
    @Json(name = "logo_path") val logoPath: String?,
    @Json(name = "first_air_date") val firstAirDate: String?,
    @Json(name = "last_air_date") val lastAirDate: String?,
    @Json(name = "number_of_seasons") val numberOfSeasons: Int?,
    @Json(name = "number_of_episodes") val numberOfEpisodes: Int?,
    @Json(name = "vote_average") val voteAverage: Double?,
    @Json(name = "vote_count") val voteCount: Int?,
    val popularity: Double?,
    val adult: Boolean?,
    @Json(name = "in_production") val inProduction: Boolean?,
    val status: String?,
    val type: String?
)

data class EpisodesResponse(
    val episodes: List<EpisodeDto>
)

data class EpisodeDto(
    val id: Int,
    @Json(name = "tmdb_id") val tmdbId: Int?,
    @Json(name = "show_id") val showId: Int,
    @Json(name = "season_number") val seasonNumber: Int,
    @Json(name = "episode_number") val episodeNumber: Int,
    val name: String?,
    val overview: String?,
    @Json(name = "still_path") val stillPath: String?,
    @Json(name = "air_date") val airDate: String?,
    val runtime: Int?,
    @Json(name = "vote_average") val voteAverage: Double?,
    @Json(name = "vote_count") val voteCount: Int?,
    val played: Boolean?
)

data class GenresResponse(
    val genres: List<GenreDto>
)

data class GenreDto(
    val id: Int,
    val name: String
)

data class SeasonsResponse(
    val seasons: List<SeasonDto>
)

data class SeasonDto(
    val id: Int,
    @Json(name = "show_id") val showId: Int,
    @Json(name = "season_number") val seasonNumber: Int,
    val name: String?,
    val overview: String?,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "air_date") val airDate: String?,
    @Json(name = "episode_count") val episodeCount: Int?
)

data class ApiResponse<T>(
    val data: T?,
    val error: String?
)
