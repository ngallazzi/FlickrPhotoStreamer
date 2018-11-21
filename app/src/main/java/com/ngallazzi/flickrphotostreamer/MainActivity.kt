package com.ngallazzi.flickrphotostreamer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
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

    private lateinit var startItem: MenuItem
    private lateinit var stopItem: MenuItem

    private var locationUpdatedStarted: Boolean = false

    private lateinit var rvAdapter: RecyclerView.Adapter<*>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mActivityViewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)

        mLocationUpdatesRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(LOCATION_UPDATES_INTERVAL_MILLIS)
            .setFastestInterval(LOCATION_UPDATES_FASTEST_INTERVAL_MILLIS)
            .setSmallestDisplacement(LOCATION_UPDATES_DISPLACEMENT_IN_METERS)

        mLocationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationUpdatesRequest).build()

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.v(TAG, "Location update received")
                for (location in locationResult.locations) {
                    // refresh viewModel
                    mActivityViewModel.loadPhotos(location.latitude, location.longitude)
                    Log.v(TAG, "Loading photos\nLat: " + location.latitude + " Long: " + location.longitude)
                }
            }
        }


        rvAdapter = PhotosAdapter(photos, this)

        initPhotosRecyclerView()

        photosLiveData = mActivityViewModel.getSearchPhotos()
        photosLiveData.observe(this@MainActivity, Observer {
            for (item in it.response.photos) {
                photos.add(0, item)
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
        if (checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
            // Permission granted
            fusedLocationProviderClient.requestLocationUpdates(request, mLocationCallback, null)
            locationUpdatedStarted = true
            Log.v(TAG, "Location updated started")
        } else {
            // todo show an error
        }
    }

    private fun stopLocationUpdates() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
        locationUpdatedStarted = false
        Log.v(TAG, "Location updated stopped")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOCATION_SETTINGS_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    if (LocationSettingsHelper.locationPermissionsAllowed(this@MainActivity)) {
                        startItem.isEnabled = true
                    } else {
                        // No explanation needed, we can request the permission.
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            LOCATION_PERMISSIONS_REQUEST_CODE
                        )
                    }
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

        startItem = menu.findItem(R.id.start)
        stopItem = menu.findItem(R.id.stop)

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
                    startItem.isEnabled = true
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
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSIONS_REQUEST_CODE) {
            when (grantResults[0]) {
                PackageManager.PERMISSION_GRANTED -> {
                    startItem.isEnabled = true
                }
                else -> {
                    Toast.makeText(this, getString(R.string.location_settings_unsatisfied_mesage), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.start -> {
                startLocationUpdates(mLocationUpdatesRequest)
                startItem.isVisible = false
                stopItem.isVisible = true
                true
            }
            R.id.stop -> {
                stopLocationUpdates()
                startItem.isVisible = true
                stopItem.isVisible = false
                true
            }
            R.id.clear -> {
                photos.clear()
                rvAdapter.notifyDataSetChanged()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val TAG = "MainActivity"
        const val LOCATION_SETTINGS_REQUEST_CODE = 23
        const val LOCATION_PERMISSIONS_REQUEST_CODE = 24
        const val LOCATION_UPDATES_INTERVAL_MILLIS = 0L
        const val LOCATION_UPDATES_FASTEST_INTERVAL_MILLIS = 0L
        const val LOCATION_UPDATES_DISPLACEMENT_IN_METERS = 100f
    }
}
