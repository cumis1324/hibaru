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
    val id: Int = 0,
    
    val link: String? = null,
    val username: String? = null,
    val password: String? = null,
    val indexType: String? = null,
    val folderType: String? = null,
    
    @ColumnInfo(name = "disabled", defaultValue = "0")
    val disabled: Int = 0
) : Parcelable
