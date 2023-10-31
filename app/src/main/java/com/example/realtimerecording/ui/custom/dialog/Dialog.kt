package com.example.realtimerecording.ui.custom.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Insets
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.DisplayMetrics
import android.util.Size
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.widget.LinearLayout
import com.example.realtimerecording.R

fun createFullDialog(context: Context, layoutId: Int): Dialog {
    val dialog = Dialog(context, R.style.DialogTheme)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setContentView(layoutId)
    dialog.show()
    return dialog
}

fun createDialog(context: Context, layoutId: Int): Dialog {
    val dialog = Dialog(context)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.window!!.decorView.setBackgroundResource(R.drawable.background_area_selection)
    dialog.window!!.decorView.setPadding(0, 0, 0, 0)
    dialog.window!!.attributes.windowAnimations = R.style.DialogSlide1

    if (context is Activity) {
        dialog.window!!.attributes.height =
            LinearLayout.LayoutParams.WRAP_CONTENT
        dialog.window!!.attributes.width =
            ((getWidth(context) - context.resources.getDimension(R.dimen.size_40)).toInt())
    }
    dialog.setContentView(layoutId)
    dialog.show()
    return dialog
}


@Suppress("DEPRECATION")
fun getWidth(activity: Activity): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = activity.windowManager.currentWindowMetrics
        val insets: Insets = windowMetrics.windowInsets
            .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
        windowMetrics.bounds.width() - insets.left - insets.right
    } else {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        displayMetrics.widthPixels
    }
}

@Suppress("DEPRECATION")
fun getDisplaySize(activity: Activity): Size {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = activity.windowManager.currentWindowMetrics
        Size(windowMetrics.bounds.width(), windowMetrics.bounds.height())
    } else {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        Size(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }
}

fun getDP(dip: Int, context: Context): Float {
    return (TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dip.toFloat(),
        context.resources.displayMetrics
    ))
}

fun createBottomDialog(context: Context, layoutId: Int): Dialog {
    val dialog = Dialog(context)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setContentView(layoutId)
    dialog.window!!.setLayout(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
    dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    dialog.window!!.setGravity(Gravity.BOTTOM)

    dialog.show()
    return dialog
}