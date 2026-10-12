package com.nuvio.app.features.debrid

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DebridPendingDeviceAuthExpiryTest {

    @Test
    fun `absolute iso expiry expires after the timestamp`() {
        val session = DebridDeviceAuthorization(
            providerId = "torbox",
            deviceCode = "device",
            userCode = "USER",
            verificationUrl = "https://example.com/device",
            friendlyVerificationUrl = "https://example.com/device",
            intervalSeconds = 5,
            expiresAt = "2026-01-01T00:00:00.000Z",
            savedAtEpochMs = null,
        )

        assertFalse(isDebridDeviceAuthorizationExpired(session, nowEpochMs = 1_700_000_000_000L))
        assertTrue(isDebridDeviceAuthorizationExpired(session, nowEpochMs = 1_700_000_000_000L + 60_000L))
    }

    @Test
    fun `relative lifetime is anchored to the saved timestamp`() {
        val session = DebridDeviceAuthorization(
            providerId = "premiumize",
            deviceCode = "device",
            userCode = "USER",
            verificationUrl = "https://example.com",
            friendlyVerificationUrl = "https://example.com",
            intervalSeconds = 5,
            expiresAt = "600s",
            savedAtEpochMs = 1_000_000_000L,
        )

        assertEquals(1_000_000_000L + 600_000L, debridDeviceAuthorizationExpiryEpochMs(session))
        assertFalse(isDebridDeviceAuthorizationExpired(session, nowEpochMs = 1_000_000_000L + 500_000L))
        assertTrue(isDebridDeviceAuthorizationExpired(session, nowEpochMs = 1_000_000_000L + 601_000L))
    }

    @Test
    fun `relative lifetime without a saved timestamp cannot be evaluated`() {
        val session = DebridDeviceAuthorization(
            providerId = "premiumize",
            deviceCode = "device",
            userCode = "USER",
            verificationUrl = "https://example.com",
            friendlyVerificationUrl = "https://example.com",
            intervalSeconds = 5,
            expiresAt = "600s",
            savedAtEpochMs = null,
        )

        assertNull(debridDeviceAuthorizationExpiryEpochMs(session))
        assertFalse(isDebridDeviceAuthorizationExpired(session, nowEpochMs = Long.MAX_VALUE / 2))
    }

    @Test
    fun `missing or blank expiry never expires`() {
        assertFalse(isDebridDeviceAuthorizationExpired(session("600s", 1_000L), nowEpochMs = 9_999_999_999L))
        assertFalse(isDebridDeviceAuthorizationExpired(session(null, null), nowEpochMs = 1_000L))
        assertFalse(isDebridDeviceAuthorizationExpired(session("   ", null), nowEpochMs = 1_000L))
    }

    private fun session(
        expiresAt: String?,
        savedAtEpochMs: Long?,
    ): DebridDeviceAuthorization = DebridDeviceAuthorization(
        providerId = "torbox",
        deviceCode = "device",
        userCode = "USER",
        verificationUrl = "https://example.com",
        friendlyVerificationUrl = "https://example.com",
        intervalSeconds = 5,
        expiresAt = expiresAt,
        savedAtEpochMs = savedAtEpochMs,
    )
}
