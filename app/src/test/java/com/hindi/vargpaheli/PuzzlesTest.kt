package com.hindi.vargpaheli

import com.hindi.vargpaheli.engine.CrosswordEngine
import com.hindi.vargpaheli.model.Puzzle
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PuzzlesTest {
    /** Same keys as ui/HindiKeyboard. */
    private val keys = """अ आ इ ई उ ऊ ए ऐ ओ औ ऋ क ख ग घ ङ च छ ज झ ञ ट ठ ड ढ ड़ ढ़ ण त थ द ध न प फ ब भ म य र ल
        व श ष स ह क्ष त्र ज्ञ श्र ा ि ी ु ू े ै ो ौ ृ ं ँ ः ्""".split(Regex("\\s+")).toSet()

    private fun puzzles(): List<Puzzle> {
        val arr = JSONArray(File("src/main/assets/puzzles.json").readText())
        return List(arr.length()) { i ->
            val p = arr.getJSONObject(i)
            val g = p.getJSONArray("grid")
            val grid = List(g.length()) { r -> g.getJSONArray(r).let { row -> List(row.length()) { row.getString(it).ifEmpty { null } } } }
            val cl = p.getJSONArray("clues")
            Puzzle.build(p.getInt("id"), p.getString("title"), grid, List(cl.length()) { k ->
                cl.getJSONObject(k).let { Puzzle.Entry(it.getInt("r"), it.getInt("c"), it.getString("d") == "A", it.getString("ans"), it.getString("clue")) }
            })
        }
    }

    /** Splits an akshara into keyboard presses: letters (ड़ is one key), then signs. */
    private fun presses(akshara: String): List<String> {
        val out = mutableListOf<String>()
        for (ch in akshara) {
            val s = ch.toString()
            if (s == "़") out[out.size - 1] = out.last() + s else out += s
        }
        out.forEach { assertTrue("key missing on keyboard: $it in $akshara", it in keys) }
        return out
    }

    @Test fun exactly100LevelsAllSolvableWithKeyboard() {
        val ps = puzzles()
        assertEquals(100, ps.size)
        assertEquals((1..100).toList(), ps.map { it.id })
        for (p in ps) {
            assertTrue(p.words.size in 5..15)
            val e = CrosswordEngine(p, CrosswordEngine.empty(p.size))
            for (w in p.words) {
                assertEquals("${p.id} ${w.answer}", w.answer, w.cells().joinToString("") { (r, c) -> p.solution[r][c]!! })
                if (e.isSolved(w)) continue
                e.selectWord(w)
                // Type the whole word as a user would, box after box, without tapping.
                w.cells().forEach { (r, c) ->
                    e.tap(r, c); if (e.currentWord != w) e.tap(r, c)
                    presses(p.solution[r][c]!!).forEach { k -> e.input(k) }
                }
                assertTrue("${p.id} ${w.answer} not solved", e.isSolved(w))
            }
            assertTrue("level ${p.id} not complete", e.isComplete())
            // Every white box belongs to a clue.
            for (r in 0 until p.size) for (c in 0 until p.size)
                if (!p.isBlock(r, c)) assertTrue(p.words.any { it.contains(r, c) })
        }
    }

    private fun jaipur(): CrosswordEngine {
        val g = listOf(listOf("ज", "य", "पु", "र"))
        val p = Puzzle.build(1, "t", g + List(3) { List(4) { null } }, listOf(Puzzle.Entry(0, 0, true, "जयपुर", "राजस्थान का गुलाबी शहर")))
        return CrosswordEngine(p, CrosswordEngine.empty(4))
    }

    @Test fun typingFlowsWithoutTapping() {
        val e = jaipur()
        listOf("ज", "य", "प", "ु", "र").forEach { e.input(it) }
        assertEquals(listOf("ज", "य", "पु", "र"), e.entries[0].toList())
        assertTrue(e.isComplete())
    }

    @Test fun conjunctsAndMatrasStayInOneBox() {
        val e = jaipur()
        listOf("क", "्", "र", "ि").forEach { e.input(it) }
        assertEquals("क्रि", e.entries[0][0])
        e.input("ा") // second matra replaces the first
        assertEquals("क्रा", e.entries[0][0])
        e.input("म")
        assertEquals("म", e.entries[0][1])
        e.delete(); e.delete()
        assertEquals("", e.entries[0][1])
        assertEquals("क्र", e.entries[0][0])
    }

    @Test fun wrongWordIsNotGreenAndSaveRestoreRoundTrips() {
        val e = jaipur()
        listOf("ज", "य", "प", "र").forEach { e.input(it) }
        assertFalse(e.isComplete())
        assertTrue(e.isFilledWrong(e.currentWord))
        val restored = CrosswordEngine.deserialize(e.serialize(), 4)
        assertEquals(e.entries.map { it.toList() }, restored.map { it.toList() })
        e.tap(0, 2); e.input("प"); e.input("ु")
        assertTrue(e.isComplete())
    }

    @Test fun revealFillsCurrentBox() {
        val e = jaipur()
        repeat(4) { e.revealCell() }
        assertTrue(e.isComplete())
    }
}
