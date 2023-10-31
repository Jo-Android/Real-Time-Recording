package com.example.realtimerecording.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.example.realtimerecording.ui.custom.dialog.LoadingDialog
import kotlin.math.roundToInt


class Paint_View(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val path = Path()
    private val brush = Paint()

    private var xCoordinates = ArrayList<Float>()
    private var yCoordinates = ArrayList<Float>()

    private val TAG by lazy {
        Paint_View::class.java.simpleName
    }

    val loadingDialog by lazy { LoadingDialog(context) }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val printX = event.x
        val printY = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (printX > 0 && printY > 0)
                    path.moveTo(printX, printY)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (printX>0&&printY>0){
                    path.lineTo(printX, printY)
                    xCoordinates.add(printX)
                    yCoordinates.add(printY)
                }
            }
            else -> return false
        }
        postInvalidate()
        return false
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawPath(path, brush)
    }

    init {
        brush.isAntiAlias = true
        brush.color = Color.BLACK
        brush.style = Paint.Style.STROKE
        brush.strokeJoin = Paint.Join.ROUND
        brush.strokeWidth = 8f
    }

    private var screenshot: Bitmap? = null
    private var canvas: Canvas? = null
    fun takeScreenshot(): Bitmap {
        screenshot = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        canvas = Canvas(screenshot!!)
        draw(canvas)
        return screenshot!!
    }

    fun loadBitmapFromView(): Bitmap? {
        getBoundingMargin().also {
            Log.e(TAG, "Coordinates ${it?.left}, ${it?.top}, ${it?.right}, ${it?.bottom}")
            return if (it != null) {
                val b = Bitmap.createBitmap(
                    ((it.left + it.right) + 40).roundToInt(),
                    ((it.top + it.bottom) + 40).roundToInt(),
                    Bitmap.Config.ARGB_8888
                )
                canvas = Canvas(b)
                setBackgroundColor(Color.TRANSPARENT)
                draw(canvas)
                setBackgroundColor(Color.WHITE)
                b
            } else
                null
        }
    }


    private fun getBoundingMargin(): RectF? {
        return if (xCoordinates.isNotEmpty() && yCoordinates.isNotEmpty()) {
            var left = xCoordinates[0]
            var right = 0f
            var top = yCoordinates[0]
            var bottom = 0f

            for (x in xCoordinates) {
                if (x < left)
                    left = x
                else if (x > right)
                    right = x
            }

            for (y in yCoordinates) {
                if (y < top)
                    top = y
                else if (y > bottom)
                    bottom = y
            }
            RectF(left, top, right, bottom)
        } else
            null
    }

    fun clear() {
        path.reset()
        invalidate()
        xCoordinates = ArrayList()
        yCoordinates = ArrayList()
    }

    fun onDestroy() {
        clear()
    }
}
