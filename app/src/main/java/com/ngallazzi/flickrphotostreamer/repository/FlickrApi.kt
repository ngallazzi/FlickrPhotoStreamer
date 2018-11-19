package com.ngallazzi.flickrphotostreamer.repository

import com.ngallazzi.flickrphotostreamer.BuildConfig
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * FlickrPhotoStreamer
 * Created by Nicola on 11/19/2018.
 * Copyright © 2018 Zehus. All rights reserved.
 */
interface FlickrApi {
    // https://api.flickr.com/services/rest/?method=flickr.photos.search&api_key=665cfc184da3b8b0809c97cbb0475aee&lat=45.4854739&lon=9.2022176&format=json&nojsoncallback=1
    @GET("search/repositories")
    fun listRepositories(@Query("method") method: String,
                         @Query("lat") latitude: Double,
                         @Query("lon") longitude: Double): Call<RepositoriesResponse>

    companion object {
        private fun create(): FlickrApi {

            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            var client = OkHttpClient.Builder().addInterceptor(interceptor).build()

            val retrofit = Retrofit.Builder()
                .addConverterFactory(
                    MoshiConverterFactory.create())
                .baseUrl(BuildConfig.FLICKR_API_ENDPOINT)
                .client(client)
                .build()

            return retrofit.create(FlickrApi::class.java)
        }


        val gitHubApiServe by lazy {
            create()
        }

    }


}