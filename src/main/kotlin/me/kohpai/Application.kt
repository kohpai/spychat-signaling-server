package me.kohpai

import io.ktor.server.application.*
import me.kohpai.plugins.*
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    Security.addProvider(BouncyCastleProvider())
    configureSockets()
    configureRouting()
}
