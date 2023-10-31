package com.example.realtimerecording.manager.location

import android.Manifest
import android.app.Service
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.fragment.app.FragmentActivity
import com.example.realtimerecording.R
import com.example.realtimerecording.ui.custom.dialog.AskDialog.setup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class GPSTracker : Service(), LocationListener {

    private lateinit var locationManager: LocationManager

    fun onStart(activity: FragmentActivity) {
        Log.e(TAG,"Registering LOCATION_SERVICE")
        locationManager = activity.getSystemService(LOCATION_SERVICE) as LocationManager
    }

    private var locCounter = 0

    @RequiresPermission(value = Manifest.permission.ACCESS_FINE_LOCATION)
    fun requestCoordinates(
        activity: FragmentActivity,
        onEnableGps: (isEnable:Boolean) -> Unit,
        onError: (state: GPSSate) -> Unit,
        counter: Int? = 0,
    ): Flow<Location?>? {
        return if (::locationManager.isInitialized) {
            if (!canGetLocation()) {
                Log.e(TAG, "Location Error -> no network provider is enabled")
                showSettingsAlert(
                    activity,
                    counter,
                    activity.getString(R.string.gps_disabled),
                    activity.getString(R.string.go_settings),
                    onEnableGps
                )
                onError.invoke(GPSSate.REQUESTING_TURN_ON_GPS)
                null
            } else {
                try {
                    Log.d("Location fetchLocation", "GetinggggggggggggggggLocation")
                    locCounter = 0
                    flow { getLocationCoordinates(this) }.flowOn(Dispatchers.Main)
                } catch (e: Exception) {
                    onError.invoke(GPSSate.COORDINATE_ERROR)
                    null
                }
            }
        } else {
            onError.invoke(GPSSate.LOCATION_MANAGER_STOPPED)
            null
        }
    }

    @RequiresPermission(value = Manifest.permission.ACCESS_FINE_LOCATION)
    private suspend fun getLocationCoordinates(location: FlowCollector<Location?>) {

        Log.d(TAG, "Location State -> getting Coordinates")

        // if GPS Enabled get lat/long using GPS Services
        val isGPSEnabled =
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (isGPSEnabled) {
            //check the network permission
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_BW_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
            )
            Log.d(TAG, "GPS Enabled")
            delay(500)
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).also {
                if (it != null) {
                    Log.d(
                        TAG,
                        "By GPS -> Coordinates ${it.latitude} , ${it.longitude}"
                    )
                    location.emit(it)
                } else if (locCounter < 10) {
                    locCounter++
                    delay(1000)
                    getLocationCoordinates(location)
                } else {
                    Log.e(TAG, "Getting Coordinates GPS Error -> location variable is null")
                    location.emit(null)
                }
            }
        }
        /*else {
            Log.e(TAG, "Location Get error -> GPS is Disabled Trying From Network")
            getCoordinatesFromNetwork(locationManager)
        }*/
    }

    @RequiresPermission(value = Manifest.permission.ACCESS_FINE_LOCATION)
    private suspend fun getCoordinatesFromNetwork(
        locationManager: LocationManager
    ): Location? {
        val isNetworkEnabled =
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return if (isNetworkEnabled) {
            //check the network permission
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                MIN_TIME_BW_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
            )
            delay(500)
            Log.d(TAG, "Location Network Network")
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).also {
                if (it != null)
                    Log.d(
                        TAG,
                        "By Network -> Coordinates ${it.latitude} , ${it.longitude}"
                    )
                else
                    Log.e(
                        TAG,
                        "Location Getting Coordinates Network Error -> location variable is null"
                    )
                it
            }
        } else {
            Log.e(TAG, "Location Get error -> Network is Disabled")
            null
        }
    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     */
    fun stopUsingGPS() {
        if (::locationManager.isInitialized)
            locationManager.removeUpdates(this)
        else
            Log.e(TAG, "Couldn't Stop GPS -> LocationManager not initialized")
    }

    /**
     * Function to check GPS/wifi enabled
     * @return boolean
     */
    private fun canGetLocation(): Boolean {
        return if (::locationManager.isInitialized) {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) //|| locationManager.isProviderEnabled(
            //LocationManager.NETWORK_PROVIDER
            //)
        } else {
            Log.e(TAG, "Couldn't Check if Location Enabled -> LocationManager not initialized")
            false
        }
    }

    private fun showSettingsAlert(
        activity: FragmentActivity,
        counter: Int?,
        alertText: String,
        confirmBtn: String,
        onEnableGps: (isEnable:Boolean) -> Unit
    ) {
        if (counter != null) {
            if (counter <= 0)
                enableGPSAlert(activity, alertText, confirmBtn, onEnableGps)
            else
                Toast.makeText(activity, "GPS Disabled !!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enableGPSAlert(
        activity: FragmentActivity,
        alertText: String,
        confirmBtn: String,
        onEnableGps: (isEnable:Boolean) -> Unit
    ) {
        setup(
            activity,
            alertText,
            R.drawable.ic_gps_disabled,
            confirmBtn,
            activity.getString(R.string.cancel)
        ) { isConfirm ->

            if (isConfirm)
                onEnableGps.invoke(true)
            else
                onEnableGps.invoke(false)
        }
    }

    override fun onLocationChanged(location: Location) {}
    override fun onProviderDisabled(provider: String) {
//        showSettingsAlert(0)
    }

    override fun onProviderEnabled(provider: String) {}

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
    }

    override fun onBind(arg0: Intent): IBinder? {
        return null
    }

    companion object {
        // The minimum distance to change Updates in meters
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 10 // 10 meters

        // The minimum time between updates in milliseconds
        private const val MIN_TIME_BW_UPDATES = (1000 * 60 * 1 // 1 minute
                ).toLong()
        private val TAG = GPSTracker::class.java.simpleName
    }

    enum class GPSSate {
        LOCATION_MANAGER_STOPPED,
        REQUESTING_TURN_ON_GPS,
        COORDINATE_ERROR,
        REQUESTING_PERMISSIONS
    }
}