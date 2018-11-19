package com.ngallazzi.flickrphotostreamer.repository.models

import com.squareup.moshi.Json

data class Photo(
    @Json(name = "id") var id: String,
    @Json(name = "owner") var owner: String,
    @Json(name = "secret") var secret: String?,
    @Json(name = "server") val server: String?,
    @Json(name = "farm") val farm: String?,
    @Json(name = "title") val title: String?
)