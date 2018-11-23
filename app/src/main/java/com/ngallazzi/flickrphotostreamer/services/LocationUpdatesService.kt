package com.ngallazzi.flickrphotostreamer.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.ngallazzi.flickrphotostreamer.MainActivity
import java.util.concurrent.TimeUnit
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat.PRIORITY_LOW


class LocationUpdatesService : Service() {
    private lateinit var mLocationUpdatesRequest: LocationRequest
    private lateinit var mLocationSettingsRequest: LocationSettingsRequest
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onCreate() {
        super.onCreate()

        mLocationUpdatesRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(LOCATION_UPDATES_INTERVAL_MILLIS)
            .setFastestInterval(LOCATION_UPDATES_FASTEST_INTERVAL_MILLIS)
            .setExpirationDuration(EXPIRATION_DURATION_MINUTES)
            .setSmallestDisplacement(LOCATION_UPDATES_DISPLACEMENT_IN_METERS)

        mLocationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationUpdatesRequest).build()

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.v(MainActivity.TAG, "Location update received")
                for (location in locationResult.locations) {
                    // notify listeners
                    sendPositionChangedBroadcast(location.latitude, location.longitude)
                    Log.v(TAG, "position changed: " + location.latitude + "," + location.longitude)
                }
            }
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action

            when (action) {
                ACTION_START_FOREGROUND_SERVICE -> {
                    startForegroundService()
                    Toast.makeText(applicationContext, "Foreground service is started.", Toast.LENGTH_LONG).show()
                }
                ACTION_STOP_FOREGROUND_SERVICE -> {
                    stopForegroundService()
                    Toast.makeText(applicationContext, "Foreground service is stopped.", Toast.LENGTH_LONG).show()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("MissingPermission")
    private fun startForegroundService() {
        Log.d(TAG, "Start foreground service.")
        startForeground(NOTIFICATION_ID, getNotification())
        fusedLocationProviderClient.requestLocationUpdates(mLocationUpdatesRequest, mLocationCallback, null)
        Log.v(MainActivity.TAG, "Location update started")

    }

    fun getNotification(): Notification {
        val channel: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            channel = createChannel()
        else {
            channel = ""
        }
        val mBuilder = NotificationCompat.Builder(this, channel).setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("snap map fake location")


        return mBuilder
            .setPriority(PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    @TargetApi(26)
    @Synchronized
    private fun createChannel(): String {
        val mNotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val name = "snap map fake location "
        val importance = NotificationManager.IMPORTANCE_LOW

        val mChannel = NotificationChannel("snap map channel", name, importance)

        mChannel.enableLights(true)
        mChannel.lightColor = Color.BLUE
        if (mNotificationManager != null) {
            mNotificationManager.createNotificationChannel(mChannel)
        } else {
            stopSelf()
        }
        return "snap map channel"
    }

    private fun stopForegroundService() {
        Log.d(TAG, "Stop foreground service.")

        fusedLocationProviderClient.removeLocationUpdates(mLocationCallback)

        // Stop foreground service and remove the notification.
        stopForeground(true)

        // Stop the foreground service.
        stopSelf()
    }


    override fun onDestroy() {
        super.onDestroy()
        fusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
        Log.v(TAG, "Location update stopped")
    }

    private fun sendPositionChangedBroadcast(latitude: Double, longitude: Double) {
        val intent = Intent()
        intent.action = POSITION_CHANGED_ACTION_ID
        intent.putExtra(POSITION_LATITUDE, latitude)
        intent.putExtra(POSITION_LONGITUDE, longitude)
        sendBroadcast(intent)
    }

    companion object {
        private val TAG = LocationUpdatesService::class.java.simpleName

        const val NOTIFICATION_ID = 1
        const val ACTION_START_FOREGROUND_SERVICE = "start_service"
        const val ACTION_STOP_FOREGROUND_SERVICE = "stop_service"

        const val LOCATION_UPDATES_INTERVAL_MILLIS = 0L
        const val LOCATION_UPDATES_FASTEST_INTERVAL_MILLIS = 0L
        const val LOCATION_UPDATES_DISPLACEMENT_IN_METERS = 100f
        val EXPIRATION_DURATION_MINUTES = TimeUnit.MINUTES.toMillis(120)

        const val POSITION_CHANGED_ACTION_ID = "position_changed_action"
        const val POSITION_LATITUDE = "position_latitude"
        const val POSITION_LONGITUDE = "position_longitude"
    }
}