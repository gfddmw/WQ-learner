package com.example.wq_learner1.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStateTest {
    @Test
    fun snapshotReturnsPersistableLoginState() {
        val state = SessionState()

        state.setSession(AuthSession(accessToken = "abc123", tokenType = "bearer"), "demo@example.com")

        val snapshot = state.snapshot()

        assertEquals("abc123", snapshot?.accessToken)
        assertEquals("bearer", snapshot?.tokenType)
        assertEquals("demo@example.com", snapshot?.email)
    }

    @Test
    fun restoreLoadsPersistedLoginState() {
        val state = SessionState()

        state.restore(
            StoredSession(
                accessToken = "abc123",
                tokenType = "bearer",
                email = "demo@example.com",
            ),
        )

        assertTrue(state.isLoggedIn)
        assertEquals("abc123", state.accessToken)
        assertEquals("bearer", state.tokenType)
        assertEquals("demo@example.com", state.email)
    }

    @Test
    fun restoreIgnoresIncompleteSession() {
        val state = SessionState()

        state.restore(
            StoredSession(
                accessToken = "",
                tokenType = "bearer",
                email = "demo@example.com",
            ),
        )

        assertFalse(state.isLoggedIn)
        assertEquals(null, state.snapshot())
    }
}
