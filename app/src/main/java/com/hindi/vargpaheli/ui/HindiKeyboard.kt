package com.hindi.vargpaheli.ui

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.hindi.vargpaheli.engine.CrosswordEngine

/** Built-in Hindi letter keyboard, so no Hindi keyboard needs to be installed. */
class HindiKeyboard(
    context: Context,
    keyHeightDp: Float,
    private val onKey: (String) -> Unit,
    private val onDelete: () -> Unit,
    private val onReveal: () -> Unit,
) : LinearLayout(context) {

    private val rows = listOf(
        "अ आ इ ई उ ऊ ए ऐ ओ औ ऋ",
        "क ख ग घ ङ च छ ज झ ञ",
        "ट ठ ड ढ ड़ ढ़ ण त थ द",
        "ध न प फ ब भ म य र ल",
        "व श ष स ह क्ष त्र ज्ञ श्र",
        "ा ि ी ु ू े ै ो ौ ृ",
    )

    init {
        orientation = VERTICAL
        setBackgroundColor(Style.PAPER)
        val h = Style.dp(context, keyHeightDp).toInt()
        rows.forEach { line -> addRow(h, line.split(" ").map { key(it) }) }
        addRow(h, listOf("ं", "ँ", "ः", "्").map { key(it) } +
            listOf(action("अक्षर दिखाएँ", 2.6f, onReveal), action("⌫ मिटाएँ", 2.4f, onDelete)))
    }

    private fun addRow(h: Int, keys: List<TextView>) {
        val row = LinearLayout(context).apply { orientation = HORIZONTAL }
        keys.forEach { row.addView(it) }
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, h))
    }

    private fun key(value: String): TextView {
        // Matras are shown on a dotted circle so they are easy to recognise: ◌ि
        val label = if (CrosswordEngine.isSign(value)) "◌$value" else value
        return cell(label, 1f, 20f) { onKey(value) }
    }

    private fun action(label: String, weight: Float, f: () -> Unit) = cell(label, weight, 15f, f)

    private fun cell(label: String, weight: Float, sp: Float, f: () -> Unit) = TextView(context).apply {
        text = label
        gravity = Gravity.CENTER
        setTextColor(Style.INK)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        includeFontPadding = false
        maxLines = 1
        background = Style.box(context, Color.WHITE, 0.5f)
        isSoundEffectsEnabled = true
        setOnClickListener { f() }
        layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, weight)
    }
}
