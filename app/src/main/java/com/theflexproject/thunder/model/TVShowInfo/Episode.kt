package com.theflexproject.thunder.model.TVShowInfo

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fasterxml.jackson.annotation.JsonFormat
import com.theflexproject.thunder.model.MyMedia
import kotlinx.parcelize.Parcelize
import java.util.Date

@Entity
@Parcelize
data class Episode(
    @PrimaryKey(autoGenerate = true)
    val idForDB: Int = 0,
    
    val fileName: String? = null,
    val mimeType: String? = null,
    
    @JsonFormat(pattern = "E MMM dd HH:mm:ss z yyyy")
    val modifiedTime: Date? = null,
    
    val size: String? = null,
    val urlString: String? = null,
    
    @ColumnInfo(name = "index_id", defaultValue = "0")
    val indexId: Int = 0,
    
    @ColumnInfo(name = "disabled", defaultValue = "0")
    val disabled: Int = 0,
    
    @ColumnInfo(name = "gd_id", defaultValue = "")
    val gdId: String = "",
    
    val played: Int = 0,
    
    @ColumnInfo(name = "season_id")
    val seasonId: Int = 0,
    
    @ColumnInfo(name = "air_date")
    val airDate: String? = null,
    
    @ColumnInfo(name = "episode_number")
    val episodeNumber: Int = 0,
    
    val id: Int = 0,
    val name: String? = null,
    val overview: String? = null,
    
    @ColumnInfo(name = "production_code")
    val productionCode: String? = null,
    
    val runtime: Int = 0,
    
    @ColumnInfo(name = "season_number")
    val seasonNumber: Int = 0,
    
    @ColumnInfo(name = "show_id")
    val showId: Long = 0,
    
    @ColumnInfo(name = "still_path")
    val stillPath: String? = null,
    
    @ColumnInfo(name = "vote_average")
    val voteAverage: Double = 0.0,
    
    @ColumnInfo(name = "vote_count")
    val voteCount: Int = 0
) : MyMedia, Parcelable
