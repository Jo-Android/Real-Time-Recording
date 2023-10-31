package com.example.realtimerecording.koin.module

import com.example.realtimerecording.manager.camera.CameraManager
import com.example.realtimerecording.manager.location.GPSTracker
import com.example.realtimerecording.manager.location.LocationPermission
import com.example.realtimerecording.manager.location.Locations
import com.example.realtimerecording.manager.opencv.VideoRecorder
import com.example.realtimerecording.ui.viewModel.DrawSignatureViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

object Modules {

    val viewModel = module {
        viewModel { (DrawSignatureViewModel()) }
    }

    val manager = module {
        factory { CameraManager() }
        factory { VideoRecorder() }
        single { LocationPermission() }
        single { Locations() }
        single { GPSTracker() }
    }

}