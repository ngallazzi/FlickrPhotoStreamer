package com.ngallazzi.flickrphotostreamer

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.ngallazzi.flickrphotostreamer.activities.MainActivityViewModel
import com.ngallazzi.flickrphotostreamer.repository.models.Photo
import com.ngallazzi.flickrphotostreamer.repository.models.SearchPhotosResponse
import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions


class MainActivity : AppCompatActivity() {
    private lateinit var mActivityViewModel: MainActivityViewModel
    private lateinit var mLocationUpdatesRequest: LocationRequest
    private lateinit var mLocationSettingsRequest: LocationSettingsRequest
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var photosLiveData: LiveData<SearchPhotosResponse>
    private lateinit var errorLiveData: LiveData<String>
    private var photos: ArrayList<Photo> = ArrayList()

    private var locationUpdatesStarted = false
    private lateinit var startItem: MenuItem
    private lateinit var stopItem: MenuItem

    private lateinit var rvAdapter: RecyclerView.Adapter<*>
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient


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

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

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

    @SuppressLint("MissingPermission")
    @AfterPermissionGranted(LOCATION_PERMISSIONS_REQUEST_CODE)
    private fun startLocationUpdates() {
        val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (EasyPermissions.hasPermissions(this, *perms)) {
            // Already have permission, do the thing
            fusedLocationProviderClient.requestLocationUpdates(mLocationUpdatesRequest, mLocationCallback, null)
            locationUpdatesStarted = true
            Log.v(TAG, "Location update started")
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(
                this@MainActivity, getString(R.string.location_permissions_required),
                LOCATION_PERMISSIONS_REQUEST_CODE, *perms
            )
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
        locationUpdatesStarted = false
        Log.v(TAG, "Location update stopped")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)

        startItem = menu.findItem(R.id.start)
        stopItem = menu.findItem(R.id.stop)

        if (locationUpdatesStarted) {
            startItem.isVisible = false
            stopItem.isVisible = true
        } else {
            stopItem.isVisible = false
            startItem.isVisible = true
        }

        return true
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.start -> {
                startLocationUpdates()
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

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState)

        // Restore state members from saved instance
        savedInstanceState?.run {
            locationUpdatesStarted = getBoolean(LOCATION_UPDATES_STARTED_KEY)
        }

    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.run {
            putBoolean(LOCATION_UPDATES_STARTED_KEY, locationUpdatesStarted)
        }
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(outState)
    }

    companion object {
        const val TAG = "MainActivity"
        const val LOCATION_UPDATES_STARTED_KEY = "location_updates_started_id"
        const val LOCATION_PERMISSIONS_REQUEST_CODE = 24
        const val LOCATION_UPDATES_INTERVAL_MILLIS = 0L
        const val LOCATION_UPDATES_FASTEST_INTERVAL_MILLIS = 0L
        const val LOCATION_UPDATES_DISPLACEMENT_IN_METERS = 100f
    }
}
