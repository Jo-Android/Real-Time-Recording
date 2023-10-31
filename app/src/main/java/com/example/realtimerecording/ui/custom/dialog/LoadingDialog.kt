package com.example.realtimerecording.ui.custom.dialog

import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ProgressBar

class LoadingDialog(context: Context) :
    Dialog(context, android.R.style.Theme_DeviceDefault_Light_Dialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setCancelable(false)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window?.let {
            it.setBackgroundDrawable(ColorDrawable(0))
            it.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            val layoutParams = it.attributes
            layoutParams.dimAmount = 0.8f
            layoutParams.gravity = Gravity.CENTER
            it.attributes = layoutParams
        }

        setContentView(FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            addView(ProgressBar(context).apply {
                layoutParams = FrameLayout.LayoutParams(55.px, 55.px)
                isIndeterminate = true
            })
        })
    }

    fun shouldShowDialog(show: Boolean) {
        if (show) {
            show()
        } else {
            dismiss()
        }
    }

    val Int.px: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()
}