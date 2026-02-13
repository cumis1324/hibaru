package com.theflexproject.thunder.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class File(
    var id: String? = null,
    var name: String? = null,
    var mimeType: String? = null,
    var modifiedTime: Date? = null,
    var size: String? = null,
    var urlString: String? = null,
    var subtitle: String? = null
) : Parcelable
