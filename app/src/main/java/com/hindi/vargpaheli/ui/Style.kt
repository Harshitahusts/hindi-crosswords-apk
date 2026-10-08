package com.hindi.vargpaheli.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView

/** Newspaper palette: paper, ink, one green. */
object Style {
    val PAPER = Color.parseColor("#FBF8F1")
    val INK = Color.BLACK
    val GREEN = Color.parseColor("#1B6B2F")
    val GREEN_LIGHT = Color.parseColor("#D3EBD5")
    val WORD = Color.parseColor("#FFF0B3")
    val CURSOR = Color.parseColor("#F5C842")
    val GREY = Color.parseColor("#555555")

    fun dp(ctx: Context, v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, ctx.resources.displayMetrics)

    fun box(ctx: Context, fill: Int, strokeDp: Float = 1f) = GradientDrawable().apply {
        setColor(fill); setStroke(dp(ctx, strokeDp).toInt().coerceAtLeast(1), INK)
    }

    fun button(ctx: Context, text: String, sizeSp: Float = 18f) = TextView(ctx).apply {
        this.text = text
        setTextColor(INK)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        gravity = Gravity.CENTER
        val p = dp(ctx, 8f).toInt()
        setPadding(p, p / 2, p, p / 2)
        background = box(ctx, Color.WHITE)
        isClickable = true
    }

    fun text(ctx: Context, text: String, sizeSp: Float, bold: Boolean = false, color: Int = INK) = TextView(ctx).apply {
        this.text = text
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }
}
