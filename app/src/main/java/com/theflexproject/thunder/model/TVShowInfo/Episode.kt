package com.theflexproject.thunder.model.TVShowInfo

import android.os.Parcelable
import androidx.room.*
import com.fasterxml.jackson.annotation.JsonFormat
import com.theflexproject.thunder.model.MyMedia
import kotlinx.parcelize.Parcelize
import java.util.Date

@Entity(indices = [Index(value = ["gd_id"], unique = true)])
@Parcelize
data class Episode(
    @PrimaryKey(autoGenerate = true)
    var idForDB: Int = 0,
    
    @get:JvmName("getFileName") @set:JvmName("setFileName")
    var file_name: String? = null,
    @get:JvmName("getMimeType") @set:JvmName("setMimeType")
    var mime_type: String? = null,
    
    @get:JvmName("getModifiedTime") @set:JvmName("setModifiedTime")
    @JsonFormat(pattern = "E MMM dd HH:mm:ss z yyyy")
    var modified_time: Date? = null,
    
    var size: String? = null,
    
    @get:JvmName("getUrlString") @set:JvmName("setUrlString")
    @ColumnInfo(name = "url_string")
    var url_string: String? = null,
    
    @get:JvmName("getIndexId") @set:JvmName("setIndexId")
    @ColumnInfo(name = "index_id", defaultValue = "0")
    var index_id: Int = 0,
    
    @ColumnInfo(name = "disabled", defaultValue = "0")
    var disabled: Int = 0,
    
    @get:JvmName("getGdId") @set:JvmName("setGdId")
    @ColumnInfo(name = "gd_id", defaultValue = "")
    var gd_id: String = "",
    
    @get:JvmName("getPlayed") @set:JvmName("setPlayed")
    @ColumnInfo(name = "played")
    var played: String? = null,
    
    @get:JvmName("getSeasonId") @set:JvmName("setSeasonId")
    @ColumnInfo(name = "season_id")
    var season_id: Int = 0,
    
    @get:JvmName("getAirDate") @set:JvmName("setAirDate")
    @ColumnInfo(name = "air_date")
    var air_date: String? = null,
    
    @get:JvmName("getEpisodeNumber") @set:JvmName("setEpisodeNumber")
    @ColumnInfo(name = "episode_number")
    var episode_number: Int = 0,
    
    var id: Int = 0,
    var name: String? = null,
    var overview: String? = null,
    
    @get:JvmName("getProductionCode") @set:JvmName("setProductionCode")
    @ColumnInfo(name = "production_code")
    var production_code: String? = null,
    
    var runtime: Int = 0,
    
    @get:JvmName("getSeasonNumber") @set:JvmName("setSeasonNumber")
    @ColumnInfo(name = "season_number")
    var season_number: Int = 0,
    
    @get:JvmName("getShowId") @set:JvmName("setShowId")
    @ColumnInfo(name = "show_id")
    var show_id: Long = 0,
    
    @get:JvmName("getStillPath") @set:JvmName("setStillPath")
    @ColumnInfo(name = "still_path")
    var still_path: String? = null,
    
    @get:JvmName("getVoteAverage") @set:JvmName("setVoteAverage")
    @ColumnInfo(name = "vote_average")
    var vote_average: Double = 0.0,
    
    @get:JvmName("getVoteCount") @set:JvmName("setVoteCount")
    @ColumnInfo(name = "vote_count")
    var vote_count: Int = 0
) : MyMedia, Parcelable
