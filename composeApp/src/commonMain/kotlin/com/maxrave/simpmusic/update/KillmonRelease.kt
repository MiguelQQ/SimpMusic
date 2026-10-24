package com.maxrave.simpmusic.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One installable Killmon APK chosen from the GitHub releases list. */
data class KillmonUpdate(
    /** Tag or release name, shown in the dialog. */
    val label: String,
    val downloadUrl: String,
    val versionCode: Int?,
)

private val json = Json { ignoreUnknownKeys = true }

private val versionPattern =
    Regex(
        """^v?(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?$""",
        RegexOption.IGNORE_CASE,
    )

private val versionSearch =
    Regex(
        """v?\d+(?:\.\d+){0,2}(?:-[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?""",
        RegexOption.IGNORE_CASE,
    )

private val versionCodePattern = Regex("""(?i)versionCode["'\s:=]+(\d+)""")

private val arm64Apk = Regex("""^KillmonMusic-.*arm64-release\.apk$""")

/**
 * Newest release that ships a `KillmonMusic-*-arm64-release.apk`, when it is newer than the
 * installed build. Drafts are skipped. Prereleases are included. A `-killmon` tag suffix is a
 * label, not a semver prerelease. Returns null on a bad payload or when nothing is newer.
 *
 * Newer means a higher semver, or the same semver with a `versionCode` in the body that is
 * higher than [installedCode].
 */
fun selectKillmonUpdate(
    releasesJson: String,
    installedName: String,
    installedCode: Int,
): KillmonUpdate? {
    val installed = parseKillmonVersion(installedName) ?: return null
    val releases =
        runCatching { json.decodeFromString<List<GhRelease>>(releasesJson) }.getOrNull() ?: return null
    val best =
        releases
            .asSequence()
            .filter { it.draft != true }
            .mapNotNull { it.toCandidate() }
            .maxWithOrNull(compareBy<ReleaseCandidate>({ it.semver }, { it.versionCode ?: -1 }))
            ?: return null
    if (!best.isNewerThan(installed, installedCode)) return null
    return KillmonUpdate(
        label = best.label,
        downloadUrl = best.downloadUrl,
        versionCode = best.versionCode,
    )
}

internal fun parseKillmonVersion(raw: String): SemVer? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    versionPattern.matchEntire(trimmed)?.let { return it.toSemVer() }
    val found = versionSearch.find(trimmed)?.value ?: return null
    return versionPattern.matchEntire(found)?.toSemVer()
}

private fun MatchResult.toSemVer(): SemVer {
    val pre =
        groupValues[4]
            .split('.')
            .filter { it.isNotEmpty() && !it.equals("killmon", ignoreCase = true) }
    return SemVer(
        major = groupValues[1].toInt(),
        minor = groupValues[2].toIntOrNull() ?: 0,
        patch = groupValues[3].toIntOrNull() ?: 0,
        pre = pre,
    )
}

internal data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val pre: List<String> = emptyList(),
) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        if (patch != other.patch) return patch.compareTo(other.patch)
        if (pre.isEmpty() && other.pre.isEmpty()) return 0
        if (pre.isEmpty()) return 1
        if (other.pre.isEmpty()) return -1
        val count = minOf(pre.size, other.pre.size)
        for (index in 0 until count) {
            val diff = comparePre(pre[index], other.pre[index])
            if (diff != 0) return diff
        }
        return pre.size.compareTo(other.pre.size)
    }
}

private fun comparePre(
    a: String,
    b: String,
): Int {
    val left = a.toIntOrNull()
    val right = b.toIntOrNull()
    return when {
        left != null && right != null -> left.compareTo(right)
        left != null -> -1
        right != null -> 1
        else -> a.compareTo(b)
    }
}

private data class ReleaseCandidate(
    val label: String,
    val downloadUrl: String,
    val semver: SemVer,
    val versionCode: Int?,
)

private fun ReleaseCandidate.isNewerThan(
    installed: SemVer,
    installedCode: Int,
): Boolean {
    val compared = semver.compareTo(installed)
    if (compared > 0) return true
    if (compared < 0) return false
    val code = versionCode ?: return false
    return code > installedCode
}

private fun GhRelease.toCandidate(): ReleaseCandidate? {
    val semver = parseKillmonVersion(tagName.orEmpty()) ?: parseKillmonVersion(name.orEmpty()) ?: return null
    val asset =
        assets.orEmpty().firstOrNull { asset ->
            val name = asset.name.orEmpty()
            val url = asset.browserDownloadUrl.orEmpty()
            arm64Apk.matches(name) && url.startsWith("https://")
        } ?: return null
    val label = tagName?.takeIf { it.isNotBlank() } ?: name?.takeIf { it.isNotBlank() } ?: return null
    return ReleaseCandidate(
        label = label,
        downloadUrl = asset.browserDownloadUrl.orEmpty(),
        semver = semver,
        versionCode = versionCodePattern.find(body.orEmpty())?.groupValues?.get(1)?.toIntOrNull(),
    )
}

@Serializable
private data class GhRelease(
    @SerialName("tag_name") val tagName: String? = null,
    val name: String? = null,
    val draft: Boolean? = null,
    val body: String? = null,
    val assets: List<GhAsset>? = null,
)

@Serializable
private data class GhAsset(
    val name: String? = null,
    @SerialName("browser_download_url") val browserDownloadUrl: String? = null,
)
