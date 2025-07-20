package com.example.tournote.Functionality.Segments.Memories.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PhotoItem(
    val date: String,
    val thumbnail: String,
    val allPhotos: List<PhotosData>
):Parcelable
