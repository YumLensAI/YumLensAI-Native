package com.yumlensai.data.storage

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yumlensai.data.api.model.Bookmark
import com.yumlensai.data.api.model.StoredBenchmark
import java.io.File
import java.util.UUID

object  StorageManager {

    private const val PREFS_NAME = "yumlensai_prefs"
    private const val KEY_BOOKMARKS = "@yumlensai-bookmarks"
    private const val KEY_BENCHMARKS = "@yumlensai-benchmarks"

    private val gson = Gson()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---- Bookmarks ----

    fun getBookmarks(context: Context): List<Bookmark> {
        val json = prefs(context).getString(KEY_BOOKMARKS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Bookmark>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveBookmark(context: Context, sourceUri: Uri, ingredients: List<String>): Bookmark {
        val id = UUID.randomUUID().toString()
        val destFile = File(context.filesDir, "bookmarks/$id.jpg")
        destFile.parentFile?.mkdirs()

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val bookmark = Bookmark(id = id, image = destFile.absolutePath, ingredients = ingredients)
        val current = getBookmarks(context).toMutableList()
        current.add(bookmark)
        prefs(context).edit().putString(KEY_BOOKMARKS, gson.toJson(current)).apply()
        return bookmark
    }

    fun removeBookmark(context: Context, id: String) {
        val current = getBookmarks(context).toMutableList()
        val bookmark = current.find { it.id == id }
        bookmark?.let {
            File(it.image).delete()
            current.remove(it)
        }
        prefs(context).edit().putString(KEY_BOOKMARKS, gson.toJson(current)).apply()
    }

    // ---- Benchmarks ----

    fun getBenchmarks(context: Context): List<StoredBenchmark> {
        val json = prefs(context).getString(KEY_BENCHMARKS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<StoredBenchmark>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getUnsyncedBenchmarks(context: Context): List<StoredBenchmark> =
        getBenchmarks(context).filter { !it.synced }

    fun saveBenchmark(context: Context, benchmark: StoredBenchmark) {
        val current = getBenchmarks(context).toMutableList()
        current.add(benchmark)
        prefs(context).edit().putString(KEY_BENCHMARKS, gson.toJson(current)).apply()
    }

    fun markBenchmarkSynced(context: Context, id: String) {
        val current = getBenchmarks(context).toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current.removeAt(index)
            prefs(context).edit().putString(KEY_BENCHMARKS, gson.toJson(current)).apply()
        }
    }
}
