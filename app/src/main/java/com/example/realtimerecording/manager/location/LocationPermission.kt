package com.example.realtimerecording.manager.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class LocationPermission : DefaultLifecycleObserver {
    private val LOCATION = "Location Permission"
    private val LOCATION_SETTINGS = "Location Settings"
    private val GPS_SETTINGS = "GPS Settings"

    private lateinit var locationResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var gpsResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    fun register(
        lifecycleOwner: LifecycleOwner,
        registry: ActivityResultRegistry,
        onLocationGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        locationResultLauncher = registry.register(
            LOCATION_SETTINGS,
            lifecycleOwner,
            ActivityResultContracts.StartActivityForResult()
        ) {
            requestLocationPermission()
        }

        gpsResultLauncher = registry.register(
            GPS_SETTINGS,
            lifecycleOwner,
            ActivityResultContracts.StartActivityForResult()
        ) {
            requestLocationPermission()
        }

        permissionLauncher = registry.register(
            LOCATION,
            lifecycleOwner,
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            var isFineGranted = false
            var isCoarseGranted = false
            for (it in permissions.entries) {
                if (it.key == "android.permission.ACCESS_FINE_LOCATION") {
                    if (it.value)
                        isFineGranted=true
                    else
                        break
                }
                if (it.key == "android.permission.ACCESS_COARSE_LOCATION") {
                    if (it.value)
                        isCoarseGranted=true
                    else
                        break
                }
            }
            if (isFineGranted&&isCoarseGranted)
                onLocationGranted.invoke()
            else
                onPermissionDenied.invoke()
        }
    }

    fun requestLocationPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }


    fun openLocationSettings(context: Context) {
        locationResultLauncher.launch(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            )
        )
    }

    fun enableGPS() {
        gpsResultLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }


    fun destroy() {
        Log.e("Observer State", "Unregister")
        locationResultLauncher.unregister()
        permissionLauncher.unregister()
    }
}