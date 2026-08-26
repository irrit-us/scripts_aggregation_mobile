package com.scripthost.ssh

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.ByteArrayOutputStream

/**
 * A single SSH connection, abstracted for testability.
 */
interface SSHSession {
    val isConnected: Boolean
    fun exec(command: String): String
    fun disconnect()
}

/**
 * Factory for opening [SSHSession]s, so tests can substitute fakes.
 */
fun interface SSHSessionFactory {
    fun open(host: String, port: Int, username: String, password: String): SSHSession
}

/**
 * SSH Session Manager - Holds at most one active SSH session.
 *
 * Android-free so it can be unit-tested on the JVM. Thread confinement is the
 * caller's responsibility (the bridge runs all calls on the IO dispatcher).
 */
class SSHSessionManager(
    private val factory: SSHSessionFactory = JschSessionFactory()
) {

    @Volatile
    private var session: SSHSession? = null

    val isConnected: Boolean
        get() = session?.isConnected ?: false

    /**
     * Open a session to [host], replacing any existing session.
     * On failure the session stays null and the exception propagates.
     */
    fun connect(host: String, port: Int, username: String, password: String) {
        disconnect()
        session = factory.open(host, port, username, password)
    }

    /**
     * Run [command] on the active session and return its output.
     */
    fun exec(command: String): String {
        val current = session ?: throw IllegalStateException("Not connected")
        return current.exec(command)
    }

    /**
     * Close the active session, if any. Safe to call repeatedly.
     */
    fun disconnect() {
        val current = session ?: return
        session = null
        try {
            current.disconnect()
        } catch (e: Exception) {
            // Disconnect is best-effort; the reference is already cleared.
        }
    }
}

/**
 * JSch-backed [SSHSessionFactory].
 *
 * Host-key checking is disabled ("StrictHostKeyChecking" = "no") because a
 * script sandbox cannot show the interactive host-key confirmation prompt.
 * Scripts are expected to connect to hosts the user already trusts (e.g. a
 * personal tmux box); users should be aware the connection is not protected
 * against MITM attacks.
 */
class JschSessionFactory : SSHSessionFactory {

    override fun open(host: String, port: Int, username: String, password: String): SSHSession {
        val jsch = JSch()
        val jschSession = jsch.getSession(username, host, port)
        jschSession.setPassword(password)
        jschSession.setConfig("StrictHostKeyChecking", "no")
        jschSession.connect(CONNECT_TIMEOUT_MS)
        return JschSSHSession(jschSession)
    }

    private class JschSSHSession(
        private val jschSession: Session
    ) : SSHSession {

        override val isConnected: Boolean
            get() = jschSession.isConnected

        override fun exec(command: String): String {
            val channel = jschSession.openChannel("exec") as ChannelExec
            try {
                channel.setCommand(command)
                val stdout = ByteArrayOutputStream()
                val stderr = ByteArrayOutputStream()
                channel.outputStream = stdout
                channel.setErrStream(stderr)
                channel.connect(CONNECT_TIMEOUT_MS)

                // Wait until the remote command has finished and all output arrived.
                while (!channel.isClosed) {
                    Thread.sleep(POLL_INTERVAL_MS)
                }

                val out = stdout.toString(Charsets.UTF_8.name())
                val err = stderr.toString(Charsets.UTF_8.name())
                return if (err.isNotEmpty()) "$out\n$err" else out
            } finally {
                channel.disconnect()
            }
        }

        override fun disconnect() {
            jschSession.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val POLL_INTERVAL_MS = 100L
    }
}
