package net.ambitious.daigoapi

import com.vladsch.flexmark.ext.toc.TocExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

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

  val credential = environment.config.property("firestore.admin_sdk").getString().let {
    if (it.isEmpty()) {
      null
    } else {
      Base64.getDecoder().decode(it)
    }
  }
  val firestore = firestore(credential)
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
        val param = call.receiveOrNull<SaveRequest>()
        if (param?.word.isNullOrEmpty() || param?.daiGo.isNullOrEmpty()) {
          call.respond(HttpStatusCode.BadRequest, mapOf("save" to "parameter is empty"))
          return@post
        }
        call.respond(save(firestore, param!!, log))
      }

      get("/get-samples") {
        call.respond(samples)
      }

      post("/update-samples") {
        if (credential != null) {
          setSampleWords(firestore)
        }
        call.respond(samples)
      }
    }

    get("/health") {
      call.respond(mapOf("status" to "ok"))
    }

    get("/privacy_policy") {
      call.respond(mapOf("text" to getHtml("/privacy_policy.md")))
    }

    get("/terms_of_use") {
      call.respond(mapOf("text" to getHtml("/terms_of_use.md")))
    }
  }
}

private fun getHtml(path: String): String {
  val mdLines = arrayListOf<String>()
  object {}.javaClass.getResourceAsStream(path)?.let { stream ->
    BufferedReader(InputStreamReader(stream)).use {
      mdLines.addAll(it.readLines())
    }
  } ?: throw java.lang.IllegalArgumentException("$path is null")

  val options = MutableDataSet().apply {
    set(Parser.EXTENSIONS, listOf(TocExtension.create()))
  }

  val parser = Parser.builder(options).build()
  val renderer = HtmlRenderer.builder(options).build()

  val document = parser.parse(mdLines.joinToString("\n"))
  return renderer.render(document)
}

fun main(args: Array<String>) {
  embeddedServer(Netty, commandLineEnvironment(args)).start()
}