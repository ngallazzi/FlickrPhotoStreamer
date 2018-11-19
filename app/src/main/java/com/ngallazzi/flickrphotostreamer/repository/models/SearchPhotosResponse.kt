package com.ngallazzi.flickrphotostreamer.repository.models

import com.squareup.moshi.Json

data class SearchPhotosResponse(
    @Json(name = "photos") var photos: Photos
)

data class Photos(@Json(name = "photo") var photos: List<Photo>)