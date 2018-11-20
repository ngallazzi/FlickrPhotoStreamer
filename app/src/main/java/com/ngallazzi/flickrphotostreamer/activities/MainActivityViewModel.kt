package com.ngallazzi.flickrphotostreamer.activities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ngallazzi.flickrphotostreamer.repository.FlickrApi
import com.ngallazzi.flickrphotostreamer.repository.models.SearchPhotosResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


/**
 * Created by Nicola on 2017-03-02.
 */

class MainActivityViewModel : ViewModel() {

    var searchPhotosResponse: MutableLiveData<SearchPhotosResponse> = MutableLiveData()
    var showError: MutableLiveData<String> = MutableLiveData()


    public fun loadPhotos(lat: Double, long: Double) {
        val service = FlickrApi.flickrApiServe
        val call = service.searchPhotos(lat, long)
        call.enqueue(object : Callback<SearchPhotosResponse> {
            override fun onResponse(call: Call<SearchPhotosResponse>, response: Response<SearchPhotosResponse>) {
                if (response.isSuccessful) {
                    searchPhotosResponse.value = SearchPhotosResponse(response.body()!!.photos)
                } else {
                    showError.value = response.errorBody()!!.string()
                }
            }

            override fun onFailure(call: Call<SearchPhotosResponse>, t: Throwable) {
                showError.value = t.message
            }
        })
    }
}