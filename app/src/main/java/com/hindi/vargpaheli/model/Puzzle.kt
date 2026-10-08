package com.hindi.vargpaheli.model

/** One clue/answer in the grid. Each box holds one akshara (e.g. "पु", "क्र"). */
data class Word(
    val number: Int,
    val across: Boolean,
    val row: Int,
    val col: Int,
    val length: Int,
    val answer: String,
    val clue: String,
) {
    fun cells(): List<Pair<Int, Int>> =
        List(length) { i -> if (across) row to col + i else row + i to col }

    fun contains(r: Int, c: Int) =
        if (across) r == row && c in col until col + length else c == col && r in row until row + length
}

/** solution[r][c] is the akshara in that box, or null for a black box. */
class Puzzle(
    val id: Int,
    val title: String,
    val size: Int,
    val solution: List<List<String?>>,
    val words: List<Word>,
) {
    /** Clue number shown in the top-left corner of a box, 0 if none. */
    val numbers: Array<IntArray> = Array(size) { IntArray(size) }.also { n ->
        words.forEach { n[it.row][it.col] = it.number }
    }

    fun isBlock(r: Int, c: Int) = solution[r][c] == null
    fun wordAt(r: Int, c: Int, across: Boolean) = words.firstOrNull { it.across == across && it.contains(r, c) }

    companion object {
        /** Builds a puzzle from a grid and (row, col, across, answer, clue) entries; numbers clues like a newspaper. */
        fun build(
            id: Int, title: String, solution: List<List<String?>>,
            entries: List<Entry>,
        ): Puzzle {
            val size = solution.size
            fun length(e: Entry): Int {
                var n = 0
                while (true) {
                    val r = if (e.across) e.row else e.row + n
                    val c = if (e.across) e.col + n else e.col
                    if (r >= size || c >= size || solution[r][c] == null) return n
                    n++
                }
            }
            val starts = entries.map { it.row to it.col }.distinct().sortedWith(compareBy({ it.first }, { it.second }))
            val words = entries.map { e ->
                Word(starts.indexOf(e.row to e.col) + 1, e.across, e.row, e.col, length(e), e.answer, e.clue)
            }.sortedWith(compareBy({ !it.across }, { it.number }))
            return Puzzle(id, title, size, solution, words)
        }
    }

    data class Entry(val row: Int, val col: Int, val across: Boolean, val answer: String, val clue: String)
}
