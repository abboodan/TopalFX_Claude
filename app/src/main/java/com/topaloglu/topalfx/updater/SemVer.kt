package com.topaloglu.topalfx.updater

/** Pure semantic-version comparison — no Android dependencies, unit-testable. */
object SemVer {

    /** Converts "v1.2.3" / "1.2" to a comparable number, or null when malformed. */
    fun toNumeric(version: String): Long? {
        val cleaned = version.trim().removePrefix("v").removePrefix("V")
        if (cleaned.isEmpty()) return null
        val parts = cleaned.split('.')
        if (parts.size > 3) return null
        val numbers = parts.map { it.toLongOrNull() ?: return null }
        if (numbers.any { it < 0 }) return null
        val (major, minor, patch) = List(3) { numbers.getOrElse(it) { 0L } }
        return major * 1_000_000 + minor * 1_000 + patch
    }

    /** True only when [remoteTag] is strictly newer than [currentVersion]. */
    fun isNewer(remoteTag: String, currentVersion: String): Boolean {
        val remote = toNumeric(remoteTag) ?: return false
        val current = toNumeric(currentVersion) ?: return false
        return remote > current
    }
}
