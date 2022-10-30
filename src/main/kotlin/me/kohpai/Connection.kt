package me.kohpai

import io.ktor.server.websocket.*

val connections = mutableListOf<Connection>()

data class Connection(
    val publicKeyPem: String,
    val session: DefaultWebSocketServerSession
) {
    override fun equals(other: Any?): Boolean {
        val connection = other as Connection
        return publicKeyPem == connection.publicKeyPem
    }

    override fun hashCode(): Int {
        return publicKeyPem.hashCode()
    }
}