package com.hindi.vargpaheli.storage

import android.content.Context

/** All progress stays on the device (SharedPreferences). Every change is saved immediately. */
class ProgressManager(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    fun completed(): Set<Int> = prefs.getStringSet("done", emptySet())!!.mapNotNull { it.toIntOrNull() }.toSet()
    fun isCompleted(id: Int) = id in completed()
    fun markCompleted(id: Int) =
        prefs.edit().putStringSet("done", completed().map { it.toString() }.toSet() + id.toString()).apply()

    fun cells(id: Int): String? = prefs.getString("cells_$id", null)
    fun hasProgress(id: Int) = cells(id)?.any { it != '|' } == true

    /** Selected box as "row,col,across". */
    fun position(id: Int): Triple<Int, Int, Boolean>? =
        prefs.getString("pos_$id", null)?.split(",")?.takeIf { it.size == 3 }?.let {
            Triple(it[0].toInt(), it[1].toInt(), it[2] == "1")
        }

    fun save(id: Int, cells: String, row: Int, col: Int, across: Boolean) =
        prefs.edit()
            .putString("cells_$id", cells)
            .putString("pos_$id", "$row,$col,${if (across) 1 else 0}")
            .putInt("last", id)
            .apply()

    fun lastLevel(): Int = prefs.getInt("last", 0)

    /** Change to true to unlock levels one after another (1 → 2 → 3 ...). */
    fun isUnlocked(id: Int) = !SEQUENTIAL_UNLOCK || id == 1 || isCompleted(id - 1)

    companion object {
        const val SEQUENTIAL_UNLOCK = false
    }
}
