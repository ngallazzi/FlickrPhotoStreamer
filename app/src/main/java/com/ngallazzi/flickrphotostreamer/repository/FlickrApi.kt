package com.ngallazzi.flickrphotostreamer.repository

import com.ngallazzi.flickrphotostreamer.BuildConfig
import com.ngallazzi.flickrphotostreamer.repository.models.SearchPhotosResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * FlickrPhotoStreamer
 * Created by Nicola on 11/19/2018.
 * Copyright © 2018 Zehus. All rights reserved.
 */

// photo url https://farm5.staticflickr.com/4850/32085224808_33404b5832.jpg
// get api url https://api.flickr.com/services/rest/?method=flickr.photos.search&api_key=665cfc184da3b8b0809c97cbb0475aee&lat=45.4854739&lon=9.2022176&format=json&nojsoncallback=1


interface FlickrApi {
    @GET(
        "rest?api_key=" + BuildConfig.API_KEY + "&method=flickr.photos.search" +
                "&format=json&nojsoncallback=1&page=1&per_page=1&radius=0.10" +
                ""
    )
    fun searchPhotos(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double
    ): Call<SearchPhotosResponse>

    companion object {
        private fun create(): FlickrApi {

            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            var client = OkHttpClient.Builder().addInterceptor(interceptor).build()
            val baseUrl = BuildConfig.FLICKR_API_ENDPOINT

            val retrofit = Retrofit.Builder()
                .addConverterFactory(
                    MoshiConverterFactory.create()
                )
                .baseUrl(baseUrl)
                .client(client)
                .build()

            return retrofit.create(FlickrApi::class.java)
        }


        val flickrApiServe by lazy {
            create()
        }

    }


}