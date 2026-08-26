package com.scripthost.ssh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [SSHSessionManager] using a mocked [SSHSessionFactory].
 */
class SSHSessionManagerTest {

    private lateinit var factory: SSHSessionFactory
    private lateinit var session: SSHSession
    private lateinit var sessionManager: SSHSessionManager

    @Before
    fun setUp() {
        factory = mock()
        session = mock()
        sessionManager = SSHSessionManager(factory)
    }

    private fun stubConnect(newSession: SSHSession = session) {
        whenever(factory.open("example.com", 22, "user", "secret")).thenReturn(newSession)
        whenever(newSession.isConnected).thenReturn(true)
    }

    @Test
    fun connect_successStoresSession() {
        stubConnect()

        sessionManager.connect("example.com", 22, "user", "secret")

        assertTrue(sessionManager.isConnected)
        verify(factory).open("example.com", 22, "user", "secret")
    }

    @Test
    fun connect_failurePropagatesAndStaysDisconnected() {
        whenever(factory.open("example.com", 22, "user", "secret"))
            .thenThrow(RuntimeException("Auth fail"))

        assertThrows(RuntimeException::class.java) {
            sessionManager.connect("example.com", 22, "user", "secret")
        }
        assertFalse(sessionManager.isConnected)
    }

    @Test
    fun connect_whenAlreadyConnected_disconnectsPreviousSession() {
        stubConnect()
        sessionManager.connect("example.com", 22, "user", "secret")

        val newSession = mock<SSHSession>()
        stubConnect(newSession)
        sessionManager.connect("example.com", 22, "user", "secret")

        verify(session).disconnect()
    }

    @Test
    fun exec_withoutConnect_throwsIllegalState() {
        val error = assertThrows(IllegalStateException::class.java) {
            sessionManager.exec("tmux ls")
        }
        assertEquals("Not connected", error.message)
    }

    @Test
    fun exec_delegatesToSessionAndReturnsOutput() {
        stubConnect()
        whenever(session.exec("tmux ls")).thenReturn("0: bash")
        sessionManager.connect("example.com", 22, "user", "secret")

        val output = sessionManager.exec("tmux ls")

        assertEquals("0: bash", output)
        verify(session).exec("tmux ls")
    }

    @Test
    fun disconnect_isIdempotent() {
        stubConnect()
        sessionManager.connect("example.com", 22, "user", "secret")

        sessionManager.disconnect()
        assertFalse(sessionManager.isConnected)

        // Second call must not touch the session again nor throw.
        sessionManager.disconnect()
        verify(session, times(1)).disconnect() // still exactly one call
    }

    @Test
    fun disconnect_whenSessionDisconnectThrows_isSwallowed() {
        stubConnect()
        doThrow(RuntimeException("broken pipe")).whenever(session).disconnect()
        sessionManager.connect("example.com", 22, "user", "secret")

        sessionManager.disconnect() // must not throw
        assertFalse(sessionManager.isConnected)
    }
}
