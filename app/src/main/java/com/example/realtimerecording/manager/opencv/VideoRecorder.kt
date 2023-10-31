package com.example.realtimerecording.manager.opencv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import android.util.Log
import android.util.TypedValue
import org.bytedeco.javacpp.avcodec
import org.bytedeco.javacpp.avutil
import org.bytedeco.javacpp.opencv_core
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.FrameRecorder
import org.bytedeco.javacv.OpenCVFrameConverter
import java.io.File
import kotlin.math.roundToInt

class VideoRecorder {

    companion object {
        const val FRAME_PER_SECOND = 15.0
    }

    private var isRecording: Boolean = false
    private val TAG = VideoRecorder::class.java.simpleName

    private var recorder: FFmpegFrameRecorder? = null

    private val converter by lazy { OpenCVFrameConverter.ToIplImage() }


    fun setup(savePath: File) {
        Log.i(
            TAG,
            "saved file path: " + savePath.absolutePath
        )
        System.setProperty("org.bytedeco.javacpp.maxphysicalbytes", "0")
        System.setProperty("org.bytedeco.javacpp.maxbytes", "0")
        recorder = FFmpegFrameRecorder(savePath, 1200, 1840, 0)
        recorder!!.format = "mp4"
        recorder!!.videoCodec = avcodec.AV_CODEC_ID_MPEG4
        // Set in the surface changed method
        recorder!!.frameRate = FRAME_PER_SECOND
        recorder!!.videoBitrate = 1200
        recorder!!.pixelFormat = avutil.AV_PIX_FMT_YUV420P
        Log.i(
            TAG,
            "recorder initialize success"
        )
        startRecording()
    }

    private fun startRecording() {
        if (recorder != null) {
            try {
                recorder!!.start()
                isRecording = true
                Log.d(TAG, "Recording Started")
            } catch (e: FrameRecorder.Exception) {
                e.printStackTrace()
            }
        } else
            Log.e(TAG, "Couldn't Start Recording. Recorder is Null")
    }

    fun addFrame(face: Bitmap?, signature: Bitmap) {
        if (recorder != null && face != null && infoBitmap != null) {
            try {
//                CoroutineScope(Dispatchers.IO).launch {
                mergeAll(signature, face, infoBitmap!!).also { ipl ->
                    Log.i(TAG, "Merging Frame")
                    if (ipl != null) {
                        converter.convert(ipl).also {
                            recorder!!.record(it)
                        }
                    } else
                        Log.e(TAG, "Error Generating Frame from Bitmap")
                }
//                }
            } catch (e: java.lang.Exception) {
                Log.e(TAG, "Error ADding Frame :")
                e.printStackTrace()
            }
        } else
            Log.e(TAG, "Couldn't Add Frame! Recorder $recorder Frame $face Info $infoBitmap")
    }

    fun stopRecording() {
//        CoroutineScope(Dispatchers.IO).launch {
            if (recorder != null && isRecording) {
                isRecording = false
                Log.v(
                    TAG,
                    "Finishing recording, calling stop and release on recorder"
                )
                try {
                    recorder!!.stop()
                    recorder!!.release()
                } catch (e: FrameRecorder.Exception) {
                    Log.e(TAG, "An Error Occurred While Stopping Recorder")
                    e.printStackTrace()
                }
                recorder = null
//            }
            }
    }


    private var infoBitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private var mergedBitmap: Bitmap? = null


    fun generateInfo(
        context: Context,
        location: Location?,
        uuid: String,
        currentTime: String
    ) {
        val textPaint = Paint()
        textPaint.style = Paint.Style.FILL
        textPaint.color = Color.BLACK
        val textSize = getDP(12, context)
        textPaint.textSize = textSize

        infoBitmap = Bitmap.createBitmap(
            (textPaint.measureText(
                getGreatestText(
                    "   ${location?.latitude},${location?.longitude}",
                    "   $uuid",
                    "   $currentTime"
                )
            ) + textSize).roundToInt(),
            ((textSize * 10) + 5 * getDP(
                2,
                context
            )).roundToInt(),// 3 lines + margins between each line
            Bitmap.Config.RGB_565
        )
        canvas = Canvas(infoBitmap!!)
        canvas!!.drawColor(Color.WHITE)
        canvas!!.drawText("Location:", textSize, textSize * 2, textPaint)
        canvas!!.drawText(
            "   ${location?.latitude},${location?.longitude}",
            textSize,
            textSize * 3 + getDP(2, context),
            textPaint
        )
        canvas!!.drawText("Id:", textSize, textSize * 5 + getDP(2, context), textPaint)
        canvas!!.drawText("   $uuid", textSize, textSize * 6 + getDP(2, context), textPaint)
        canvas!!.drawText("Time:", textSize, textSize * 8 + getDP(2, context), textPaint)
        canvas!!.drawText(
            "   $currentTime",
            textSize,
            textSize * 9 + getDP(2, context),
            textPaint
        )
    }

    private fun mergeAll(
        signature: Bitmap,
        camera: Bitmap,
        infoBitmap: Bitmap
    ): opencv_core.IplImage? {
        mergedBitmap = Bitmap.createBitmap(1200, 1840, Bitmap.Config.ARGB_8888)
        return if (mergedBitmap != null) {
            canvas = Canvas(mergedBitmap!!)
            canvas!!.drawColor(Color.WHITE)
            canvas!!.drawBitmap(camera, 0f, 0f, null)
            canvas!!.drawBitmap(infoBitmap, camera.width.toFloat(), 0f, null)
            canvas!!.drawBitmap(
                signature,
                0f,
                getFrameBiggestSize(camera.height, infoBitmap.height).toFloat(),
                null
            )
            mergedBitmap!!.toIplImage()
        } else
            null

    }

    private var container: opencv_core.IplImage? = null
    private fun Bitmap.toIplImage(): opencv_core.IplImage? {
        container = opencv_core.IplImage.create(width, height, opencv_core.IPL_DEPTH_8U, 4)
        copyPixelsToBuffer(container!!.byteBuffer)
        return container
    }

    private fun getFinalWidth(camera: Int, signature: Int, info: Int): Int {
        (getFrameSmallestSize(signature, camera) + info).also {
            return if (signature < it)
                signature + (it - signature)
            else
                signature
        }
    }

    private fun getFrameSmallestSize(width: Int, width1: Int): Int {
        return if (width < width1)
            width
        else
            width1
    }

    private fun getFrameBiggestSize(signature: Int, camera: Int): Int {
        return if (signature > camera)
            signature
        else
            camera
    }

    private fun getGreatestText(location: String, uuid: String, time: String): String {
        return (if (location.length >= uuid.length && location.length >= time.length)
            location
        else if (uuid.length >= location.length && uuid.length >= time.length)
            uuid
        else
            time).also {
            Log.e(TAG, "Greatest Text $it")
        }
    }

    private fun getDP(dip: Int, context: Context): Float {
        return (TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dip.toFloat(),
            context.resources.displayMetrics
        )) * 1.05f
    }
}