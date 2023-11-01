package com.example.realtimerecording.ui.fragments.base

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.example.realtimerecording.R
import com.example.realtimerecording.manager.camera.CameraPermission
import com.example.realtimerecording.manager.location.GPSTracker
import com.example.realtimerecording.manager.location.LocationPermission
import com.example.realtimerecording.manager.location.Locations
import com.example.realtimerecording.ui.custom.dialog.AskDialog.askPermissionDialog
import kotlinx.coroutines.flow.Flow
import org.koin.android.ext.android.inject

abstract class PermissionFragment<B : ViewBinding>(
    private val bindingFactory: (LayoutInflater) -> B,
) : Fragment() {

    private var _binding: B? = null
    val binding get() = _binding!!
    private lateinit var cameraPermission: CameraPermission
    private val locationPermission by inject<LocationPermission>()
    val location by inject<Locations>()

    private var isAlreadyRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraPermission = CameraPermission(
            requireActivity().activityResultRegistry,
            onCameraGranted = {
                onCameraGranted()
            },
            onPermissionDenied = {
                askPermissionDialog(
                    requireContext(),
                    getString(R.string.ask_camera_permission)
                ) {
                    if (it)
                        cameraPermission.openCameraSettings(requireContext())
                    else {
                        Log.e("TAG ", "Error Opening Camera Permission Denied")
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.camera_access_error),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = bindingFactory(inflater)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initLayout()
    }

    abstract fun initLayout()

    abstract fun onCameraGranted()

    override fun onStart() {
        super.onStart()
        isAlreadyRequested = false
        location.onStart(requireActivity())
        cameraPermission.register(viewLifecycleOwner)
        locationPermission.register(
            viewLifecycleOwner,
            requireActivity().activityResultRegistry,
            onLocationGranted = { onLocationPermissionGranted() },
            onPermissionDenied = {
                if (isAlreadyRequested)
                    onLocationPermissionDenied()
                else {
                    isAlreadyRequested = true
                    location.requestPermission(
                        requireActivity(),
                        onOpenSettings = { locationPermission.openLocationSettings(requireContext()) },
                        onDeny = { onLocationPermissionDenied() })
                }
            }
        )
        requestCameraPermissions()
    }

    override fun onStop() {
        super.onStop()
        cameraPermission.destroy()
        locationPermission.destroy()
        location.onStop()
    }

    abstract fun requestCameraPermissions()

    abstract fun onLocationPermissionGranted()
    abstract fun onLocationPermissionDenied()

    abstract fun onGPSTurnOnDenied()

    fun requestCameraPermission() {
        cameraPermission.requestCameraPermission()
    }

    fun getCoordinates(onError: (state: GPSTracker.GPSSate) -> Unit): Flow<Location?>? {
        return location.getLocation(
            requireActivity(),
            onRequestPermission = { locationPermission.requestLocationPermission() },
            onEnableGps = { if (it) locationPermission.enableGPS() else onGPSTurnOnDenied() },
            onError
        )
    }
}