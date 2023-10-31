package com.example.realtimerecording.manager.location

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.realtimerecording.R
import com.example.realtimerecording.ui.custom.dialog.AskDialog.setup
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Locations : KoinComponent {
    private val gpsTracker by inject<GPSTracker>()

    fun onStart(activity: FragmentActivity) {
        gpsTracker.onStart(activity)
    }

    private fun checkPermission(
        activity: FragmentActivity,
        onRequestPermission: () -> Unit
    ): Boolean {
        Log.d(TAG, "Checking Location Permission")
        try {
            return when {
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "Requesting Location Permission")
                    onRequestPermission.invoke()
                    false
                }
                else -> {
                    Log.d(TAG, "Location Permission Granted")
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("Location Permission Error ", e.message.toString())
            return false
        }
    }


    fun requestPermission(
        activity: FragmentActivity,
        onOpenSettings: () -> Unit,
        onDeny: () -> Unit
    ) {
        setup(
            activity,
            activity.getString(R.string.ask_location_permission),
            0,
            activity.getString(R.string.grant),
            activity.getString(R.string.cancel)
        ) { isConfirm ->
            if (isConfirm) {
                onOpenSettings.invoke()
            } else
                onDeny.invoke()
        }
    }

    fun getLocation(
        activity: FragmentActivity,
        onRequestPermission: () -> Unit,
        onEnableGps: (isEnabled:Boolean) -> Unit,
        onError: (state: GPSTracker.GPSSate) -> Unit
    ): Flow<Location?>?{
        return if (checkPermission(activity,onRequestPermission))
            getCoordinates(activity, onEnableGps, onError)
        else {
            onError.invoke(GPSTracker.GPSSate.REQUESTING_PERMISSIONS)
            null
        }
    }

    @RequiresPermission(value = Manifest.permission.ACCESS_FINE_LOCATION)
    private fun getCoordinates(
        activity: FragmentActivity,
        onEnableGps: (isEnable: Boolean) -> Unit,
        onError: (state: GPSTracker.GPSSate) -> Unit
    ): Flow<Location?>?{
        Log.d("Location fetchLocation", "GetinggggggggggggggggLocation")
        return gpsTracker.requestCoordinates(activity, onEnableGps,onError)
    }


    fun onStop() {
        gpsTracker.stopUsingGPS()
    }

    companion object {
        private val TAG: String = Locations::class.java.name
    }
}