package me.kohpai

import io.ktor.server.websocket.*

val connections = mutableMapOf<String, DefaultWebSocketServerSession>()