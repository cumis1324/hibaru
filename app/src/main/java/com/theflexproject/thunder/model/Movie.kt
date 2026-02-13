package com.theflexproject.thunder.model

import android.os.Parcelable
import androidx.room.*
import kotlinx.parcelize.Parcelize
import java.util.Date

@Entity(indices = [Index(value = ["gd_id"], unique = true)])
@Parcelize
data class Movie(
    @PrimaryKey(autoGenerate = true)
    var fileidForDB: Int = 0,
    
    @get:JvmName("getFileName") @set:JvmName("setFileName")
    var file_name: String? = null,
    @get:JvmName("getMimeType") @set:JvmName("setMimeType")
    var mime_type: String? = null,
    @get:JvmName("getModifiedTime") @set:JvmName("setModifiedTime")
    var modified_time: Date? = null,
    var size: String? = null,
    @get:JvmName("getUrlString") @set:JvmName("setUrlString")
    var url_string: String? = null,
    
    @get:JvmName("getGdId") @set:JvmName("setGdId")
    @ColumnInfo(name = "gd_id", defaultValue = "")
    var gd_id: String = "",
    
    @get:JvmName("getLogoPath") @set:JvmName("setLogoPath")
    @ColumnInfo(name = "logo_path", defaultValue = "")
    var logo_path: String = "",
    
    @get:JvmName("getIndexId") @set:JvmName("setIndexId")
    @ColumnInfo(name = "index_id", defaultValue = "0")
    var index_id: Int = 0,
    
    @ColumnInfo(name = "disabled", defaultValue = "0")
    var disabled: Int = 0,
    
    @get:JvmName("getAddToList") @set:JvmName("setAddToList")
    @ColumnInfo(name = "add_to_list", defaultValue = "0")
    var add_to_list: Int = 0,
    
    @get:JvmName("getPlayed") @set:JvmName("setPlayed")
    @ColumnInfo(name = "played")
    var played: String? = null,
    
    // TMDB Fields
    var adult: Boolean = false,
    @get:JvmName("getBackdropPath") @set:JvmName("setBackdropPath")
    @ColumnInfo(name = "backdrop_path")
    var backdrop_path: String? = null,
    var budget: Long = 0,
    var genres: ArrayList<Genre>? = null,
    var homepage: String? = null,
    var id: Int = 0,
    @get:JvmName("getImdbId") @set:JvmName("setImdbId")
    @ColumnInfo(name = "imdb_id")
    var imdb_id: String? = null,
    @get:JvmName("getOriginalLanguage") @set:JvmName("setOriginalLanguage")
    @ColumnInfo(name = "original_language")
    var original_language: String? = null,
    @get:JvmName("getOriginalTitle") @set:JvmName("setOriginalTitle")
    @ColumnInfo(name = "original_title")
    var original_title: String? = null,
    var overview: String? = null,
    var popularity: Double = 0.0,
    @get:JvmName("getPosterPath") @set:JvmName("setPosterPath")
    @ColumnInfo(name = "poster_path")
    var poster_path: String? = null,
    @get:JvmName("getReleaseDate") @set:JvmName("setReleaseDate")
    @ColumnInfo(name = "release_date")
    var release_date: String? = null,
    var revenue: Long = 0,
    var runtime: Int = 0,
    var status: String? = null,
    var tagline: String? = null,
    var title: String? = null,
    var video: Boolean = false,
    @get:JvmName("getVoteAverage") @set:JvmName("setVoteAverage")
    @ColumnInfo(name = "vote_average")
    var vote_average: Double = 0.0,
    @get:JvmName("getVoteCount") @set:JvmName("setVoteCount")
    @ColumnInfo(name = "vote_count")
    var vote_count: Int = 0
) : MyMedia, Parcelable
