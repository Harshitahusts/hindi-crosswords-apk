package com.hindi.vargpaheli.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import com.hindi.vargpaheli.engine.CrosswordEngine

/** Square newspaper-style grid drawn on a Canvas. Width decides the box size. */
class CrosswordGrid(context: Context, private val engine: CrosswordEngine, private val onTap: () -> Unit) : View(context) {
    private val n = engine.puzzle.size
    private val fill = Paint()
    private val line = Paint().apply { color = Style.INK; style = Paint.Style.STROKE; strokeWidth = Style.dp(context, 1f) }
    private val border = Paint(line).apply { strokeWidth = Style.dp(context, 2.5f) }
    private val letter = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val number = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Style.INK }
    private val wrongMark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Style.GREY; style = Paint.Style.STROKE }

    override fun onMeasure(w: Int, h: Int) {
        val size = MeasureSpec.getSize(w)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        val pad = border.strokeWidth
        val cell = (width - 2 * pad) / n
        letter.textSize = cell * 0.48f
        number.textSize = cell * 0.24f
        wrongMark.strokeWidth = cell * 0.04f
        val word = engine.currentWord
        val wrongWord = engine.isFilledWrong(word)
        for (r in 0 until n) for (c in 0 until n) {
            val x = pad + c * cell
            val y = pad + r * cell
            val block = engine.puzzle.isBlock(r, c)
            val solved = !block && engine.isCellInSolvedWord(r, c)
            fill.color = when {
                block -> Style.INK
                r == engine.row && c == engine.col -> Style.CURSOR
                word.contains(r, c) -> Style.WORD
                solved -> Style.GREEN_LIGHT
                else -> Color.WHITE
            }
            canvas.drawRect(x, y, x + cell, y + cell, fill)
            canvas.drawRect(x, y, x + cell, y + cell, line)
            if (block) continue
            engine.puzzle.numbers[r][c].takeIf { it > 0 }?.let {
                canvas.drawText(it.toString(), x + cell * 0.06f, y + number.textSize, number)
            }
            val t = engine.entries[r][c]
            if (t.isNotEmpty()) {
                letter.color = if (solved) Style.GREEN else Style.INK
                val ty = y + cell * 0.58f - (letter.descent() + letter.ascent()) / 2 - cell * 0.08f
                canvas.drawText(t, x + cell / 2, ty, letter)
            }
            // Calm "not right yet" hint: a thin grey line under a fully filled wrong word.
            if (wrongWord && word.contains(r, c)) canvas.drawLine(x + cell * .15f, y + cell * .9f, x + cell * .85f, y + cell * .9f, wrongMark)
        }
        canvas.drawRect(pad / 2, pad / 2, width - pad / 2, height - pad / 2, border)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_UP) {
            val pad = border.strokeWidth
            val cell = (width - 2 * pad) / n
            val c = ((e.x - pad) / cell).toInt()
            val r = ((e.y - pad) / cell).toInt()
            if (r in 0 until n && c in 0 until n) { engine.tap(r, c); onTap() }
            performClick()
        }
        return true
    }

    override fun performClick() = super.performClick()
}
