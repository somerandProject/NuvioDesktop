package com.nuvio.app.features.updater

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GitHubAssetDigestTest {
    @Test
    fun `parses valid sha256 digest`() {
        val hex = "d81e0419b69fc7e6c537de86e5bd0a64c220de413cdedb7949d28828f0096ccc"
        assertEquals(hex, parseGitHubAssetDigest("sha256:$hex"))
        assertEquals(hex, parseGitHubAssetDigest("SHA256:${hex.uppercase()}"))
    }

    @Test
    fun `rejects malformed digests`() {
        assertNull(parseGitHubAssetDigest(null))
        assertNull(parseGitHubAssetDigest(""))
        assertNull(parseGitHubAssetDigest("md5:" + "a".repeat(64)))
        assertNull(parseGitHubAssetDigest("sha256:" + "a".repeat(63)))
        assertNull(parseGitHubAssetDigest("sha256:" + "g".repeat(64)))
        assertNull(parseGitHubAssetDigest("sha512:" + "a".repeat(64)))
        assertNull(parseGitHubAssetDigest("a".repeat(64)))
    }
}
