package com.maxrave.simpmusic.utils

/**
 * Hosts that must not be shown or opened from the Killmon UI (site, blog, upstream
 * repo, donations, Discord invites). Backend hosts such as api-lyrics and chart API
 * are not matched here.
 */
private val visibleProjectLink =
    Regex(
        """(?i)https?://(?:www\.)?simpmusic\.org\S*|https?://maxrave\.dev\S*|https?://(?:www\.)?buymeacoffee\.com\S*|https?://discord\.gg\S*|https?://github\.com/maxrave-dev/\S*|git://github\.com/maxrave-dev/\S*|ssh://(?:git@)?github\.com/maxrave-dev/\S*|scm:git@github\.com:maxrave-dev/\S*|https?://x\.com/maxrave\S*|(?<![\w.-])simpmusic\.org\b|(?<![\w.-])maxrave\.dev\b""",
    )

fun String.withoutVisibleProjectLinks(): String =
    replace(visibleProjectLink, "")
        .replace(Regex("[ \\t]{2,}"), " ")
        .replace(Regex(" *\\n *"), "\n")
        .trim()

fun String.isVisibleProjectLink(): Boolean {
    val value = trim()
    if (value.isEmpty()) return false
    return visibleProjectLink.containsMatchIn(value) &&
        !value.contains("api-lyrics.simpmusic.org") &&
        !value.contains("chart.simpmusic.org/api")
}
