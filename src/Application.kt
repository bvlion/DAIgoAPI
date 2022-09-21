package net.ambitious.daigoapi

import com.vladsch.flexmark.ext.toc.TocExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.request.path
import io.ktor.server.request.receiveOrNull
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.*

fun main(args: Array<String>) {
    val logOutDir = "LOG_OUT_DIR"
    if (System.getProperty(logOutDir).isNullOrEmpty()) {
        System.setProperty(logOutDir, ".")
    }
    EngineMain.main(args)
}

fun Application.module() {
    val log = log
    val authHeader = environment.config.property("app.auth_header").getString()
    val allowHeaderHost = environment.config.property("app.allow_header_host").getString()

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
    setWords(firestore)

    routing {
        intercept(ApplicationCallPipeline.Plugins) {
            if (call.request.uri != "/health") {
                log.info(URLDecoder.decode(call.request.uri, Charset.defaultCharset()))
            }
            if (allowHeaderHost.isNotEmpty()) {
                val hosts = call.request.header(HttpHeaders.XForwardedHost)
                    ?.split(",")
                    ?.map { it.trim() } ?: throw IllegalStateException("target host is null")
                if (hosts.none { it == allowHeaderHost }) {
                    throw IllegalStateException("target host [$allowHeaderHost], different hosts [${hosts.joinToString()}]")
                }
            }
        }
        authenticate {
            get("/get-dai-go") {
                val target = call.request.queryParameters["target"]
                if (target == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("text" to "target is empty"))
                    return@get
                }
                call.respond(mapOf("text" to createDaiGo(target, log)))
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
                setWords(firestore)
                call.respond(
                    mapOf(
                        "notExists" to notExists,
                        "samples" to samples
                    )
                )
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
                        getHtml(
                            if (isPrivacyPolicy!! == "true") {
                                "/privacy_policy.md"
                            } else {
                                "/terms_of_use.md"
                            }
                        )
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
