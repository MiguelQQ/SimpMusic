package com.maxrave.simpmusic.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KillmonReleaseTest {
    @Test
    fun currentTwoPartTagIsNotNewerThan071() {
        val json =
            """
            [
              {
                "tag_name": "v0.7-killmon",
                "name": "Killmon Music 0.7",
                "draft": false,
                "prerelease": true,
                "body": "versionCode: 65",
                "assets": [
                  {
                    "name": "KillmonMusic-0.7-arm64-release.apk",
                    "browser_download_url": "https://example.com/0.7.apk"
                  }
                ]
              }
            ]
            """.trimIndent()
        assertNull(selectKillmonUpdate(json, "0.7.1", 66))
    }

    @Test
    fun sameSemverWithoutHigherCodeIsNotNewer() {
        val json = release("v0.7.1-killmon", "versionCode: 66", "KillmonMusic-0.7.1-arm64-release.apk")
        assertNull(selectKillmonUpdate(json, "0.7.1", 66))
    }

    @Test
    fun sameSemverWithHigherCodeIsNewer() {
        val json = release("v0.7.1-killmon", "Notas\nversionCode: 67", "KillmonMusic-0.7.1-arm64-release.apk")
        val update = selectKillmonUpdate(json, "0.7.1", 66)
        assertEquals("v0.7.1-killmon", update?.label)
        assertEquals(67, update?.versionCode)
        assertEquals("https://example.com/apk", update?.downloadUrl)
    }

    @Test
    fun highestTagFallsBackToReleasePageWithoutArm64() {
        val json =
            """
            [
              {
                "tag_name": "v0.9-killmon",
                "name": "nine",
                "draft": false,
                "prerelease": true,
                "body": "versionCode: 90",
                "assets": [
                  {"name": "KillmonMusic-0.9-universal.apk", "browser_download_url": "https://example.com/u.apk"}
                ]
              },
              {
                "tag_name": "v0.7.2-killmon",
                "name": "Killmon Music 0.7.2",
                "draft": false,
                "prerelease": true,
                "body": "versionCode: 68",
                "assets": [
                  {"name": "notes.txt", "browser_download_url": "https://example.com/n"},
                  {"name": "KillmonMusic-0.7.2-arm64-release.apk", "browser_download_url": "https://example.com/072.apk"}
                ]
              },
              {
                "tag_name": "v0.6-killmon",
                "draft": false,
                "assets": [
                  {"name": "KillmonMusic-0.6-arm64-release.apk", "browser_download_url": "https://example.com/06.apk"}
                ]
              }
            ]
            """.trimIndent()
        val update = selectKillmonUpdate(json, "0.7.1", 66)
        assertEquals("v0.9-killmon", update?.label)
        assertNull(update?.downloadUrl)
        assertEquals("https://github.com/MiguelQQ/SimpMusic/releases/tag/v0.9-killmon", update?.releaseUrl)
    }

    @Test
    fun prereleaseNewerThanInstalledIsOfferedAndStableWinsOverIt() {
        val beta = release("v0.8.0-beta.1", "", "KillmonMusic-0.8.0-beta-arm64-release.apk")
        assertEquals("v0.8.0-beta.1", selectKillmonUpdate(beta, "0.7.1", 66)?.label)
        val both =
            """
            [
              ${releaseBody("v0.8.0-beta.1", "", "KillmonMusic-b-arm64-release.apk", "https://example.com/b.apk")},
              ${releaseBody("v0.8.0", "", "KillmonMusic-s-arm64-release.apk", "https://example.com/s.apk")}
            ]
            """.trimIndent()
        assertEquals("v0.8.0", selectKillmonUpdate(both, "0.7.1", 66)?.label)
    }

    @Test
    fun draftAndBrokenPayloadsAreIgnored() {
        val draft =
            """
            [{"tag_name":"v9.0.0","draft":true,"assets":[
              {"name":"KillmonMusic-9-arm64-release.apk","browser_download_url":"https://example.com/9.apk"}
            ]}]
            """.trimIndent()
        assertNull(selectKillmonUpdate(draft, "0.7.1", 66))
        assertNull(selectKillmonUpdate("""{"message":"API rate limit exceeded"}""", "0.7.1", 66))
        assertNull(selectKillmonUpdate("", "0.7.1", 66))
        assertNull(selectKillmonUpdate("[]", "0.7.1", 66))
    }

    @Test
    fun killmonSuffixDoesNotCountAsAPrerelease() {
        val parsed = parseKillmonVersion("v0.7.1-killmon")
        assertEquals(SemVer(0, 7, 1), parsed)
        assertTrue(parseKillmonVersion("v0.7-killmon")!! < parseKillmonVersion("0.7.1")!!)
        assertTrue(parseKillmonVersion("v0.8-killmon")!! > parseKillmonVersion("0.7.1")!!)
    }

    @Test
    fun numericSemverIncludesKillmonPrereleasesWithoutNotes() {
        val json = release("v0.7.14-killmon", "", "KillmonMusic-0.7.14-arm64-release.apk")
        assertEquals("v0.7.14-killmon", selectKillmonUpdate(json, "0.7.13", 78)?.label)
        assertNull(selectKillmonUpdate(json, "0.7.14", 79))
        assertNull(selectKillmonUpdate(json, "0.7.15-killmon", 80))
        assertTrue(parseKillmonVersion("0.7.10")!! > parseKillmonVersion("0.7.9")!!)
        assertTrue(parseKillmonVersion("0.10.0")!! > parseKillmonVersion("0.9.99")!!)
    }

    @Test
    fun plainVersionCodeInNotesBreaksSemverTie() {
        val json = release("v0.7.14-killmon", "versionCode 80", "KillmonMusic-0.7.14-arm64-release.apk")
        assertEquals(80, selectKillmonUpdate(json, "0.7.14", 79)?.versionCode)
        assertNull(selectKillmonUpdate(json, "0.7.14", 80))
        assertNull(selectKillmonUpdate(json, "0.7.15", 79))
    }

    @Test
    fun malformedAndOverflowVersionsDoNotCrash() {
        assertNull(parseKillmonVersion("banana"))
        assertNull(parseKillmonVersion("v999999999999999999.7.14-killmon"))
        assertNull(selectKillmonUpdate(release("v999999999999999999.7.14-killmon", "", "KillmonMusic-0.7.14-arm64-release.apk"), "0.7.13", 78))
    }

    private fun release(
        tag: String,
        body: String,
        asset: String,
    ): String = "[${releaseBody(tag, body, asset, "https://example.com/apk")}]"

    private fun releaseBody(
        tag: String,
        body: String,
        asset: String,
        url: String,
    ): String =
        """
        {"tag_name":"$tag","draft":false,"prerelease":true,"body":${jsonString(body)},
         "assets":[{"name":"$asset","browser_download_url":"$url"}]}
        """.trimIndent()

    private fun jsonString(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
}
