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


    public fun loadPhotos(lat: Double, long: Double) {
        val service = FlickrApi.flickrApiServe
        val call = service.searchPhotos(lat, long)
        call.enqueue(object : Callback<SearchPhotosResponse> {
            override fun onResponse(call: Call<SearchPhotosResponse>, response: Response<SearchPhotosResponse>) {
                if (response.isSuccessful && response.body()!!.content.photos.size > 0) {
                    photos.add(response.body()!!.content.photos[0])
                    photosLiveData.postValue(photos)
                } else {
                    Log.d(TAG, "No photos found at this position. Lat: $lat, Long:$long")
                }
            }

            override fun onFailure(call: Call<SearchPhotosResponse>, t: Throwable) {
                showError.value = t.message
            }
        })
    }

    public fun clear() {
        photos.clear()
    }

    public fun getPhotos(): MutableLiveData<ArrayList<Photo>> {
        return photosLiveData
    }

    public fun getError(): MutableLiveData<String> {
        return showError
    }

    companion object {
        val TAG = PhotosViewModel::class.java.simpleName
    }
}