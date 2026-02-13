package com.theflexproject.thunder.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class IndexLink(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    
    var link: String? = null,
    var username: String? = null,
    var password: String? = null,
    var indexType: String? = null,
    var folderType: String? = null,
    
    @ColumnInfo(name = "disabled", defaultValue = "0")
    var disabled: Int = 0
) : Parcelable
