package com.ngallazzi.flickrphotostreamer

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import java.util.concurrent.TimeUnit

/**
 * FlickrPhotoStreamer
 * Created by Nicola on 11/20/2018.
 * Copyright © 2018 Zehus. All rights reserved.
 */

class LocationHelper(private val mContext: Context) {
    private val mLocationRequest: LocationRequest
    private val mLocationSettingsBuilder: LocationSettingsRequest.Builder
    private val mFusedLocationClient: FusedLocationProviderClient
    private val mLocationSettingsRequest: LocationSettingsRequest

    init {

        mLocationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(TimeUnit.SECONDS.toMillis(5))        // 10 seconds, in milliseconds
            .setFastestInterval(TimeUnit.SECONDS.toMillis(1)) // 1 second, in milliseconds

        mLocationSettingsBuilder = LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationRequest)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext)

        // Create LocationSettingsRequest object using location request
        mLocationSettingsRequest = mLocationSettingsBuilder.build()
    }

    fun requestLocationSettingsStatus(listener: LocationSettingsStatusListener) {
        val locationSettingsTask =
            LocationServices.getSettingsClient(mContext).checkLocationSettings(mLocationSettingsBuilder.build())
        locationSettingsTask.addOnCompleteListener {
            try {
                // All location settings are satisfied. The client can initialize location
                // requests here.
                listener.onSettingsSatisfied()
                Log.v(TAG, "Settings satisfied")

            } catch (exception: ApiException) {
                Log.e(TAG, "Settings not satisfied: " + exception.message)
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->
                        // Location settings are not satisfied. But could be fixed by showing the
                        // user a dialog.
                        try {
                            listener.onResolutionRequired(exception as ResolvableApiException)
                        } catch (e: ClassCastException) {
                            // Ignore, should be an impossible error.
                        }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE ->
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        listener.onSettingsChangeUnavailable()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun requestLocationUpdates(listener: LocationUpdateListener) {

        val settingsClient = LocationServices.getSettingsClient(mContext)
        settingsClient.checkLocationSettings(mLocationSettingsRequest)

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                if (locationResult == null) {
                    listener.onError()
                }
                for (location in locationResult!!.locations) {
                    listener.onLocationRetrieved(location.latitude, location.longitude)
                }
                mFusedLocationClient.removeLocationUpdates(this)
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                if (!locationAvailability.isLocationAvailable) {
                    listener.onError()
                }
            }
        }, Looper.myLooper())
    }

    interface LocationSettingsStatusListener {
        fun onSettingsSatisfied()

        fun onResolutionRequired(exception: ResolvableApiException)

        fun onSettingsChangeUnavailable()
    }

    interface LocationUpdateListener {
        fun onLocationRetrieved(latitude: Double, longitude: Double)

        fun onError()
    }

    companion object {
        private val TAG = LocationHelper::class.java.simpleName
    }
}
