package com.azhari.tclremote.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Tv(val host: String, val name: String)

/** [link] is an app link (https://…, market://…) or an Android package name. */
data class TvApp(val name: String, val link: String)

val DEFAULT_APPS = listOf(
    TvApp("YouTube", "https://www.youtube.com"),
    TvApp("Netflix", "https://www.netflix.com/title"),
    TvApp("Prime Video", "https://app.primevideo.com"),
    TvApp("Disney+", "https://www.disneyplus.com"),
    TvApp("Spotify", "spotify://"),
    TvApp("YouTube Music", "com.google.android.youtube.tvmusic"),
    TvApp("Play Store", "com.android.vending"),
)

/** Paired TVs and the user's own app shortcuts, kept in SharedPreferences. */
class TvStore(context: Context) {
    private val prefs = context.getSharedPreferences("tcl_remote", Context.MODE_PRIVATE)

    var lastHost: String?
        get() = prefs.getString(KEY_LAST, null)
        set(value) = prefs.edit().putString(KEY_LAST, value).apply()

    fun savedTvs(): List<Tv> = readList(KEY_TVS) { Tv(it.getString("host"), it.getString("name")) }

    fun saveTv(tv: Tv) = writeList(KEY_TVS, listOf(tv) + savedTvs().filter { it.host != tv.host }) {
        JSONObject().put("host", it.host).put("name", it.name)
    }

    fun removeTv(tv: Tv) = writeList(KEY_TVS, savedTvs().filter { it.host != tv.host }) {
        JSONObject().put("host", it.host).put("name", it.name)
    }

    fun customApps(): List<TvApp> = readList(KEY_APPS) { TvApp(it.getString("name"), it.getString("link")) }

    fun addApp(app: TvApp) = writeApps(customApps().filter { it.link != app.link } + app)

    fun removeApp(app: TvApp) = writeApps(customApps().filter { it.link != app.link })

    private fun writeApps(apps: List<TvApp>) = writeList(KEY_APPS, apps) {
        JSONObject().put("name", it.name).put("link", it.link)
    }

    private fun <T> readList(key: String, parse: (JSONObject) -> T): List<T> {
        val array = runCatching { JSONArray(prefs.getString(key, "[]")) }.getOrElse { JSONArray() }
        return (0 until array.length()).mapNotNull { runCatching { parse(array.getJSONObject(it)) }.getOrNull() }
    }

    private fun <T> writeList(key: String, items: List<T>, toJson: (T) -> JSONObject) {
        val array = JSONArray()
        items.forEach { array.put(toJson(it)) }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private companion object {
        const val KEY_LAST = "last_host"
        const val KEY_TVS = "tvs"
        const val KEY_APPS = "apps"
    }
}
