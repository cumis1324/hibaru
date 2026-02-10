package com.theflexproject.thunder.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.Date

@Entity
@Parcelize
data class Movie(
    @PrimaryKey(autoGenerate = true)
    val fileidForDB: Int = 0,
    
    val fileName: String? = null,
    val mimeType: String? = null,
    val modifiedTime: Date? = null,
    val size: String? = null,
    val urlString: String? = null,
    
    @ColumnInfo(name = "gd_id", defaultValue = "")
    val gdId: String = "",
    
    @ColumnInfo(name = "logo_path", defaultValue = "")
    val logoPath: String = "",
    
    @ColumnInfo(name = "index_id", defaultValue = "0")
    val indexId: Int = 0,
    
    @ColumnInfo(name = "disabled", defaultValue = "0")
    val disabled: Int = 0,
    
    @ColumnInfo(name = "addToList", defaultValue = "0")
    val addToList: Int = 0,
    
    val played: Int = 0,
    
    // TMDB Fields
    val adult: Boolean = false,
    @ColumnInfo(name = "backdrop_path")
    val backdropPath: String? = null,
    val budget: Long = 0,
    val genres: List<Genre>? = null,
    val homepage: String? = null,
    val id: Int = 0,
    @ColumnInfo(name = "imdb_id")
    val imdbId: String? = null,
    @ColumnInfo(name = "original_language")
    val originalLanguage: String? = null,
    @ColumnInfo(name = "original_title")
    val originalTitle: String? = null,
    val overview: String? = null,
    val popularity: Double = 0.0,
    @ColumnInfo(name = "poster_path")
    val posterPath: String? = null,
    @ColumnInfo(name = "release_date")
    val releaseDate: String? = null,
    val revenue: Long = 0,
    val runtime: Int = 0,
    val status: String? = null,
    val tagline: String? = null,
    val title: String? = null,
    val video: Boolean = false,
    @ColumnInfo(name = "vote_average")
    val voteAverage: Double = 0.0,
    @ColumnInfo(name = "vote_count")
    val voteCount: Int = 0
) : MyMedia, Parcelable
