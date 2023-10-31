package com.example.realtimerecording.manager.camera

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

class CameraPermission(
    private val registry: ActivityResultRegistry,
    private val onCameraGranted: () -> Unit,
    private val onPermissionDenied: () -> Unit,
) : DefaultLifecycleObserver {

    private val CAMERA = "Camera Permission"
    private val CAMERA_SETTINGS = "Camera Settings"

    private lateinit var cameraResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraPermission: ActivityResultLauncher<Array<String>>

    fun register(lifecycleOwner: LifecycleOwner) {
        cameraResultLauncher = registry.register(
            CAMERA_SETTINGS,
            lifecycleOwner,
            ActivityResultContracts.StartActivityForResult()
        ) {
            requestCameraPermission()
        }

        cameraPermission = registry.register(
            CAMERA,
            lifecycleOwner,
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.entries.forEach {

                if (it.value) {
                    if (it.key == "android.permission.CAMERA") {
                        onCameraGranted.invoke()
                    }
                } else
                    onPermissionDenied.invoke()
            }
        }
    }

    fun requestCameraPermission() {
        cameraPermission.launch(
            arrayOf(
                Manifest.permission.CAMERA,
            )
        )
    }


    fun openCameraSettings(context: Context) {
        cameraResultLauncher.launch(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            )
        )
    }


    fun destroy() {
        Log.e("Observer State", "Unregister")
        cameraResultLauncher.unregister()
        cameraPermission.unregister()
    }
}
