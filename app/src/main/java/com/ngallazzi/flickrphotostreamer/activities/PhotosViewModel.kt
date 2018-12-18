package com.ngallazzi.flickrphotostreamer.activities

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ngallazzi.flickrphotostreamer.repository.FlickrApi
import com.ngallazzi.flickrphotostreamer.repository.models.Photo
import com.ngallazzi.flickrphotostreamer.repository.models.SearchPhotosResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


/**
 * Created by Nicola on 2017-03-02.
 */

class PhotosViewModel : ViewModel() {
    private var photos: ArrayList<Photo> = arrayListOf()
    private var photosLiveData: MutableLiveData<ArrayList<Photo>> = MutableLiveData()
    private var showError: MutableLiveData<String> = MutableLiveData()

    init {
        photosLiveData.value = photos
    }


    fun loadPhotos(lat: Double, long: Double) {
        Log.d(TAG, "searching photos at $lat,$long")
        val service = FlickrApi.flickrApiServe
        val call = service.searchPhotos(lat, long)
        call.enqueue(object : Callback<SearchPhotosResponse> {
            override fun onResponse(call: Call<SearchPhotosResponse>, response: Response<SearchPhotosResponse>) {
                if (response.isSuccessful && response.body()!!.content.photos.size > 0) {
                    val downloadedPhoto = response.body()!!.content.photos[0]
                    if (isNew(downloadedPhoto)) {
                        photos.add(downloadedPhoto)
                        photosLiveData.postValue(photos)
                        Log.v(TAG, "Photos count: " + photos.size)
                    } else {
                        showError.value = "duplicated_photo_error_key"
                        Log.d(TAG, "Photo already in the list, dropping...")
                    }

                } else {
                    showError.value = "no_photos_found_error_key"
                    Log.d(TAG, "No photos found at this location")
                }
            }

            override fun onFailure(call: Call<SearchPhotosResponse>, t: Throwable) {
                showError.value = t.message
            }
        })
    }

    private fun isNew(photo: Photo): Boolean {
        for (item in photos) {
            if (item.id == photo.id) {
                return false
            }
        }
        return true
    }

    fun clear() {
        photos.clear()
    }

    fun getPhotos(): MutableLiveData<ArrayList<Photo>> {
        return photosLiveData
    }

    fun getError(): MutableLiveData<String> {
        return showError
    }

    companion object {
        val TAG = MainActivity.TAG + " - " + PhotosViewModel::class.java.simpleName
    }
}