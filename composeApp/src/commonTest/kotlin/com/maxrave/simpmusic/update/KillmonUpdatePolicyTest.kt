package com.maxrave.simpmusic.update

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KillmonUpdatePolicyTest {
    @Test
    fun coldStartAlwaysChecksAndResumesAreDebounced() {
        val policy = KillmonUpdatePolicy()
        assertTrue(policy.startCheck(100))
        assertFalse(policy.startCheck(100))
        assertFalse(policy.startCheck(100 + 30 * 60 * 1000L - 1))
        assertTrue(policy.startCheck(100 + 30 * 60 * 1000L))
        // A new process has no persisted debounce, even after an unsuccessful request.
        assertTrue(KillmonUpdatePolicy().startCheck(101))
    }

    @Test
    fun dismissalOnlySuppressesThatBuildForThisProcess() {
        val policy = KillmonUpdatePolicy()
        val update = KillmonUpdate("v0.7.14-killmon", "https://example.com/app.apk", "https://github.com/MiguelQQ/SimpMusic/releases/tag/v0.7.14-killmon", 79)
        assertTrue(policy.shouldOffer(update))
        policy.dismiss(update)
        assertFalse(policy.shouldOffer(update))
        assertTrue(policy.shouldOffer(update.copy(versionCode = 80)))
        assertTrue(policy.shouldOffer(update.copy(label = "v0.7.15-killmon")))
        assertTrue(KillmonUpdatePolicy().shouldOffer(update))
    }
}
