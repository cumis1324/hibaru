package com.theflexproject.thunder.model.TVShowInfo

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.theflexproject.thunder.model.Genre
import com.theflexproject.thunder.model.MyMedia
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class TVShow(
    @PrimaryKey(autoGenerate = true)
    val idForDB: Int = 0,
    
    val adult: Boolean = false,
    
    @ColumnInfo(name = "backdrop_path")
    val backdropPath: String? = null,
    
    @ColumnInfo(name = "addToList", defaultValue = "0")
    val addToList: Int = 0,
    
    @ColumnInfo(name = "logo_path", defaultValue = "")
    val logoPath: String = "",
    
    val homepage: String? = null,
    
    @ColumnInfo(name = "first_air_date")
    val firstAirDate: String? = null,
    
    val id: Int = 0,
    
    @ColumnInfo(name = "in_production")
    val inProduction: Boolean = false,
    
    @ColumnInfo(name = "last_air_date")
    val lastAirDate: String? = null,
    
    val name: String? = null,
    
    @ColumnInfo(name = "number_of_episodes")
    val numberOfEpisodes: Int = 0,
    
    @ColumnInfo(name = "number_of_seasons")
    val numberOfSeasons: Int = 0,
    
    @ColumnInfo(name = "original_name")
    val originalName: String? = null,
    
    val overview: String? = null,
    val popularity: Double = 0.0,
    
    @ColumnInfo(name = "original_language")
    val originalLanguage: String? = null,
    
    @ColumnInfo(name = "poster_path")
    val posterPath: String? = null,
    
    val status: String? = null,
    val seasons: List<Season>? = null,
    val tagline: String? = null,
    val type: String? = null,
    
    @ColumnInfo(name = "vote_average")
    val voteAverage: Double = 0.0,
    
    @ColumnInfo(name = "vote_count")
    val voteCount: Int = 0,
    
    val genres: List<Genre>? = null
) : MyMedia, Parcelable
