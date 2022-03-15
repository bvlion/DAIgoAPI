package net.ambitious.daigoapi

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
    jackson()
  }

  install(CallLogging) {
    filter { call -> call.request.path() == "/create-dai-go" }
  }

  install(Authentication) {
    bearer {
      realm = "DAIgoAPI Server"
      validate(environment.config.property("app.auth_header").getString())
    }
  }

  val firestore = firestore(environment.config.property("firestore.database_url").getString())
  setOriginalWords(firestore)

  routing {
    authenticate {
      get("/get-dai-go") {
        val target = call.request.queryParameters["target"]
        if (target == null) {
          call.respond(HttpStatusCode.BadRequest, mapOf("text" to "target is empty"))
          return@get
        }
        call.respond(mapOf("text" to createDaiGo(target)))
      }

      post("/upsert-dai-go") {
        call.respond(save(firestore, call.receive()))
      }
    }

    get("/health") {
      call.respond(mapOf("status" to "ok"))
    }
  }
}

fun main(args: Array<String>) {
  embeddedServer(Netty, commandLineEnvironment(args)).start()
}