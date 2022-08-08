package net.ambitious.daigoapi

import com.vladsch.flexmark.ext.toc.TocExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {

  val log = log
  val authHeader = environment.config.property("app.auth_header").getString()

  install(StatusPages) {
    exception<IllegalStateException> { call, cause ->
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
      validate(authHeader)
    }
  }

  install(Routing) {
    static {
      resources("/web")
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
  setSampleWords(firestore)

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
        call.respond(mapOf("samples" to samples))
      }

      post("/update-samples") {
        setSampleWords(firestore)
        call.respond(mapOf("samples" to samples))
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

    get("/view/privacy_policy") {
      call.respondText(
        getResourceText("/rules.html").format("プライバシーポリシー", getHtml("/privacy_policy.md")),
        ContentType.Text.Html
      )
    }

    get("/view/terms_of_use") {
      call.respondText(
        getResourceText("/rules.html").format("利用規約", getHtml("/terms_of_use.md")),
        ContentType.Text.Html
      )
    }

    get("/app/rules") {
      val query = call.request.queryParameters
      val textColor = query["textColor"]
      val backColor = query["backColor"]
      val isPrivacyPolicy = query["isPrivacyPolicy"]
      try {
        call.respondText(
          getResourceText("/rules_app.html").format(
            textColor!!,
            backColor!!,
            getHtml(if (isPrivacyPolicy!! == "true") {
              "/privacy_policy.md"
            } else {
              "/terms_of_use.md"
            })
          ),
          ContentType.Text.Html
        )
      } catch (_: Exception) {
        call.respond(HttpStatusCode.NotFound)
      }
    }
  }
}

private fun getHtml(path: String): String {
  val options = MutableDataSet().apply {
    set(Parser.EXTENSIONS, listOf(TocExtension.create()))
  }

  val parser = Parser.builder(options).build()
  val renderer = HtmlRenderer.builder(options).build()

  val document = parser.parse(getResourceText(path))
  return renderer.render(document)
}

private fun getResourceText(path: String): String {
  val lines = arrayListOf<String>()
  object {}.javaClass.getResourceAsStream(path)?.let { stream ->
    BufferedReader(InputStreamReader(stream)).use {
      lines.addAll(it.readLines())
    }
  } ?: throw java.lang.IllegalArgumentException("$path is null")
  return lines.joinToString("\n")
}