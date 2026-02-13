package com.theflexproject.thunder.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class ResFormat(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    
    var data: Data? = null,
    var nextPageToken: String? = null,
    var curPageIndex: String? = null,
    var code: Int = 0
) : Parcelable
