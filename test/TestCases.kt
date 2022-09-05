import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import net.ambitious.daigoapi.module
import net.ambitious.daigoapi.samples
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.junit.*
import org.slf4j.LoggerFactory

class TestCases {
  private lateinit var engine: TestApplicationEngine

  @Before
  fun setUp() {
    engine = TestApplicationEngine(applicationEngineEnvironment {
      config = MapApplicationConfig(
        "app.auth_header" to "test",
        "app.allow_header_host" to "test",
        "firestore.admin_sdk" to ""
      )
      log = LoggerFactory.getLogger("ktor.test")
    }).apply {
      start(wait = false)
      application.module()
    }
  }

  @After
  fun tearDown() {
    engine.stop(0, 0)
  }

  /** health check test */
  @Test
  fun health() {
    with(engine) {
      // error no set header
      handleRequest(HttpMethod.Get, "/health").response.run {
        Assert.assertEquals(HttpStatusCode.NotFound, status())
      }

      headerAddedHandleRequest(HttpMethod.Get, "/health").response.run {
        Assert.assertEquals(HttpStatusCode.OK, status())
        Assert.assertEquals(JSONObject(mapOf("status" to "ok")).toJSONString(), content)
      }
    }
  }

  /** terms_of_use test */
  @Test
  fun termsOfUse() {
    with(engine) {
      headerAddedHandleRequest(HttpMethod.Get, "/terms_of_use").response.run {
        Assert.assertEquals(HttpStatusCode.OK, status())
        Assert.assertEquals(
          JSONObject(mapOf("text" to TERMS_OF_USE_CONTENT)).toJSONString().replace("\\/", "/"),
          content
        )
      }

      headerAddedHandleRequest(HttpMethod.Get, "/view/terms_of_use").response.run {
        Assert.assertEquals(HttpStatusCode.OK, status())
        Assert.assertEquals("<title>利用規約</title>$TERMS_OF_USE_CONTENT", content)
      }
    }
  }

  /** privacy_policy test */
  @Test
  fun privacyPolicy() {
    with(engine) {
      headerAddedHandleRequest(HttpMethod.Get, "/privacy_policy").response.run {
        Assert.assertEquals(HttpStatusCode.OK, status())
        Assert.assertEquals(
          JSONObject(mapOf("text" to PRIVACY_POLICY_CONTENT)).toJSONString().replace("\\/", "/"),
          content
        )
      }

      headerAddedHandleRequest(HttpMethod.Get, "/view/privacy_policy").response.run {
        Assert.assertEquals(HttpStatusCode.OK, status())
        Assert.assertEquals("<title>プライバシーポリシー</title>$PRIVACY_POLICY_CONTENT", content)
      }
    }
  }

  /** app rules test */
  @Test
  fun appRules() {
    with(engine) {
      // error not enough parameters
      headerAddedHandleRequest(HttpMethod.Get, "/app/rules").response.run {
        Assert.assertEquals(HttpStatusCode.NotFound, status())
      }
      headerAddedHandleRequest(HttpMethod.Get, "/app/rules?isPrivacyPolicy=true").response.run {
        Assert.assertEquals(HttpStatusCode.NotFound, status())
      }
      headerAddedHandleRequest(HttpMethod.Get, "/app/rules?backColor=%23FFFFFF").response.run {
        Assert.assertEquals(HttpStatusCode.NotFound, status())
      }
      headerAddedHandleRequest(HttpMethod.Get, "/app/rules?textColor=%23000000").response.run {
        Assert.assertEquals(HttpStatusCode.NotFound, status())
      }
      headerAddedHandleRequest(HttpMethod.Get, "/app/rules?backColor=%23FFFFFF&textColor=%23000000").response.run {
        Assert.assertEquals(HttpStatusCode.NotFound, status())
      }
      headerAddedHandleRequest(HttpMethod.Get, "/app/rules?textColor=%23000000&isPrivacyPolicy=true").response.run {
        Assert.assertEquals(HttpStatusCode.NotFound, status())
      }
      headerAddedHandleRequest(HttpMethod.Get, "/app/rules?backColor=%23FFFFFF&isPrivacyPolicy=true").response.run {
        Assert.assertEquals(HttpStatusCode.NotFound, status())
      }
      
      // show privacy policy
      headerAddedHandleRequest(HttpMethod.Get, "/app/rules?backColor=%23FFFFFF&textColor=%23000000&isPrivacyPolicy=true").response.run {
        Assert.assertEquals(HttpStatusCode.OK, status())
        Assert.assertEquals("#000000<title>#FFFFFF</title>$PRIVACY_POLICY_CONTENT", content)
      }

      // show terms of use
      headerAddedHandleRequest(HttpMethod.Get, "/app/rules?backColor=%23FFFFFF&textColor=%23000000&isPrivacyPolicy=false").response.run {
        Assert.assertEquals(HttpStatusCode.OK, status())
        Assert.assertEquals("#000000<title>#FFFFFF</title>$TERMS_OF_USE_CONTENT", content)
      }
    }
  }

  /** get daigo test */
  @Test
  fun getDaigo() {
    with(engine) {
      // error no auth
      headerAddedHandleRequest(HttpMethod.Get, "/get-dai-go").response.run {
        Assert.assertEquals(HttpStatusCode.Unauthorized, status())
      }

      // error auth
      headerAddedHandleRequest(HttpMethod.Get, "/get-dai-go") {
        addHeader(HttpHeaders.Authorization, "Bearer test2")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.Unauthorized, status())
      }
      headerAddedHandleRequest(HttpMethod.Get, "/get-dai-go?target=努力大事") {
        addHeader(HttpHeaders.Authorization, "Bearer test2")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.Unauthorized, status())
      }

      // error no parameter
      headerAddedHandleRequest(HttpMethod.Get, "/get-dai-go") {
        addHeader(HttpHeaders.Authorization, "Bearer test")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.BadRequest, status())
        Assert.assertEquals(JSONObject(mapOf("text" to "target is empty")).toJSONString(), content)
      }

      // success
      headerAddedHandleRequest(HttpMethod.Get, "/get-dai-go?target=努力大事") {
        addHeader(HttpHeaders.Authorization, "Bearer test")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.OK, status())
        Assert.assertEquals(JSONObject(mapOf("text" to "DD")).toJSONString(), content)
      }
    }
  }

  /** upsert daigo test */
  @Test
  fun upsertDaigo() {
    with(engine) {
      // error no auth
      headerAddedHandleRequest(HttpMethod.Post, "/upsert-dai-go").response.run {
        Assert.assertEquals(HttpStatusCode.Unauthorized, status())
      }

      // error auth
      headerAddedHandleRequest(HttpMethod.Post, "/upsert-dai-go") {
        addHeader(HttpHeaders.Authorization, "Bearer test2")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.Unauthorized, status())
      }

      // error header
      headerAddedHandleRequest(HttpMethod.Post, "/upsert-dai-go") {
        addHeader(HttpHeaders.Authorization, "Bearer test")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.BadRequest, status())
        Assert.assertEquals(JSONObject(mapOf("save" to "parameter is empty")).toJSONString(), content)
      }

      // error no parameter
      headerAddedHandleRequest(HttpMethod.Post, "/upsert-dai-go") {
        addHeader(HttpHeaders.Authorization, "Bearer test")
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(JSONObject().toString())
      }.response.run {
        Assert.assertEquals(HttpStatusCode.BadRequest, status())
        Assert.assertEquals(JSONObject(mapOf("save" to "parameter is empty")).toJSONString(), content)
      }

      // error empty parameter
      headerAddedHandleRequest(HttpMethod.Post, "/upsert-dai-go") {
        addHeader(HttpHeaders.Authorization, "Bearer test")
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(JSONObject().apply {
          put("word", "")
        }.toString())
      }.response.run {
        Assert.assertEquals(HttpStatusCode.BadRequest, status())
        Assert.assertEquals(JSONObject(mapOf("save" to "parameter is empty")).toJSONString(), content)
      }
      headerAddedHandleRequest(HttpMethod.Post, "/upsert-dai-go") {
        addHeader(HttpHeaders.Authorization, "Bearer test")
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(JSONObject().apply {
          put("dai_go", "")
        }.toString())
      }.response.run {
        Assert.assertEquals(HttpStatusCode.BadRequest, status())
        Assert.assertEquals(JSONObject(mapOf("save" to "parameter is empty")).toJSONString(), content)
      }
      headerAddedHandleRequest(HttpMethod.Post, "/upsert-dai-go") {
        addHeader(HttpHeaders.Authorization, "Bearer test")
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(JSONObject().apply {
          put("word", "")
          put("dai_go", "")
        }.toString())
      }.response.run {
        Assert.assertEquals(HttpStatusCode.BadRequest, status())
        Assert.assertEquals(JSONObject(mapOf("save" to "parameter is empty")).toJSONString(), content)
      }

      // success insert
      val target = "大好物"
      headerAddedHandleRequest(HttpMethod.Get, "/get-dai-go?target=$target") {
        addHeader(HttpHeaders.Authorization, "Bearer test")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.OK, status())
        Assert.assertEquals(JSONObject(mapOf("text" to "DK")).toJSONString(), content)

        headerAddedHandleRequest(HttpMethod.Post, "/upsert-dai-go") {
          addHeader(HttpHeaders.Authorization, "Bearer test")
          addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
          setBody(JSONObject().apply {
            put("word", target)
            put("dai_go", "DKB")
          }.toString())
        }.response.run {
          Assert.assertEquals(HttpStatusCode.OK, status())
          Assert.assertEquals(JSONObject(mapOf("save" to "success")).toJSONString(), content)

          Thread.sleep(2000)
          headerAddedHandleRequest(HttpMethod.Get, "/get-dai-go?target=$target") {
            addHeader(HttpHeaders.Authorization, "Bearer test")
          }.response.run {
            Assert.assertEquals(HttpStatusCode.OK, status())
            Assert.assertEquals(JSONObject(mapOf("text" to "DKB")).toJSONString(), content)
          }
        }
      }

      // success update
      headerAddedHandleRequest(HttpMethod.Post, "/upsert-dai-go") {
        addHeader(HttpHeaders.Authorization, "Bearer test")
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(JSONObject().apply {
          put("word", target)
          put("dai_go", "DKB2")
        }.toString())
      }.response.run {
        Assert.assertEquals(HttpStatusCode.OK, status())
        Assert.assertEquals(JSONObject(mapOf("save" to "success")).toJSONString(), content)

        Thread.sleep(2000)
        headerAddedHandleRequest(HttpMethod.Get, "/get-dai-go?target=$target") {
          addHeader(HttpHeaders.Authorization, "Bearer test")
        }.response.run {
          Assert.assertEquals(HttpStatusCode.OK, status())
          Assert.assertEquals(JSONObject(mapOf("text" to "DKB2")).toJSONString(), content)
        }
      }
    }
  }

  /** get samples test */
  @Test
  fun getSamples() {
    with(engine) {
      // error no auth
      headerAddedHandleRequest(HttpMethod.Get, "/get-samples").response.run {
        Assert.assertEquals(HttpStatusCode.Unauthorized, status())
      }

      // error auth
      headerAddedHandleRequest(HttpMethod.Get, "/get-samples") {
        addHeader(HttpHeaders.Authorization, "Bearer test2")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.Unauthorized, status())
      }

      // success
      headerAddedHandleRequest(HttpMethod.Get, "/get-samples") {
        addHeader(HttpHeaders.Authorization, "Bearer test")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.OK, status())
        Assert.assertEquals(JSONObject(mapOf("samples" to JSONArray().apply { addAll(samples) })).toJSONString(), content)
      }
    }
  }

  /** update samples test */
  @Test
  fun updateSamples() {
    with(engine) {
      // error no auth
      headerAddedHandleRequest(HttpMethod.Post, "/update-samples").response.run {
        Assert.assertEquals(HttpStatusCode.Unauthorized, status())
      }

      // error auth
      headerAddedHandleRequest(HttpMethod.Post, "/update-samples") {
        addHeader(HttpHeaders.Authorization, "Bearer test2")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.Unauthorized, status())
      }

      // success
      headerAddedHandleRequest(HttpMethod.Post, "/update-samples") {
        addHeader(HttpHeaders.Authorization, "Bearer test")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.OK, status())
        Assert.assertEquals(JSONObject(mapOf("samples" to JSONArray().apply { addAll(samples) })).toJSONString(), content)
      }
    }
  }

  private fun TestApplicationEngine.headerAddedHandleRequest(
    method: HttpMethod,
    uri: String,
    setup: TestApplicationRequest.() -> Unit = {}
  ): TestApplicationCall = handleRequest {
    this.uri = uri
    this.method = method
    this.addHeader(HttpHeaders.XForwardedHost, "https://test.com, test")
    setup()
  }
  companion object {
    private const val TERMS_OF_USE_CONTENT = "<h1 id=\"利用規約のテスト\">利用規約のテスト</h1>\n<p>これは <strong>テスト</strong> です！</p>\n"
    private const val PRIVACY_POLICY_CONTENT = "<h1 id=\"プライバシーポリシーのテスト\">プライバシーポリシーのテスト</h1>\n<p>これは <strong>テスト</strong> です！</p>\n"
  }
}