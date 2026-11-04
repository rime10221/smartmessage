package com.callcheck.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 콜 기록 저장소. filesDir/calls.json 에 JSON 배열로 보관.
 * WebView(JS 브리지)와 NtfyService 가 공유하므로 모든 접근을 동기화한다.
 * 각 항목: { id, t(ms), note, done, doneT(ms|null) }
 */
object Store {
    private const val FILE = "calls.json"
    private val lock = Any()

    private fun file(ctx: Context) = File(ctx.filesDir, FILE)

    private fun read(ctx: Context): JSONArray {
        return try {
            val f = file(ctx)
            if (!f.exists()) JSONArray() else JSONArray(f.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            JSONArray()
        }
    }

    private fun write(ctx: Context, arr: JSONArray) {
        try {
            file(ctx).writeText(arr.toString(), Charsets.UTF_8)
        } catch (_: Exception) {
        }
    }

    fun listJson(ctx: Context): String = synchronized(lock) { read(ctx).toString() }

    fun add(ctx: Context, t: Long): String = synchronized(lock) {
        val arr = read(ctx)
        val id = t.toString() + "_" + (Math.random() * 1_000_000).toInt()
        val o = JSONObject()
        o.put("id", id)
        o.put("t", t)
        o.put("note", "")
        o.put("done", false)
        o.put("doneT", JSONObject.NULL)
        arr.put(o)
        write(ctx, arr)
        id
    }

    fun toggle(ctx: Context, id: String) = synchronized(lock) {
        val arr = read(ctx)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("id") == id) {
                val done = !o.optBoolean("done", false)
                o.put("done", done)
                o.put("doneT", if (done) System.currentTimeMillis() else JSONObject.NULL)
            }
        }
        write(ctx, arr)
    }

    fun setNote(ctx: Context, id: String, note: String) = synchronized(lock) {
        val arr = read(ctx)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("id") == id) o.put("note", note)
        }
        write(ctx, arr)
    }

    fun remove(ctx: Context, id: String) = synchronized(lock) {
        val arr = read(ctx)
        val out = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("id") != id) out.put(o)
        }
        write(ctx, out)
    }
}
