package com.ngallazzi.flickrphotostreamer

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.ngallazzi.flickrphotostreamer.activities.PhotosViewModel
import com.ngallazzi.flickrphotostreamer.repository.models.Photo
import com.ngallazzi.flickrphotostreamer.services.LocationUpdatesService
import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions


class MainActivity : AppCompatActivity() {
    lateinit var mActivityViewModel: PhotosViewModel
    private lateinit var photosLiveData: LiveData<ArrayList<Photo>>
    private lateinit var errorLiveData: LiveData<String>
    private var photos: ArrayList<Photo> = ArrayList()
    private lateinit var locationUpdatesService: LocationUpdatesService

    private var locationUpdatesStarted = false
    private lateinit var startItem: MenuItem
    private lateinit var stopItem: MenuItem

    private lateinit var rvAdapter: RecyclerView.Adapter<*>

    private lateinit var brPositionChanged: BroadcastReceiver


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mActivityViewModel = ViewModelProviders.of(this).get(PhotosViewModel::class.java)
        rvAdapter = PhotosAdapter(photos, this)

        initPhotosRecyclerView()

        photosLiveData = mActivityViewModel.getPhotos()

        photosLiveData.observe(this@MainActivity, Observer {
            if (it.size > 0) {
                photos.add(0, it.last())
                rvAdapter.notifyDataSetChanged()
            }
        })

        errorLiveData = mActivityViewModel.getError()
        errorLiveData.observe(this@MainActivity, Observer {
            val error = Utils.getErrorStringByIdentifier(it, this)
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        })

        locationUpdatesService = LocationUpdatesService()

        registerPositionBroadcastReceiver()
    }

    private fun registerPositionBroadcastReceiver() {
        val filter = IntentFilter().apply {
            addAction(LocationUpdatesService.POSITION_CHANGED_ACTION_ID)
        }

        brPositionChanged = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                //extract our message from intent
                val latitude = intent.getDoubleExtra(LocationUpdatesService.POSITION_LATITUDE, 0.0)
                val longitude = intent.getDoubleExtra(LocationUpdatesService.POSITION_LONGITUDE, 0.0)
                //log our message value
                Log.i(TAG, "Position update received. Lat: $latitude , Long: $longitude")
                mActivityViewModel.loadPhotos(latitude, longitude)
            }
        }

        registerReceiver(brPositionChanged, filter)
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
            val intent = Intent(this@MainActivity, LocationUpdatesService::class.java)
            intent.setAction(LocationUpdatesService.ACTION_START_FOREGROUND_SERVICE)
            startService(intent)
            Toast.makeText(this, getString(R.string.location_updates_started), Toast.LENGTH_SHORT).show()
            locationUpdatesStarted = true
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(
                this@MainActivity, getString(R.string.location_permissions_required),
                LOCATION_PERMISSIONS_REQUEST_CODE, *perms
            )
        }
    }

    fun stopLocationUpdates() {
        val intent = Intent(this@MainActivity, LocationUpdatesService::class.java)
        intent.setAction(LocationUpdatesService.ACTION_STOP_FOREGROUND_SERVICE)
        startService(intent)
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

    override fun onNewIntent(intent: Intent?) {
        // override to avoid activity recreation when clicking on service notification
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
                mActivityViewModel.clear()
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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(brPositionChanged)
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
    }

}
