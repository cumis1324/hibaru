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
    var idForDB: Int = 0,
    
    var adult: Boolean = false,
    
    @get:JvmName("getBackdropPath") @set:JvmName("setBackdropPath")
    @ColumnInfo(name = "backdrop_path")
    var backdrop_path: String? = null,
    
    @get:JvmName("getAddToList") @set:JvmName("setAddToList")
    @ColumnInfo(name = "add_to_list", defaultValue = "0")
    var add_to_list: Int = 0,
    
    @get:JvmName("getLogoPath") @set:JvmName("setLogoPath")
    @ColumnInfo(name = "logo_path", defaultValue = "")
    var logo_path: String = "",
    
    var homepage: String? = null,
    
    @get:JvmName("getFirstAirDate") @set:JvmName("setFirstAirDate")
    @ColumnInfo(name = "first_air_date")
    var first_air_date: String? = null,
    
    var id: Int = 0,
    
    @get:JvmName("getInProduction") @set:JvmName("setInProduction")
    @ColumnInfo(name = "in_production")
    var in_production: Boolean = false,
    
    @get:JvmName("getLastAirDate") @set:JvmName("setLastAirDate")
    @ColumnInfo(name = "last_air_date")
    var last_air_date: String? = null,
    
    var name: String? = null,
    
    @get:JvmName("getNumberOfEpisodes") @set:JvmName("setNumberOfEpisodes")
    @ColumnInfo(name = "number_of_episodes")
    var number_of_episodes: Int = 0,
    
    @get:JvmName("getNumberOfSeasons") @set:JvmName("setNumberOfSeasons")
    @ColumnInfo(name = "number_of_seasons")
    var number_of_seasons: Int = 0,
    
    @get:JvmName("getOriginalName") @set:JvmName("setOriginalName")
    @ColumnInfo(name = "original_name")
    var original_name: String? = null,
    
    var overview: String? = null,
    var popularity: Double = 0.0,
    
    @get:JvmName("getOriginalLanguage") @set:JvmName("setOriginalLanguage")
    @ColumnInfo(name = "original_language")
    var original_language: String? = null,
    
    @get:JvmName("getPosterPath") @set:JvmName("setPosterPath")
    @ColumnInfo(name = "poster_path")
    var poster_path: String? = null,
    
    var status: String? = null,
    var seasons: ArrayList<Season>? = null,
    var tagline: String? = null,
    var type: String? = null,
    
    @get:JvmName("getVoteAverage") @set:JvmName("setVoteAverage")
    @ColumnInfo(name = "vote_average")
    var vote_average: Double = 0.0,
    
    @get:JvmName("getVoteCount") @set:JvmName("setVoteCount")
    @ColumnInfo(name = "vote_count")
    var vote_count: Int = 0,
    
    var genres: ArrayList<Genre>? = null
) : MyMedia, Parcelable
