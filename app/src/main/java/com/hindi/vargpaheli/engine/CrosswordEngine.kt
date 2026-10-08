package com.hindi.vargpaheli.engine

import com.hindi.vargpaheli.model.Puzzle
import com.hindi.vargpaheli.model.Word
import java.text.Normalizer

/**
 * Crossword state and input rules, independent of the UI.
 *
 * Typing rules (no Unicode knowledge needed):
 *  - a letter (क, अ, क्ष ...) fills the current box; if the box already has a full akshara
 *    the cursor moves to the next box of the word first.
 *  - a matra / ं / ् is added to the current box (क + ि = कि).
 *  - after ् the next letter joins the same box (क + ् + र = क्र).
 */
class CrosswordEngine(val puzzle: Puzzle, val entries: Array<Array<String>>) {
    var row = 0; private set
    var col = 0; private set
    var across = true; private set
    /** True right after the user taps a box: the next letter replaces its content. */
    private var fresh = true

    init {
        val w = puzzle.words.first()
        row = w.row; col = w.col; across = w.across
    }

    val currentWord: Word
        get() = puzzle.wordAt(row, col, across) ?: puzzle.wordAt(row, col, !across)!!

    fun restorePosition(r: Int, c: Int, a: Boolean) {
        if (r in 0 until puzzle.size && c in 0 until puzzle.size && !puzzle.isBlock(r, c)) {
            row = r; col = c
            across = if (puzzle.wordAt(r, c, a) != null) a else !a
        }
    }

    /** Tap on a box. Tapping the selected box again switches across/down. */
    fun tap(r: Int, c: Int) {
        if (puzzle.isBlock(r, c)) return
        if (r == row && c == col) {
            if (puzzle.wordAt(r, c, !across) != null) across = !across
        } else {
            row = r; col = c
            if (puzzle.wordAt(r, c, across) == null) across = !across
        }
        fresh = true
    }

    fun selectWord(w: Word) {
        across = w.across
        val (r, c) = w.cells().firstOrNull { entries[it.first][it.second].isEmpty() } ?: (w.row to w.col)
        row = r; col = c; fresh = true
    }

    fun nextWord(step: Int = 1) {
        val ws = puzzle.words
        selectWord(ws[(ws.indexOf(currentWord) + step + ws.size) % ws.size])
    }

    /** Returns true if this input solved a word. */
    fun input(key: String): Boolean {
        val wasSolved = currentWord.let(::isSolved)
        val cur = entries[row][col]
        if (isSign(key)) {
            if (cur.isEmpty()) return false
            // A second matra replaces the first instead of stacking.
            entries[row][col] = if (isMatra(key) && isMatra(cur.last().toString())) cur.dropLast(1) + key else cur + key
        } else when {
            fresh || cur.isEmpty() -> entries[row][col] = key
            cur.endsWith(HALANT) -> entries[row][col] = cur + key
            else -> {
                moveInWord(1)
                entries[row][col] = key
            }
        }
        fresh = false
        return afterChange(wasSolved)
    }

    fun delete() {
        if (entries[row][col].isEmpty()) moveInWord(-1)
        entries[row][col] = entries[row][col].dropLast(1)
        fresh = false
    }

    /** Fills the current box with the correct akshara. Returns true if a word got solved. */
    fun revealCell(): Boolean {
        val wasSolved = isSolved(currentWord)
        entries[row][col] = puzzle.solution[row][col]!!
        val solved = afterChange(wasSolved)
        if (!solved) { moveInWord(1); fresh = true }
        return solved
    }

    private fun afterChange(wasSolved: Boolean): Boolean {
        val w = currentWord
        if (wasSolved || !isSolved(w)) return false
        // Jump to the next unsolved word, newspaper-solver style.
        val ws = puzzle.words
        val start = ws.indexOf(w)
        (1..ws.size).map { ws[(start + it) % ws.size] }.firstOrNull { !isSolved(it) }?.let(::selectWord)
        return true
    }

    private fun moveInWord(step: Int) {
        val cells = currentWord.cells()
        val i = cells.indexOf(row to col) + step
        if (i in cells.indices) { row = cells[i].first; col = cells[i].second }
    }

    fun isCorrect(r: Int, c: Int) = norm(entries[r][c]) == norm(puzzle.solution[r][c] ?: "")
    fun isSolved(w: Word) = w.cells().all { (r, c) -> isCorrect(r, c) }
    fun isCellInSolvedWord(r: Int, c: Int) = puzzle.words.any { it.contains(r, c) && isSolved(it) }
    fun isComplete() = puzzle.words.all(::isSolved)
    /** Word fully filled but wrong: shown with a calm hint only. */
    fun isFilledWrong(w: Word) = w.cells().all { (r, c) -> entries[r][c].isNotEmpty() } && !isSolved(w)

    fun serialize(): String = entries.joinToString("|") { it.joinToString("|") }

    companion object {
        const val HALANT = "्"
        fun empty(size: Int) = Array(size) { Array(size) { "" } }
        fun deserialize(s: String?, size: Int): Array<Array<String>> {
            val parts = s?.split("|")
            if (parts == null || parts.size != size * size) return empty(size)
            return Array(size) { r -> Array(size) { c -> parts[r * size + c] } }
        }
        private fun norm(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD)
        /** Dependent signs: matras, ं ँ ः, ्, nukta. */
        fun isSign(k: String) = k.isNotEmpty() && k[0].code.let { it in 0x0900..0x0903 || it in 0x093A..0x094F || it in 0x0962..0x0963 }
        private fun isMatra(k: String) = k.isNotEmpty() && k[0].code.let { it in 0x093E..0x094C || it in 0x0962..0x0963 }
    }
}
