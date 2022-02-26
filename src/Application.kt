package net.ambitious.daigoapi

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

@Suppress("unused")
fun Application.module() {
  install(StatusPages) {
    exception<IllegalStateException> { cause ->
      log.warn("StatusPages Error", cause)
      call.respond(HttpStatusCode.NotFound)
    }
  }
  install(Authentication) {
  }
  routing {
  }
}

fun main(args: Array<String>) {
  embeddedServer(Netty, commandLineEnvironment(args)).start()
}