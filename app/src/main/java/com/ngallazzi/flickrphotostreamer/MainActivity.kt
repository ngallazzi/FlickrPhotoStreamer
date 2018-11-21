package com.ngallazzi.flickrphotostreamer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.ngallazzi.flickrphotostreamer.LocationSettingsHelper.Companion.LocationSettingsStatusListener
import com.ngallazzi.flickrphotostreamer.LocationSettingsHelper.Companion.requestLocationSettingsStatus
import com.ngallazzi.flickrphotostreamer.activities.MainActivityViewModel
import com.ngallazzi.flickrphotostreamer.repository.models.Photo
import com.ngallazzi.flickrphotostreamer.repository.models.SearchPhotosResponse
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var mActivityViewModel: MainActivityViewModel
    private lateinit var mLocationUpdatesRequest: LocationRequest
    private lateinit var mLocationSettingsRequest: LocationSettingsRequest
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var photosLiveData: LiveData<SearchPhotosResponse>
    private lateinit var errorLiveData: LiveData<String>
    private var photos: ArrayList<Photo> = ArrayList()

    private var locationUpdatedStarted: Boolean = false

    private lateinit var rvAdapter: RecyclerView.Adapter<*>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mActivityViewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)

        mLocationUpdatesRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setSmallestDisplacement(LOCATION_UPDATES_DISPLACEMENT_IN_METERS)

        mLocationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationUpdatesRequest).build()

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
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
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onSettingsChangeUnavailable() {
                startActivityForResult(Intent(Settings.ACTION_SETTINGS), LOCATION_SETTINGS_REQUEST_CODE)
            }

            override fun onSettingsSatisfied() {
                if (LocationSettingsHelper.locationPermissionsAllowed(this@MainActivity)) {
                    // todo enable start button
                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSIONS_REQUEST_CODE
                    )
                }
            }
        })

        rvAdapter = PhotosAdapter(photos, this)

        initPhotosRecyclerView()

        photosLiveData = mActivityViewModel.getSearchPhotos()
        photosLiveData.observe(this@MainActivity, Observer {
            for (item in it.response.photos) {
                photos.add(item)
            }
            rvAdapter.notifyDataSetChanged()
        })

        errorLiveData = mActivityViewModel.getError()
        errorLiveData.observe(this@MainActivity, Observer {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        })

    }

    private fun initPhotosRecyclerView() {
        rvPhotoStream.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = rvAdapter
        }
    }

    private fun startLocationUpdates(request: LocationRequest) {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationProviderClient.requestLocationUpdates(request, mLocationCallback, null)
        locationUpdatedStarted = true
    }

    private fun stopLocationUpdates() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
        locationUpdatedStarted = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOCATION_SETTINGS_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    Toast.makeText(this, getString(R.string.location_settings_ok_message), Toast.LENGTH_SHORT).show()
                }
                Activity.RESULT_CANCELED -> {
                    Toast.makeText(this, getString(R.string.location_settings_unsatisfied_mesage), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        if (requestCode == LOCATION_PERMISSIONS_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    // todo enable start button
                }
                Activity.RESULT_CANCELED -> {
                    Toast.makeText(this, getString(R.string.location_settings_unsatisfied_mesage), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)

        val startItem = menu.findItem(R.id.start)
        val stopItem = menu.findItem(R.id.stop)

        if (locationUpdatedStarted) {
            startItem.isVisible = false
            stopItem.isVisible = true
        } else {
            stopItem.isVisible = false
            startItem.isVisible = true
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.start -> {
                startLocationUpdates(mLocationUpdatesRequest)
                invalidateOptionsMenu()
                true
            }
            R.id.stop -> {
                stopLocationUpdates()
                invalidateOptionsMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val TAG = "MainActivity"
        const val LOCATION_SETTINGS_REQUEST_CODE = 23
        const val LOCATION_PERMISSIONS_REQUEST_CODE = 24
        const val LOCATION_UPDATES_DISPLACEMENT_IN_METERS = 100f
    }
}
