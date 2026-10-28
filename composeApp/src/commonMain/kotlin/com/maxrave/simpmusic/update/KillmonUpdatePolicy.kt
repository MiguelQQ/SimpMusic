package com.maxrave.simpmusic.update

/** In-memory only: a new process always checks, even after a recent dismissal or error. */
internal class KillmonUpdatePolicy {
    private var lastCheckMs: Long? = null
    private val dismissed = mutableSetOf<Pair<String, Int?>>()

    fun startCheck(nowMs: Long): Boolean {
        val last = lastCheckMs
        if (last != null && nowMs - last < 30 * 60 * 1000L) return false
        lastCheckMs = nowMs
        return true
    }

    fun dismiss(update: KillmonUpdate) {
        dismissed += update.label to update.versionCode
    }

    fun shouldOffer(update: KillmonUpdate): Boolean = (update.label to update.versionCode) !in dismissed
}
