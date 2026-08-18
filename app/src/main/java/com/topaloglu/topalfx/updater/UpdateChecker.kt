package com.topaloglu.topalfx.updater

import android.content.Context
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    private const val RELEASES_URL =
        "https://api.github.com/repos/abboodan/TopalFX_Claude/releases/latest"
    private const val TIMEOUT_MS = 4_000

    data class ReleaseInfo(
        val tag: String,
        val changelog: String,
        val apkUrl: String?,
    )

    /** Returns null on any failure (offline, timeout, parse error) — never throws. */
    suspend fun fetchLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(RELEASES_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JsonParser.parseString(body).asJsonObject
                val tag = json.get("tag_name")?.asString ?: return@withContext null
                val changelog = json.get("body")?.takeIf { !it.isJsonNull }?.asString ?: ""
                val apkUrl = json.getAsJsonArray("assets")
                    ?.map { it.asJsonObject }
                    ?.firstOrNull { it.get("name")?.asString?.endsWith(".apk") == true }
                    ?.get("browser_download_url")?.asString
                ReleaseInfo(tag, changelog, apkUrl)
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun currentVersionName(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
    } catch (e: Exception) {
        "0.0.0"
    }

    fun isUpdateAvailable(release: ReleaseInfo, context: Context): Boolean =
        release.apkUrl != null && SemVer.isNewer(release.tag, currentVersionName(context))
}
