package com.example.realtimerecording.ui.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.os.CountDownTimer
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.Toast
import androidx.core.content.FileProvider.getUriForFile
import androidx.core.view.isVisible
import com.example.realtimerecording.databinding.FragmentDrawSignatureBinding
import com.example.realtimerecording.databinding.LayoutSignatureBinding
import com.example.realtimerecording.manager.camera.CameraManager
import com.example.realtimerecording.manager.camera.SignatureInterface
import com.example.realtimerecording.manager.location.GPSTracker
import com.example.realtimerecording.ui.fragments.base.PermissionFragment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import org.koin.android.ext.android.inject
import java.io.File
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


class DrawSignatureFragment : SignatureInterface,
    PermissionFragment<FragmentDrawSignatureBinding>(FragmentDrawSignatureBinding::inflate) {
    private var isRecording: Boolean = true
    private var counter: CountDownTimer? = null
    private var isFromStop: Boolean = false
    private val TAG = DrawSignatureFragment::class.java.simpleName

    private val cameraManager by inject<CameraManager>()


    private val f: NumberFormat = DecimalFormat("00")

    override fun initLayout() {
        binding.apply {
            signatureLL.setupSignatureLayout()
            waiting.root.isVisible = true
            cameraRoot.isVisible = false
            counterRoot.isVisible = false
        }
    }

    private fun setupCounter() {
        binding.counterRoot.isVisible = true
        counter = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Used for formatting digit to be in 2 digits only
                binding.counter.setText(
                    java.lang.StringBuilder(":")
                        .append(f.format(10 - (millisUntilFinished / 1000 % 60)))
                )
            }

            // When the task is over it will print 00:00:00 there
            override fun onFinish() {
//                binding.counter.setText(":00")
                cancel()
                isRecording = false
                binding.recorder.visibility = VISIBLE
            }
        }.start()
        var isVisible = true
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(500)
                if (isRecording) {
                    binding.recorder.visibility = if (isVisible)
                        INVISIBLE
                    else
                        VISIBLE
                    isVisible = !isVisible
                } else
                    break
            }
        }
    }

    private fun LayoutSignatureBinding.setupSignatureLayout() {
        root.isVisible = false
        paint.loadingDialog.shouldShowDialog(true)
        save.setOnClickListener {
            counter?.onFinish()
            paint.loadingDialog.shouldShowDialog(true)
            CoroutineScope(Dispatchers.IO).launch {
                cameraManager.stopRecording()
                withContext(Dispatchers.Main) {
                    binding.saveSignature()
                }
            }
        }
        clear.setOnClickListener {
            paint.clear()
        }
    }

    override fun onCameraGranted() {
        binding.waiting.root.isVisible = false
        getLocation()
    }

    override fun requestCameraPermissions() {
        if (!isFromStop)
            requestCameraPermission()
    }

    override fun onLocationPermissionGranted() {
        getLocation()
    }

    override fun onLocationPermissionDenied() {
//        Toast.makeText(requireContext(),"Cannot Access Location. Please Enable Permission and Make sure GPS is On",Toast.LENGTH_LONG).show()
        Log.e(TAG, "Cannot Access Location. Please Enable Permission")
        initializeCamera(null)
    }

    override fun onGPSTurnOnDenied() {
        Log.e(TAG, "Cannot Access Location. Please Make sure GPS is On")
        initializeCamera(null)
    }

    private fun initializeCamera(location: Location?) {
        binding.cameraRoot.isVisible = true
        cameraManager.setupCamera(
            binding.cameraView,
            requireContext(),
            viewLifecycleOwner,
            this,
            location,
            UUID.randomUUID().toString().substring(0, 13),
            getTime(),
        )
    }

    private fun startScreenRecordings() {
        binding.signatureLL.root.isVisible = true
        binding.signatureLL.paint.apply {
            viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    loadingDialog.shouldShowDialog(false)
                    cameraManager.startRecording(binding.cameraView, this@apply)
                    setupCounter()
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        }
    }

    private fun getLocation() {
        getCoordinates(
            onError = {
                when (it) {
                    GPSTracker.GPSSate.REQUESTING_PERMISSIONS, GPSTracker.GPSSate.REQUESTING_TURN_ON_GPS -> {
                        Log.d(TAG, "Requesting From GetLocation()")
                    }
                    GPSTracker.GPSSate.COORDINATE_ERROR -> {
                        Log.e(TAG, "Couldn't Get Coordinates")
                        initializeCamera(null)
                    }
                    else -> {
                        Log.e(TAG, "Couldn't Get Error State From GetLocation()")
                    }
                }
            }
        ).also {
            if (it != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    it.collectLatest { coordinates ->
                        initializeCamera(coordinates)
                    }
                }
            } else
                Log.e(TAG, "Error Getting Location. Check Error Message ..")
        }
    }

    private fun FragmentDrawSignatureBinding.saveSignature() {
        signatureLL.paint.loadBitmapFromView().also { signTemp ->
            if (signTemp != null) {
                saveRecording(signTemp)
            } else {
                signatureLL.paint.loadingDialog.shouldShowDialog(false)
                Log.e(TAG, "Error Generating Signature. View is Empty")
                Toast.makeText(
                    requireContext(),
                    "Please Draw Signature Before Saving",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun FragmentDrawSignatureBinding.saveRecording(
        signTemp: Bitmap
    ) {

        signatureLL.paint.loadingDialog.shouldShowDialog(false)
        cameraManager.getVideoPath().also { path ->
            Log.d(TAG, "Video Path ${path?.path}")
            onFinish()
            if (path != null) {
                signature.isVisible = true
                signature.setImageBitmap(signTemp)
                sendData(signTemp, path)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Couldn't Generate Video Recording",
                    Toast.LENGTH_LONG
                ).show()
                Log.e(TAG, "Recording Path is Null")
            }
        }
    }

    private fun getTime(): String {
        return SimpleDateFormat(
            "dd MMM yyyy HH:mm:ss",
            Locale.US
        ).format(Calendar.getInstance().time)
    }

    private fun sendData(signature: Bitmap, path: File) {
        val sendIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
        sendIntent.type = "video/*"
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        arrayListOf(
            getUriForFile(requireContext(), "com.example.realtimerecording.fileprovider", path),
//            getUriForFile(requireContext(), "com.example.realtimerecording.fileprovider", File(facePath))
        ).also {
            sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, it)
            startActivity(sendIntent)
        }
    }

    override fun onStop() {
        isFromStop = true
        onFinish()
        super.onStop()
    }

    private fun onFinish() {
        cameraManager.onDestroy()
        binding.apply {
            signatureLL.paint.onDestroy()
            signatureLL.root.isVisible = false
            cameraRoot.isVisible = false
        }
    }

    override fun onCameraStreaming(isStreaming: Boolean) {
        if (isStreaming)
            startScreenRecordings()
    }
}