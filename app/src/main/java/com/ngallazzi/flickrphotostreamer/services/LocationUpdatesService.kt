package com.ngallazzi.flickrphotostreamer.services

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import com.google.android.gms.location.*
import com.ngallazzi.flickrphotostreamer.MainActivity
import java.util.concurrent.TimeUnit


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
                }
                ACTION_STOP_FOREGROUND_SERVICE -> {
                    stopForegroundService()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("MissingPermission")
    private fun startForegroundService() {
        startForeground(NOTIFICATION_ID, getNotification())
        fusedLocationProviderClient.lastLocation.addOnSuccessListener {
            sendPositionChangedBroadcast(it.latitude, it.longitude)
        }

        fusedLocationProviderClient.requestLocationUpdates(mLocationUpdatesRequest, mLocationCallback, null)
        Log.v(MainActivity.TAG, "Location update started")
    }

    private fun getNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel()

        val mBuilder = NotificationCompat.Builder(this, CHANNEL_NAME).apply {
            setSmallIcon(android.R.drawable.ic_menu_mylocation)
            setContentTitle(NOTIFICATION_MESSAGE)
            setContentIntent(getMainActivityPendingIntent())
        }

        return mBuilder
            .setPriority(PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun getMainActivityPendingIntent(): PendingIntent {
        // Create an Intent for the activity you want to start
        val intent = Intent(this, MainActivity::class.java)
        // Create the TaskStackBuilder
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            intent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        return pendingIntent!!
    }

    @TargetApi(26)
    @Synchronized
    private fun createChannel() {
        val mNotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val importance = NotificationManager.IMPORTANCE_LOW

        val mChannel = NotificationChannel(CHANNEL_NAME, NOTIFICATION_MESSAGE, importance)

        mNotificationManager.createNotificationChannel(mChannel)
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
        Log.v(TAG, "Location updates stopped")
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
        const val CHANNEL_NAME = "FLICKR_PHOTO_STREAMER"
        const val NOTIFICATION_MESSAGE = "listening for position updates"

        const val LOCATION_UPDATES_INTERVAL_MILLIS = 0L
        const val LOCATION_UPDATES_FASTEST_INTERVAL_MILLIS = 0L
        const val LOCATION_UPDATES_DISPLACEMENT_IN_METERS = 100f
        val EXPIRATION_DURATION_MINUTES = TimeUnit.MINUTES.toMillis(180) // Three hours

        const val POSITION_CHANGED_ACTION_ID = "position_changed_action"
        const val POSITION_LATITUDE = "position_latitude"
        const val POSITION_LONGITUDE = "position_longitude"
    }
}