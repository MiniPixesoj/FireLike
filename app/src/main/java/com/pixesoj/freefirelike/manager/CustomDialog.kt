package com.pixesoj.freefirelike.manager

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.pixesoj.freefirelike.R
import java.util.Locale

class CustomDialog(private val activity: Activity, dialogType: DialogType) {
    private val context: Context = activity
    var dialog: Dialog? = null
    private val dialogType: DialogType
    private var dialogView: View? = null

    init {
        this.dialogType = dialogType

        if (dialogType == DialogType.BOTTOM_SHEET) {
            this.dialog = BottomSheetDialog(context, R.style.BottomSheetDialogTheme_Transparent)
        } else {
            this.dialog = Dialog(context, R.style.AlertDialogTheme)
        }
    }

    class Builder(private val activity: Activity) {
        private var layoutResId = 0
        private var cancelable = true
        private var roundedCorners = -1
        private var onDismissListener: DialogInterface.OnDismissListener? = null
        private val buttons: MutableList<DialogButton> = ArrayList()
        private var dialogType = DialogType.BOTTOM_SHEET
        private var buttonsOrientation = LinearLayout.HORIZONTAL
        private var buttonsGravity = Gravity.CENTER

        fun setDialogType(type: DialogType): Builder {
            this.dialogType = type
            return this
        }

        fun setDesign(@LayoutRes layoutResId: Int): Builder {
            this.layoutResId = layoutResId
            return this
        }

        fun setCancelable(cancelable: Boolean): Builder {
            this.cancelable = cancelable
            return this
        }

        fun setRoundedCorners(radiusDp: Int): Builder {
            this.roundedCorners = radiusDp
            return this
        }

        fun setOnDismissListener(listener: DialogInterface.OnDismissListener?): Builder {
            this.onDismissListener = listener
            return this
        }

        fun setButtonsOrientation(orientation: Int): Builder {
            this.buttonsOrientation = orientation
            return this
        }

        fun setButtonsGravity(gravity: Int): Builder {
            this.buttonsGravity = gravity
            return this
        }

        fun addCustomButton(
            text: String,
            @DrawableRes backgroundRes: Int,
            textColor: Int,
            @DrawableRes iconLeftRes: Int,
            textAllCaps: Boolean,
            textSizeSp: Float,
            textStyle: Int,
            listener: View.OnClickListener?
        ) {
            buttons.add(
                DialogButton(
                    text,
                    backgroundRes,
                    textColor,
                    iconLeftRes,
                    textAllCaps,
                    textSizeSp,
                    textStyle,
                    listener
                )
            )
        }

        fun addButton(
            type: String?,
            text: String,
            stuffed: Boolean,
            listener: View.OnClickListener?
        ): Builder { // <-- Agrega este tipo de retorno
            var backgroundRes = 0
            var textColor = 0
            val iconLeftRes = 0
            var textStyle = Typeface.NORMAL
            var textSizeSp = 0f
            var textAllCaps = false

            when (type) {
                "ERROR" -> {
                    backgroundRes = if (stuffed) R.drawable.bg_btn_error else 0
                    textColor = if (stuffed) ContextCompat.getColor(activity, R.color.textColor)
                    else ContextCompat.getColor(activity, R.color.darkRed)
                    textAllCaps = true
                    textSizeSp = if (stuffed) 15f else 16f
                    textStyle = Typeface.BOLD
                }

                "SUCCESS" -> {
                    backgroundRes = if (stuffed) R.drawable.bg_btn_success else 0
                    textColor = if (stuffed) ContextCompat.getColor(activity, R.color.textColor)
                    else ContextCompat.getColor(activity, R.color.green)
                    textAllCaps = true
                    textSizeSp = if (stuffed) 15f else 16f
                    textStyle = Typeface.BOLD
                }

                "INFO" -> {
                    backgroundRes = if (stuffed) R.drawable.bg_btn_info else 0
                    textColor = if (stuffed) ContextCompat.getColor(activity, R.color.textColor)
                    else ContextCompat.getColor(activity, R.color.link)
                    textAllCaps = true
                    textSizeSp = if (stuffed) 15f else 16f
                    textStyle = Typeface.BOLD
                }
            }

            buttons.add(
                DialogButton(
                    text,
                    backgroundRes,
                    textColor,
                    iconLeftRes,
                    textAllCaps,
                    textSizeSp,
                    textStyle,
                    listener
                )
            )

            return this
        }

        fun build(): CustomDialog {
            val customDialog = CustomDialog(activity, dialogType)
            customDialog.setDesign(layoutResId)
            customDialog.setCancelable(cancelable)

            if (roundedCorners >= 0) {
                customDialog.setRoundedCorners(roundedCorners)
            }

            if (onDismissListener != null) {
                customDialog.setOnDismissListener(onDismissListener)
            }

            if (!buttons.isEmpty()) {
                val buttonLayout =
                    customDialog.findView<LinearLayout>(R.id.layout_dialog_custom_buttons)
                for ((text, backgroundRes, textColor, iconLeftRes, textAllCaps, textSizeSp, textStyle, listener) in buttons) {
                    val button = TextView(activity)
                    val params = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            40f,
                            activity.resources.displayMetrics
                        ).toInt()
                    )
                    buttonLayout.orientation = buttonsOrientation
                    buttonLayout.gravity = buttonsGravity
                    params.setMargins(8, 8, 8, 8)
                    button.layoutParams = params
                    button.text = if (textAllCaps) text.uppercase(Locale.getDefault()) else text
                    button.setTextColor(textColor)
                    button.setBackgroundResource(backgroundRes)
                    button.gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
                    button.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
                    button.setTypeface(null, textStyle)
                    button.setPadding(32, 0, 32, 0)
                    button.isFocusable = true
                    button.isClickable = true
                    if (iconLeftRes != 0) {
                        val icon = ContextCompat.getDrawable(activity, iconLeftRes)
                        if (icon != null) {
                            icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
                            button.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
                            button.compoundDrawablePadding = 12
                        }
                    }
                    button.setOnClickListener(listener)
                    buttonLayout.addView(button)
                }
            }
            return customDialog
        }
    }

    data class DialogButton(
        val text: String,
        @field:DrawableRes @param:DrawableRes val backgroundRes: Int,
        val textColor: Int,
        @field:DrawableRes @param:DrawableRes val iconLeftRes: Int,
        val textAllCaps: Boolean,
        val textSizeSp: Float,
        val textStyle: Int,
        val listener: View.OnClickListener?
    )

    enum class DialogType {
        BOTTOM_SHEET,
        CENTER_DIALOG
    }

    fun setDesign(@LayoutRes layoutResId: Int = R.layout.dialog_custom_v2) {
        val view = LayoutInflater.from(context).inflate(layoutResId, null)
        dialogView = view
        dialog?.setContentView(view)

        val linearLayoutMain = view.findViewById<LinearLayout>(R.id.linear_layout_dialog_custom_main)
        linearLayoutMain?.background = ContextCompat.getDrawable(
            context,
            if (dialogType == DialogType.BOTTOM_SHEET) R.drawable.bg_dialog_bottom
            else R.drawable.bg_dialog_alert
        )
    }

    fun <T : View?> findView(id: Int): T {
        checkNotNull(dialogView) { "You must call setDesign() first." }
        return dialogView!!.findViewById(id)
    }

    fun setOnDismissListener(listener: DialogInterface.OnDismissListener?) {
        dialog!!.setOnDismissListener(listener)
    }

    fun setCancelable(cancelable: Boolean) {
        dialog!!.setCancelable(cancelable)
    }

    fun setRoundedCorners(radiusDp: Int) {
        if (dialogView != null) {
            dialogView!!.background = object : GradientDrawable() {
                init {
                    setColor(Color.WHITE)
                    cornerRadius = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        radiusDp.toFloat(),
                        context.resources.displayMetrics
                    )
                }
            }
        }
    }

    fun show() {
        register(dialog)
        if (!activity.isFinishing) {
            dialog!!.show()
        }
    }

    fun dismiss() {
        if (dialog!!.isShowing) dialog!!.dismiss()
        unregister(dialog)
    }

    companion object {
        private val activeDialogs: MutableList<Dialog?> = ArrayList()

        fun register(dialog: Dialog?) {
            dismissAll()
            activeDialogs.add(dialog)
        }

        fun dismissAll() {
            for (d in activeDialogs) {
                if (d != null && d.isShowing) {
                    try {
                        d.dismiss()
                    } catch (ignored: IllegalArgumentException) {
                    }
                }
            }
            activeDialogs.clear()
        }

        fun unregister(dialog: Dialog?) {
            activeDialogs.remove(dialog)
        }
    }
}
