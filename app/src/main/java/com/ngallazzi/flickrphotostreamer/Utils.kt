package com.ngallazzi.flickrphotostreamer

import android.content.Context
import android.net.ConnectivityManager


class Utils {
    companion object {
        fun getErrorStringByIdentifier(key: String, context: Context): String {
            try {
                val id = context.getResources()
                    .getIdentifier(key, "string", context.getPackageName())
                return context.getString(id)
            } catch (e: Exception) {
                if (!isNetworkAvailable(context)) {
                    return context.getString(R.string.no_internet_connection_error)
                }
                return key
            }
        }

        fun isNetworkAvailable(context: Context?): Boolean {
            if (context == null) {
                return false
            }
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }

    }
}