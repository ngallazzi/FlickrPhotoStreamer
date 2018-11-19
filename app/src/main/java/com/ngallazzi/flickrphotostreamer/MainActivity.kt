package com.ngallazzi.flickrphotostreamer

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    // photo url https://farm5.staticflickr.com/4850/32085224808_33404b5832.jpg
    // get api url https://api.flickr.com/services/rest/?method=flickr.photos.search&api_key=665cfc184da3b8b0809c97cbb0475aee&lat=45.4854739&lon=9.2022176&format=json&nojsoncallback=1
}
