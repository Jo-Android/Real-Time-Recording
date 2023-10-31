package com.example.realtimerecording.manager.camera

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.realtimerecording.manager.opencv.VideoRecorder
import com.example.realtimerecording.manager.opencv.VideoRecorder.Companion.FRAME_PER_SECOND
import com.example.realtimerecording.ui.custom.Paint_View
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToLong

class CameraManager : KoinComponent {

    private var job2: Job? = null
    private var job: Job? = null
    private var savePath: File? = null
    private val TAG = CameraManager::class.java.simpleName

    private val videoRecorder by inject<VideoRecorder>()

    private var isRecording = false

    private var mCameraProvider: ProcessCameraProvider? = null

    private var camera: Camera? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var imageCaptureBuilder: ImageCapture.Builder? = null
    private var preview: Preview? = null
    private var previewBuilder: Preview.Builder? = null
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    fun setupCamera(
        view: PreviewView,
        context: Context,
        owner: LifecycleOwner,
        signatureInterface: SignatureInterface,
        location: Location?,
        uuid: String,
        time: String
    ) {
        savePath = File(context.cacheDir, "authentication.mp4")
        videoRecorder.setup(savePath!!)
        videoRecorder.generateInfo(context, location, uuid, time)
        if (::cameraExecutor.isInitialized)
            cameraExecutor.shutdown()
        cameraExecutor = Executors.newSingleThreadExecutor()
        view.post {
            // Bind all camera use cases
            view.bindCameraUseCases(context, owner, signatureInterface)
        }
    }

    private fun PreviewView.bindCameraUseCases(
        context: Context,
        owner: LifecycleOwner,
        signatureInterface: SignatureInterface
    ) {
        val rotation = display.rotation

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({

            val cameraProvider = cameraProviderFuture.get()
            mCameraProvider = cameraProvider

            previewBuilder = Preview.Builder()
                .setTargetRotation(rotation)

            preview = previewBuilder?.build()

            preview?.setSurfaceProvider(surfaceProvider)

            // ImageCapture
            imageCaptureBuilder = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(rotation)

            imageCapture = imageCaptureBuilder?.build()

            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetRotation(rotation)
                .build()
            cameraProvider.unbindAll()

            try {
                camera =
                    cameraProvider.bindToLifecycle(owner, cameraSelector, preview, imageCapture)
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                previewStreamState.observe(owner) {
                    Log.d(TAG, "Camera PreviewView State $it")
                    (it == PreviewView.StreamState.STREAMING).apply {
                        signatureInterface.onCameraStreaming(this)
                    }
                }
            } catch (exception: Exception) {
                Log.e("TAG", "Use case binding failed: ${exception.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }


    fun startRecording(view: PreviewView, paintView: Paint_View) {
        if (!isRecording) {
            Log.d(TAG, "Recording Started")
            frames = ArrayList()
            job = CoroutineScope(Dispatchers.IO).launch {
                isRecording = true
                record(view, paintView)
            }
            job2 = CoroutineScope(Dispatchers.IO).launch {
                addFrame()
            }
        } else
            Log.w(TAG, "Recording already Running")
    }

    private var frames = ArrayList<GenertaeFrame>()

    data class GenertaeFrame(val camera: Bitmap, val signature: Bitmap)

    private lateinit var frame: GenertaeFrame

    private var job3: Job? = null

    private var startTime = 0L
    private var endTime = 0L
    private var difference = 0L

    private suspend fun record(view: PreviewView, paintView: Paint_View) {
        job3 = CoroutineScope(Dispatchers.Main).launch {
            startTime = System.currentTimeMillis()
            if (view.bitmap != null) {
                Log.d(TAG, "Adding Frame")
                frame = GenertaeFrame(view.bitmap!!, paintView.takeScreenshot())
                frames.add(frame)
            }
        }
        job3!!.join()
        job3!!.cancel()
        endTime = System.currentTimeMillis()
        difference = endTime - startTime
        difference = ((1000 / FRAME_PER_SECOND) - difference).roundToLong()

        Log.d("TAG", "Start Time $startTime End Time $endTime  Should wait $difference ")

        if (difference > 0)
            delay(difference)
        else
            delay((10).toLong())
        if (isRecording)
            record(view, paintView)
    }

    private fun addFrame() {
        var count = 0
        while (true) {
            if (count < frames.size) {
                Log.d(TAG, "Adding Frame To Recorder ")
                videoRecorder.addFrame(frames[count].camera, frames[count].signature)
                count++
            } else if (!isRecording) {
                Log.d(TAG, "Stopping addFrame() -> Size ${frames.size}")
                break
            }
        }
    }

    suspend fun stopRecording() {
        if (isRecording) {
            Log.d(TAG, "Stopping Recording Frames Size ${frames.size}")
            isRecording = false
            withContext(Dispatchers.Main) {
                mCameraProvider?.unbindAll()
                mCameraProvider = null
            }
            job?.cancel()
            job2?.join()
            videoRecorder.stopRecording()
            job2?.cancel()
            job = null
            job2 = null

        } else
            Log.w(TAG, "Recording Already Stopped")
    }

    fun getVideoPath(): File? {
        return savePath
    }

    fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        savePath = null
        if (isRecording) {
            CoroutineScope(Dispatchers.IO).launch {
                stopRecording()
            }
        }
    }
}