package com.ngallazzi.flickrphotostreamer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.ngallazzi.flickrphotostreamer.LocationSettingsHelper.Companion.LocationSettingsStatusListener
import com.ngallazzi.flickrphotostreamer.LocationSettingsHelper.Companion.requestLocationSettingsStatus
import com.ngallazzi.flickrphotostreamer.activities.MainActivityViewModel
import com.ngallazzi.flickrphotostreamer.repository.models.SearchPhotosResponse
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var mActivityViewModel: MainActivityViewModel
    private lateinit var mLocationUpdatesRequest: LocationRequest
    private lateinit var mLocationSettingsRequest: LocationSettingsRequest
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var photosLiveData: LiveData<SearchPhotosResponse>
    private lateinit var errorLiveData: LiveData<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mActivityViewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)

        mLocationUpdatesRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(TimeUnit.SECONDS.toMillis(LOCATION_UPDATES_BASE_INTERVAL_IN_SECONDS))
            .setFastestInterval(TimeUnit.SECONDS.toMillis(LOCATION_UPDATES_FAST_INTERVAL_IN_SECONDS))

        mLocationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationUpdatesRequest).build()

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                if (locationResult == null) {
                    // TODO show some error
                }
                for (location in locationResult!!.locations) {
                    // refresh viewModel
                    mActivityViewModel.loadPhotos(location.latitude, location.longitude)
                }
            }
        }

        requestLocationSettingsStatus(this, mLocationSettingsRequest, object :
            LocationSettingsStatusListener {
            override fun onResolutionRequired(exception: ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this@MainActivity, LOCATION_SETTINGS_REQUEST_CODE)
                } catch (e: IntentSender.SendIntentException) {
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onSettingsChangeUnavailable() {
                startActivityForResult(Intent(Settings.ACTION_SETTINGS), LOCATION_SETTINGS_REQUEST_CODE)
            }

            override fun onSettingsSatisfied() {
                // TODO enable start button here
                Toast.makeText(this@MainActivity, "Settings satisfied", Toast.LENGTH_SHORT).show()
            }
        })

        photosLiveData = mActivityViewModel.getSearchPhotos()
        photosLiveData.observe(this@MainActivity, Observer {
            // todo update UI
            for (item in it.response.photos) {
                Log.v(TAG, item.title)
            }
        })

        errorLiveData = mActivityViewModel.getError()
        errorLiveData.observe(this@MainActivity, Observer {
            // todo show and error
        })

    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(request: LocationRequest) {
        var fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationProviderClient.requestLocationUpdates(request, mLocationCallback, null)
    }

    private fun stopLocationUpdates() {
        var fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOCATION_SETTINGS_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    // TODO enable start button here
                }
                Activity.RESULT_CANCELED -> {
                    // TODO show some error here
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.start -> {
                startLocationUpdates(mLocationUpdatesRequest)
                true
            }
            R.id.stop -> {
                stopLocationUpdates()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val TAG = "MainActivity"
        const val LOCATION_SETTINGS_REQUEST_CODE = 23
        const val LOCATION_UPDATES_BASE_INTERVAL_IN_SECONDS = 5L
        const val LOCATION_UPDATES_FAST_INTERVAL_IN_SECONDS = 1L
    }
}
