package com.pixesoj.freefirelike.builder

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

class TextSpanBuilder(private val context: Context) {
    private val builder = SpannableStringBuilder()

    fun append(text: String?, style: Int, sizeScale: Float, color: Any?): TextSpanBuilder {
        val start = builder.length
        builder.append(text)

        if (style != Typeface.NORMAL) {
            builder.setSpan(
                StyleSpan(style),
                start,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        if (sizeScale != 1.0f) {
            builder.setSpan(
                RelativeSizeSpan(sizeScale),
                start,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        if (color != null) {
            val colorValue = if (color is Int && color > 0xFFFFFF) ContextCompat.getColor(
                context,
                color
            ) else color as Int

            builder.setSpan(
                ForegroundColorSpan(colorValue),
                start,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return this
    }

    fun appendUnderline(text: String?): TextSpanBuilder {
        val start = builder.length
        builder.append(text)
        builder.setSpan(UnderlineSpan(), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return this
    }

    fun appendStrikethrough(text: String?): TextSpanBuilder {
        val start = builder.length
        builder.append(text)
        builder.setSpan(
            StrikethroughSpan(),
            start,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return this
    }

    fun appendClickable(
        text: String?,
        listener: View.OnClickListener,
        underline: Boolean
    ): TextSpanBuilder {
        val start = builder.length
        builder.append(text)
        val clickable: ClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                listener.onClick(widget)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = underline
                ds.color = ds.linkColor
            }
        }
        builder.setSpan(clickable, start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return this
    }

    fun appendImage(@DrawableRes drawableRes: Int, widthDp: Int, heightDp: Int): TextSpanBuilder {
        val drawable = ContextCompat.getDrawable(context, drawableRes)
        if (drawable != null) {
            val widthPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, widthDp.toFloat(), context.resources.displayMetrics
            ).toInt()
            val heightPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, heightDp.toFloat(), context.resources.displayMetrics
            ).toInt()

            drawable.setBounds(0, 0, widthPx, heightPx)
            val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM)
            val start = builder.length
            builder.append(" ")
            builder.setSpan(imageSpan, start, start + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return this
    }

    fun build(): SpannableStringBuilder {
        return builder
    }
}