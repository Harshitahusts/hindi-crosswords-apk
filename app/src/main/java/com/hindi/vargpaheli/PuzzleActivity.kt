package com.hindi.vargpaheli

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.hindi.vargpaheli.engine.CrosswordEngine
import com.hindi.vargpaheli.model.PuzzleRepository
import com.hindi.vargpaheli.model.Word
import com.hindi.vargpaheli.storage.ProgressManager
import com.hindi.vargpaheli.ui.CrosswordGrid
import com.hindi.vargpaheli.ui.HindiKeyboard
import com.hindi.vargpaheli.ui.Style

/** One crossword: grid, numbered clue list, current clue and the Hindi keyboard. */
class PuzzleActivity : Activity() {
    private lateinit var engine: CrosswordEngine
    private lateinit var progress: ProgressManager
    private lateinit var grid: CrosswordGrid
    private lateinit var clueBar: TextView
    private lateinit var scroll: ScrollView
    private val clueViews = mutableMapOf<Word, TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progress = ProgressManager(this)
        val puzzle = PuzzleRepository.get(this, intent.getIntExtra(EXTRA_ID, 1))
        engine = CrosswordEngine(puzzle, CrosswordEngine.deserialize(progress.cells(puzzle.id), puzzle.size))
        progress.position(puzzle.id)?.let { (r, c, a) -> engine.restorePosition(r, c, a) }

        val dp = { v: Float -> Style.dp(this, v).toInt() }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Style.PAPER) }

        // Top bar: ← वापस   title
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8f), dp(6f), dp(8f), dp(6f)) }
        top.addView(Style.button(this, "← वापस", 18f).apply { setOnClickListener { finish() } })
        top.addView(Style.text(this, puzzle.title, 22f, bold = true).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(top)

        // Scrolling part: grid + all clues
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8f), 0, dp(8f), dp(8f)) }
        grid = CrosswordGrid(this, engine) { refresh(scrollToGrid = false) }
        body.addView(grid, LinearLayout.LayoutParams(-1, -2))
        for (across in listOf(true, false)) {
            body.addView(Style.text(this, if (across) "बाएँ से दाएँ →" else "ऊपर से नीचे ↓", 20f, bold = true).apply {
                setPadding(0, dp(12f), 0, dp(4f))
            })
            puzzle.words.filter { it.across == across }.forEach { w ->
                val tv = Style.text(this, "", 18f).apply {
                    setPadding(dp(6f), dp(5f), dp(6f), dp(5f))
                    setOnClickListener { engine.selectWord(w); refresh(scrollToGrid = true) }
                }
                clueViews[w] = tv
                body.addView(tv, LinearLayout.LayoutParams(-1, -2))
            }
        }
        scroll = ScrollView(this).apply { addView(body) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        // Current clue: ◀ [12 बाएँ से दाएँ: ... (4)] ▶
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; background = Style.box(context, Color.WHITE) }
        bar.addView(Style.button(this, "◀", 20f).apply { setOnClickListener { engine.nextWord(-1); refresh(true) } },
            LinearLayout.LayoutParams(dp(48f), -1))
        clueBar = Style.text(this, "", 18f).apply {
            gravity = Gravity.CENTER_VERTICAL; maxLines = 3; setPadding(dp(8f), dp(4f), dp(8f), dp(4f))
        }
        bar.addView(clueBar, LinearLayout.LayoutParams(0, -2, 1f))
        bar.addView(Style.button(this, "▶", 20f).apply { setOnClickListener { engine.nextWord(1); refresh(true) } },
            LinearLayout.LayoutParams(dp(48f), -1))
        root.addView(bar, LinearLayout.LayoutParams(-1, -2).apply { minimumHeight = dp(56f) })

        // Keyboard height adapts to the screen so the grid stays visible on small phones.
        val hDp = resources.displayMetrics.heightPixels / resources.displayMetrics.density
        val keyH = (hDp * 0.052f).coerceIn(36f, 50f)
        root.addView(HindiKeyboard(this, keyH,
            onKey = { k -> afterInput(engine.input(k)) },
            onDelete = { engine.delete(); afterInput(false) },
            onReveal = { afterInput(engine.revealCell()) }))

        setContentView(root)
        refresh(false)
    }

    private fun afterInput(solvedWord: Boolean) {
        save()
        refresh(scrollToGrid = false)
        if (solvedWord && engine.isComplete()) onComplete()
    }

    private fun refresh(scrollToGrid: Boolean) {
        grid.invalidate()
        val cur = engine.currentWord
        clueBar.text = "${cur.number} ${if (cur.across) "बाएँ से दाएँ" else "ऊपर से नीचे"}: ${cur.clue} (${cur.length})" +
            if (engine.isSolved(cur)) "  ✓" else ""
        clueViews.forEach { (w, tv) ->
            val solved = engine.isSolved(w)
            tv.text = "${w.number}. ${w.clue} (${w.length})" + if (solved) "  ✓" else ""
            tv.setTextColor(if (solved) Style.GREEN else Style.INK)
            tv.setBackgroundColor(if (w == cur) Style.WORD else Color.TRANSPARENT)
        }
        if (scrollToGrid) scroll.smoothScrollTo(0, 0)
        save()
    }

    private fun save() = progress.save(engine.puzzle.id, engine.serialize(), engine.row, engine.col, engine.across)

    private fun onComplete() {
        val id = engine.puzzle.id
        progress.markCompleted(id)
        val b = AlertDialog.Builder(this)
            .setTitle("बहुत बढ़िया!")
            .setMessage("वर्ग पहेली पूरी हो गई।")
            .setNegativeButton("वापस") { _, _ -> finish() }
        if (id < MainActivity.TOTAL) b.setPositiveButton("अगली पहेली") { _, _ ->
            startActivity(intent.putExtra(EXTRA_ID, id + 1)); finish()
        }
        b.setCancelable(false).show()
    }

    override fun onPause() {
        super.onPause()
        save()
    }

    companion object {
        const val EXTRA_ID = "id"
    }
}
