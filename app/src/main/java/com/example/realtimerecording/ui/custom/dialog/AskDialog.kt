package com.example.realtimerecording.ui.custom.dialog

import android.app.Dialog
import android.content.Context
import android.util.Log
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.example.realtimerecording.R

object AskDialog {
    fun askPermissionDialog(
        context: Context,
        message: String,
        onSelection: (isConfirm: Boolean) -> Unit
    ) {
        setup(
            context,
            message,
            0,
            context.getString(R.string.grant),
            context.getString(R.string.cancel),
            onSelection
        )
    }

    fun setup(
        context: Context,
        message: String,
        baseImage: Int,
        yesButton: String,
        cancelButton: String,
        onSelection: (isConfirm: Boolean) -> Unit
    ): Dialog {
        val dialog = createDialog(context, R.layout.dialog_ask)
        dialog.findViewById<AppCompatTextView>(R.id.confirmBtn).text = yesButton
        dialog.findViewById<AppCompatTextView>(R.id.cancelConfirm).text = cancelButton
        dialog.findViewById<AppCompatTextView>(R.id.textView6).text = message
        dialog.findViewById<AppCompatImageView>(R.id.confirmImage).setImageResource(baseImage)
        dialog.findViewById<AppCompatTextView>(R.id.cancelConfirm).setOnClickListener {
            dialog.dismiss()
        }
        var isConfirmed=false
        dialog.findViewById<AppCompatTextView>(R.id.confirmBtn).setOnClickListener {
            dialog.dismiss()
            isConfirmed=true
            onSelection.invoke(true)
        }
        dialog.findViewById<AppCompatImageView>(R.id.closeDialog).setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            Log.d("Dialog State", "dissmiss")
            if (!isConfirmed)
                onSelection.invoke(false)
        }
        return dialog
    }
}