package com.ngallazzi.flickrphotostreamer

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ngallazzi.flickrphotostreamer.activities.MainActivityViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var mActivityViewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mActivityViewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)

        // Create the observer which updates the UI.
        mActivityViewModel.searchPhotosResponse.observe(this, Observer { response ->
            Log.v(TAG, response.toString())
        })

        mActivityViewModel.showError.observe(this, Observer {
            Toast.makeText(
                this@MainActivity,
                getString(R.string.api_error, it), Toast.LENGTH_SHORT
            ).show()
        })

        // call api
        mActivityViewModel.loadPhotos(45.52, 9.19)
    }

    companion object {
        const val TAG = "MainActivity"
    }

}
