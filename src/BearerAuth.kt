package net.ambitious.daigoapi

import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationFailedCause
import io.ktor.server.auth.AuthenticationFunction
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.Principal
import io.ktor.server.auth.UnauthorizedResponse
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.response.respond

class BearerAuthenticationProvider internal constructor(
    config: Configuration,
) : AuthenticationProvider(config) {

    private val realm: String = config.realm
    val authenticationFunction = config.authenticationFunction

    class Configuration(name: String?) : Config(name) {
        var authenticationFunction: AuthenticationFunction<BearerPrincipal> = {
            throw NotImplementedError(
                "Bearer auth validate function is not specified. Use bearer { validate(correctToken) } to fix.",
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

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val credentials = call.request.bearerAuthenticationCredentials()
        val principal = credentials?.let { authenticationFunction(call, it) }

        val cause = when {
            credentials == null -> AuthenticationFailedCause.NoCredentials
            principal == null -> AuthenticationFailedCause.InvalidCredentials
            else -> null
        }

        if (cause != null) {
            context.challenge(BearerAuthenticationChallengeKey, cause) { challenge, localCall ->
                localCall.respond(
                    UnauthorizedResponse(
                        HttpAuthHeader.Parameterized(
                            "Bearer",
                            mapOf(HttpAuthHeader.Parameters.Realm to realm),
                        ),
                    ),
                )
                challenge.complete()
            }
        }
        if (principal != null) {
            context.principal(principal)
        }
    }
}

fun AuthenticationConfig.bearer(
    name: String? = null,
    configure: BearerAuthenticationProvider.Configuration.() -> Unit,
) {
    val provider = BearerAuthenticationProvider(BearerAuthenticationProvider.Configuration(name).apply(configure))
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

data class BearerPrincipal(val token: String) : Principal
object EmptyPrincipal : Principal

private val BearerAuthenticationChallengeKey: Any = "BearerAuth"
