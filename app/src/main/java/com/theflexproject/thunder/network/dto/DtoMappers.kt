package com.theflexproject.thunder.network.dto

import com.theflexproject.thunder.model.TVShowInfo.Episode
import com.theflexproject.thunder.model.Movie
import com.theflexproject.thunder.model.TVShowInfo.TVShow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DTO Mappers - Convert API DTOs to Room Entity models
 */

// Movie DTO → Movie Entity
fun MovieDto.toMovie(): Movie = Movie(
    fileidForDB = id, // Server DB ID -> Local DB PK
    id = tmdb_id,    // TMDB ID -> id field used for navigation
    title = title ?: "",
    original_title = original_title,
    overview = overview,
    poster_path = poster_path,
    backdrop_path = backdrop_path,
    logo_path = logo_path ?: "",
    release_date = release_date,
    runtime = runtime ?: 0,
    vote_average = vote_average?.toDouble() ?: 0.0,
    vote_count = vote_count ?: 0,
    popularity = popularity?.toDouble() ?: 0.0,
    adult = adult == 1,
    video = video == 1,
    budget = budget?.toLong() ?: 0L,
    revenue = revenue?.toLong() ?: 0L,
    status = status,
    tagline = tagline,
    homepage = homepage,
    imdb_id = imdb_id,
    original_language = original_language,
    disabled = disabled ?: 0,
    add_to_list = add_to_list ?: 0,
    played = played?.toString(),
    index_id = index_id ?: 0,
    gd_id = gd_id ?: "",
    file_name = file_name,
    mime_type = mime_type,
    modified_time = modified_time.toDate(),
    size = size,
    url_string = url_string
)

private fun String?.toDate(): java.util.Date? {
    if (this.isNullOrEmpty()) return null
    
    // Try toLongOrNull for timestamp strings (detect seconds vs milliseconds)
    this.toLongOrNull()?.let { 
        val ts = if (it < 100000000000L) it * 1000 else it
        return java.util.Date(ts) 
    }
    
    // Try parsing date formats
    val formats = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd"
    )
    for (format in formats) {
        try {
            return java.text.SimpleDateFormat(format, java.util.Locale.US).parse(this)
        } catch (e: Exception) {
            // Check next format
        }
    }
    return null
}


// TVShow DTO → TVShow Entity
fun TVShowDto.toTVShow(): TVShow = TVShow(
    idForDB = id,
    id = tmdb_id, // TVShow has 'id' field, not 'tmdbId'
    name = name ?: "",
    original_name = original_name,
    overview = overview,
    poster_path = poster_path,
    backdrop_path = backdrop_path,
    logo_path = logo_path ?: "",
    first_air_date = first_air_date,
    last_air_date = last_air_date,
    number_of_seasons = number_of_seasons ?: 0,
    number_of_episodes = number_of_episodes ?: 0,
    vote_average = vote_average?.toDouble() ?: 0.0,
    vote_count = vote_count ?: 0,
    popularity = popularity?.toDouble() ?: 0.0,
    adult = adult == 1,
    in_production = in_production == 1,
    status = status,
    type = type,
    tagline = tagline,
    homepage = homepage,
    original_language = original_language,
    add_to_list = add_to_list ?: 0
)

// Episode DTO → Episode Entity
fun EpisodeDto.toEpisode(): Episode = Episode(
    idForDB = id,
    // Episode Entity has 'id' field? Step 1793: val id: Int = 0.
    // DTO maps tmdb_id to local id? Or DTO id to local id?
    // DTO has id (int) and tmdb_id (int?).
    // Entity has id (int) and idForDB (int, PK).
    // Usually id = tmdb_id.
    id = tmdb_id ?: 0,
    show_id = show_id.toLong(), // Entity wants Long
    season_number = season_number,
    episode_number = episode_number,
    name = name,
    overview = overview,
    still_path = still_path,
    air_date = air_date,
    runtime = runtime ?: 0,
    vote_average = vote_average?.toDouble() ?: 0.0,
    vote_count = vote_count ?: 0,
    production_code = production_code,
    played = played?.toString(),
    disabled = disabled ?: 0,
    index_id = index_id ?: 0,
    gd_id = gd_id ?: "",
    file_name = file_name,
    mime_type = mime_type,
    modified_time = modified_time.toDate(),
    size = size,
    url_string = url_string
)

// Season DTO → SeasonDetails Entity
fun com.theflexproject.thunder.network.dto.SeasonDto.toSeasonDetails(): com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails = com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails(
    idfordb = 0, // Auto-generate
    id = id, // TMDB Season ID
    show_id = show_id.toLong(), // Will be overridden in SyncManager
    season_number = season_number,
    name = name,
    overview = overview,
    poster_path = poster_path,
    air_date = air_date,
    _id = id.toString() // String ID for legacy field?
)

// Batch mappers
fun List<MovieDto>.toMovies(): List<Movie> = map { it.toMovie() }
fun List<TVShowDto>.toTVShows(): List<TVShow> = map { it.toTVShow() }
fun List<EpisodeDto>.toEpisodes(): List<Episode> = map { it.toEpisode() }
fun List<com.theflexproject.thunder.network.dto.SeasonDto>.toSeasonDetails(): List<com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails> = map { it.toSeasonDetails() }
