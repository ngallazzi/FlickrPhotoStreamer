package com.ngallazzi.flickrphotostreamer

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

/**
 * FlickrPhotoStreamer
 * Created by Nicola on 11/20/2018.
 * Copyright © 2018 Zehus. All rights reserved.
 */

class LocationSettingsHelper {
    companion object {
        private val TAG = LocationSettingsHelper::class.java.simpleName

        fun requestLocationSettingsStatus(
            context: Context,
            locationSettingsRequest: LocationSettingsRequest,
            listener: LocationSettingsStatusListener
        ) {
            val locationSettingsTask =
                LocationServices.getSettingsClient(context).checkLocationSettings(locationSettingsRequest)
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

        interface LocationSettingsStatusListener {
            fun onSettingsSatisfied()

            fun onResolutionRequired(exception: ResolvableApiException)

            fun onSettingsChangeUnavailable()

        }
    }
}
