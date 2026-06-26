package com.vaibhawmishra.voela.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

// A stable release available on GitHub.
data class Release(
    val version: String,      // e.g. "1.0.2" (tag without the leading v)
    val notes: String,        // release body
    val apkUrl: String,       // browser_download_url of the .apk asset
    val sizeBytes: Long,
    val pageUrl: String,      // html_url of the release page (for "Full changelog")
)

// Self-update for the sideloaded (GitHub Releases) build: check the latest stable
// release, download its APK, and hand it to the system installer. No third-party deps.
object AppUpdate {

    private const val LATEST_URL = "https://api.github.com/repos/itsvaibhavmishra/Voela/releases/latest"
    private const val PREFS = "voela_update"
    private const val KEY_DISMISSED = "dismissed_version"
    private const val KEY_LAST_CHECK = "last_check_ms"
    const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L // once a day for the auto-check

    fun currentVersion(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "0"

    // Numeric semver compare: is `latest` newer than `current`?
    fun isNewer(latest: String, current: String): Boolean {
        fun parts(v: String) = v.trim().removePrefix("v").split(".", "-").mapNotNull { it.toIntOrNull() }
        val l = parts(latest); val c = parts(current)
        for (i in 0 until maxOf(l.size, c.size)) {
            val a = l.getOrElse(i) { 0 }; val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    suspend fun fetchLatest(): Release? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(LATEST_URL).openConnection() as HttpURLConnection).apply {
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Voela-Android")
                connectTimeout = 15000; readTimeout = 15000
            }
            if (conn.responseCode != 200) return@runCatching null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val tag = json.getString("tag_name")
            val notes = json.optString("body").trim()
            val pageUrl = json.optString("html_url")
            val assets = json.getJSONArray("assets")
            var apkUrl: String? = null; var size = 0L
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = a.getString("browser_download_url"); size = a.optLong("size"); break
                }
            }
            apkUrl?.let { Release(tag.removePrefix("v"), notes, it, size, pageUrl) }
        }.getOrNull()
    }

    // Streams the APK to cacheDir/update, reporting 0..100 progress. Returns the file or null.
    suspend fun download(context: Context, release: Release, onProgress: (Int) -> Unit): File? =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, "update").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
                val out = File(dir, "Voela-${release.version}.apk")
                val conn = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true; connectTimeout = 15000; readTimeout = 30000
                }
                val total = if (release.sizeBytes > 0) release.sizeBytes else conn.contentLengthLong.coerceAtLeast(0)
                conn.inputStream.use { input ->
                    out.outputStream().use { output ->
                        val buf = ByteArray(16 * 1024); var read = 0L; var n: Int
                        while (input.read(buf).also { n = it } >= 0) {
                            output.write(buf, 0, n); read += n
                            if (total > 0) onProgress(((read * 100) / total).toInt().coerceIn(0, 100))
                        }
                    }
                }
                out
            }.getOrNull()
        }

    // On API 26+ the user must allow "install unknown apps" for us first.
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    // Remove any leftover update APK (call on launch; the app is replaced during install).
    fun cleanup(context: Context) {
        runCatching { File(context.cacheDir, "update").listFiles()?.forEach { it.delete() } }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun dismissedVersion(context: Context): String = prefs(context).getString(KEY_DISMISSED, "") ?: ""
    fun setDismissedVersion(context: Context, v: String) = prefs(context).edit().putString(KEY_DISMISSED, v).apply()
    fun lastCheck(context: Context): Long = prefs(context).getLong(KEY_LAST_CHECK, 0L)
    fun setLastCheck(context: Context, t: Long) = prefs(context).edit().putLong(KEY_LAST_CHECK, t).apply()
}
