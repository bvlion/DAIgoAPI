package net.ambitious.daigoapi

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
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

  install(ContentNegotiation) {
    jackson {
      if (environment.config.property("app.pretty_print").getString() == "true") {
        enable(SerializationFeature.INDENT_OUTPUT)
      }
    }
  }

  install(CallLogging) {
    filter { call -> call.request.path() == "/create-dai-go" }
  }

  install(Authentication) {
  }

  routing {
//    authenticate {
      get("/create-dai-go") {
        val target = call.request.queryParameters["target"]
        if (target == null) {
          call.respond(HttpStatusCode.BadRequest, mapOf("text" to "target is empty"))
          return@get
        }
        call.respond(mapOf("text" to Util.createDaiGo(target)))
      }
//    }

    get("/health") {
      call.respond(mapOf("status" to "ok"))
    }
  }
}

fun main(args: Array<String>) {
  embeddedServer(Netty, commandLineEnvironment(args)).start()
}