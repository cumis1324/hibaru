package com.theflexproject.thunder.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class ResFormat(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val data: Data? = null,
    val nextPageToken: String? = null,
    val curPageIndex: String? = null,
    val code: Int = 0
) : Parcelable
