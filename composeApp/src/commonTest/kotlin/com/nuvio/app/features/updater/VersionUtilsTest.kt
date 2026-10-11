package com.nuvio.app.features.updater

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class VersionUtilsTest {
    @Test
    fun `stable release is newer than its prerelease`() {
        assertTrue(VersionUtils.isRemoteNewer("1.1.0", "1.1.0-rc.2"))
    }

    @Test
    fun `beta identifiers use numeric ordering`() {
        assertTrue(VersionUtils.isRemoteNewer("1.1.0-beta.10", "1.1.0-beta.9"))
    }

    @Test
    fun `prerelease for a later minor is newer than current stable`() {
        assertTrue(VersionUtils.isRemoteNewer("1.1.0-beta.1", "1.0.3"))
    }

    @Test
    fun `stable channel patch is not newer than later minor beta`() {
        assertFalse(VersionUtils.isRemoteNewer("1.0.4", "1.1.0-beta.2"))
    }

    @Test
    fun `version prefix and build metadata do not affect precedence`() {
        assertFalse(VersionUtils.isRemoteNewer("v1.0.0+18", "1.0.0+17"))
    }

    @Test
    fun `invalid remote version is not offered`() {
        assertFalse(VersionUtils.isRemoteNewer("latest", "1.0.0"))
    }

    @Test
    fun `legacy comparison accepts changed release labels`() {
        assertTrue(VersionUtils.isRemoteNewerLegacy("desktop-preview", "desktop-alpha"))
        assertFalse(VersionUtils.isRemoteNewerLegacy("desktop-preview", "desktop-preview"))
        assertFalse(VersionUtils.isRemoteNewerLegacy("", "desktop-preview"))
    }

    @Test
    fun `current beta naming is recognized as prerelease`() {
        assertTrue(VersionUtils.isPrerelease("0.8.12-beta"))
    }

    @Test
    fun `two component versions are accepted`() {
        assertTrue(VersionUtils.isRemoteNewer("0.2", "0.1"))
        assertFalse(VersionUtils.isRemoteNewer("0.1", "0.1"))
    }

    @Test
    fun `two component version compares against three component versions`() {
        assertTrue(VersionUtils.isRemoteNewer("0.2", "0.1.9"))
        assertFalse(VersionUtils.isRemoteNewer("0.1", "0.1.9"))
        assertTrue(VersionUtils.isRemoteNewer("1.0.0", "0.9"))
    }

    @Test
    fun `fork release labels are parsed by their embedded version`() {
        assertTrue(VersionUtils.isRemoteNewer("NuvioDev 0.2", "0.1"))
        assertTrue(VersionUtils.isRemoteNewer("NuvioDev-v0.2", "0.1"))
        assertTrue(VersionUtils.isRemoteNewer("nuvio-dev-0.2", "NuvioDev 0.1"))
        assertFalse(VersionUtils.isRemoteNewer("NuvioDev 0.1", "0.1"))
    }

    @Test
    fun `rolling release tag without a version is not offered`() {
        assertFalse(VersionUtils.isRemoteNewer("nuvio-dev-latest", "0.1"))
    }
}
