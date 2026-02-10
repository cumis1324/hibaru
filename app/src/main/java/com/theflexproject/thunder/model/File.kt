package com.theflexproject.thunder.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class File(
    val id: String? = null,
    val name: String? = null,
    val mimeType: String? = null,
    val modifiedTime: Date? = null,
    val size: String? = null,
    val urlString: String? = null,
    val subtitle: String? = null
) : Parcelable
