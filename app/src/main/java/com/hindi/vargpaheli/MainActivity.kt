package com.hindi.vargpaheli

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.hindi.vargpaheli.storage.ProgressManager
import com.hindi.vargpaheli.ui.Style

/** Home: the 100 numbered puzzles. Green = solved. */
class MainActivity : Activity() {
    private lateinit var progress: ProgressManager
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progress = ProgressManager(this)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val p = Style.dp(this, 16f).toInt()
        root.setPadding(p, p, p, p)
        setContentView(ScrollView(this).apply { setBackgroundColor(Style.PAPER); addView(root) })
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        root.removeAllViews()
        val done = progress.completed()
        root.addView(Style.text(this, "हिंदी वर्ग पहेली", 30f, bold = true).apply { gravity = Gravity.CENTER })
        root.addView(Style.text(this, "100 वर्ग पहेलियाँ  •  पूरी हुईं: ${done.size}", 18f, color = Style.GREY).apply {
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, Style.dp(context, 12f).toInt())
        }, LinearLayout.LayoutParams(-1, -2))

        val last = progress.lastLevel()
        if (last in 1..TOTAL && last !in done) {
            root.addView(Style.button(this, "जारी रखें — पहेली ${"%02d".format(last)}", 20f).apply {
                setOnClickListener { open(last) }
            }, LinearLayout.LayoutParams(-1, Style.dp(this, 52f).toInt()).apply { bottomMargin = Style.dp(this@MainActivity, 12f).toInt() })
        }

        val gap = Style.dp(this, 6f).toInt()
        for (start in 1..TOTAL step COLS) {
            val row = LinearLayout(this)
            for (id in start until start + COLS) {
                val solved = id in done
                val unlocked = progress.isUnlocked(id)
                val b = Style.button(this, "%02d".format(id), 22f).apply {
                    background = Style.box(context, if (solved) Style.GREEN else if (unlocked) Color.WHITE else Style.PAPER,
                        if (id == last) 2.5f else 1f)
                    setTextColor(if (solved) Color.WHITE else if (unlocked) Style.INK else Color.LTGRAY)
                    if (unlocked) setOnClickListener { open(id) }
                }
                row.addView(b, LinearLayout.LayoutParams(0, Style.dp(this, 56f).toInt(), 1f).apply { setMargins(gap, gap, gap, gap) })
            }
            root.addView(row)
        }
    }

    private fun open(id: Int) = startActivity(Intent(this, PuzzleActivity::class.java).putExtra(PuzzleActivity.EXTRA_ID, id))

    companion object {
        const val TOTAL = 100
        private const val COLS = 4
    }
}
