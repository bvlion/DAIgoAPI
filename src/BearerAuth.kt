package net.ambitious.daigoapi

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.auth.*
import io.ktor.request.*
import io.ktor.response.*

class BearerAuthenticationProvider internal constructor(
  configuration: Configuration
) : AuthenticationProvider(configuration) {

  val realm: String = configuration.realm
  val authenticationFunction = configuration.authenticationFunction

  class Configuration(name: String?) : AuthenticationProvider.Configuration(name) {
    var authenticationFunction: AuthenticationFunction<BearerPrincipal> = {
      throw NotImplementedError(
        "Bearer auth validate function is not specified. Use bearer { validate(correctToken) } to fix."
      )
    }

    var realm: String = "Ktor Server"

    fun validate(correctToken: String) {
      authenticationFunction = {
        if (it.token == correctToken) {
          EmptyPrincipal
        } else {
          null
        }
      }
    }
  }
}

fun Authentication.Configuration.bearer(
  name: String? = null,
  configure: BearerAuthenticationProvider.Configuration.() -> Unit
) {
  val provider = BearerAuthenticationProvider(BearerAuthenticationProvider.Configuration(name).apply(configure))
  val realm = provider.realm
  val authenticate = provider.authenticationFunction

  provider.pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
    val credentials = call.request.bearerAuthenticationCredentials()
    val principal = credentials?.let { authenticate(call, it) }

    val cause = when {
      credentials == null -> AuthenticationFailedCause.NoCredentials
      principal == null -> AuthenticationFailedCause.InvalidCredentials
      else -> null
    }

    if (cause != null) {
      context.challenge(BearerAuthenticationChallengeKey, cause) {
        call.respond(
          UnauthorizedResponse(
            HttpAuthHeader.Parameterized(
              "Bearer",
              mapOf(HttpAuthHeader.Parameters.Realm to realm)
            )
          )
        )
        it.complete()
      }
    }
    if (principal != null) {
      context.principal(principal)
    }
  }

  register(provider)
}

fun ApplicationRequest.bearerAuthenticationCredentials(): BearerPrincipal? {
  when (val authHeader = parseAuthorizationHeader()) {
    is HttpAuthHeader.Single -> {
      if (!authHeader.authScheme.equals("Bearer", ignoreCase = true)) {
        return null
      }

      return BearerPrincipal(authHeader.blob)
    }
    else -> return null
  }
}

data class BearerPrincipal(val token: String): Principal
object EmptyPrincipal : Principal

private val BearerAuthenticationChallengeKey: Any = "BearerAuth"