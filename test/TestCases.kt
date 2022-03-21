import io.ktor.config.*
import io.ktor.http.*
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
        "firestore.database_url" to ""
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
      handleRequest(HttpMethod.Get, "/health").response.run {
        Assert.assertEquals(HttpStatusCode.OK, status())
        Assert.assertEquals(JSONObject(mapOf("status" to "ok")).toJSONString(), content)
      }
    }
  }

  /** get daigo test */
  @Test
  fun getDaigo() {
    with(engine) {
      // error no auth
      handleRequest(HttpMethod.Get, "/get-dai-go").response.run {
        Assert.assertEquals(HttpStatusCode.Unauthorized, status())
      }

      // error auth
      handleRequest(HttpMethod.Get, "/get-dai-go") {
        addHeader(HttpHeaders.Authorization, "Bearer test2")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.Unauthorized, status())
      }
      handleRequest(HttpMethod.Get, "/get-dai-go?target=努力大事") {
        addHeader(HttpHeaders.Authorization, "Bearer test2")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.Unauthorized, status())
      }

      // error no parameter
      handleRequest(HttpMethod.Get, "/get-dai-go") {
        addHeader(HttpHeaders.Authorization, "Bearer test")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.BadRequest, status())
        Assert.assertEquals(JSONObject(mapOf("text" to "target is empty")).toJSONString(), content)
      }

      // success
      handleRequest(HttpMethod.Get, "/get-dai-go?target=努力大事") {
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
      handleRequest(HttpMethod.Post, "/upsert-dai-go").response.run {
        Assert.assertEquals(HttpStatusCode.Unauthorized, status())
      }

      // error auth
      handleRequest(HttpMethod.Post, "/upsert-dai-go") {
        addHeader(HttpHeaders.Authorization, "Bearer test2")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.Unauthorized, status())
      }

      // error header
      handleRequest(HttpMethod.Post, "/upsert-dai-go") {
        addHeader(HttpHeaders.Authorization, "Bearer test")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.BadRequest, status())
        Assert.assertEquals(JSONObject(mapOf("save" to "parameter is empty")).toJSONString(), content)
      }

      // error no parameter
      handleRequest(HttpMethod.Post, "/upsert-dai-go") {
        addHeader(HttpHeaders.Authorization, "Bearer test")
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(JSONObject().toString())
      }.response.run {
        Assert.assertEquals(HttpStatusCode.BadRequest, status())
        Assert.assertEquals(JSONObject(mapOf("save" to "parameter is empty")).toJSONString(), content)
      }

      // error empty parameter
      handleRequest(HttpMethod.Post, "/upsert-dai-go") {
        addHeader(HttpHeaders.Authorization, "Bearer test")
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(JSONObject().apply {
          put("word", "")
        }.toString())
      }.response.run {
        Assert.assertEquals(HttpStatusCode.BadRequest, status())
        Assert.assertEquals(JSONObject(mapOf("save" to "parameter is empty")).toJSONString(), content)
      }
      handleRequest(HttpMethod.Post, "/upsert-dai-go") {
        addHeader(HttpHeaders.Authorization, "Bearer test")
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(JSONObject().apply {
          put("dai_go", "")
        }.toString())
      }.response.run {
        Assert.assertEquals(HttpStatusCode.BadRequest, status())
        Assert.assertEquals(JSONObject(mapOf("save" to "parameter is empty")).toJSONString(), content)
      }
      handleRequest(HttpMethod.Post, "/upsert-dai-go") {
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
      handleRequest(HttpMethod.Get, "/get-dai-go?target=$target") {
        addHeader(HttpHeaders.Authorization, "Bearer test")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.OK, status())
        Assert.assertEquals(JSONObject(mapOf("text" to "DK")).toJSONString(), content)

        handleRequest(HttpMethod.Post, "/upsert-dai-go") {
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
          handleRequest(HttpMethod.Get, "/get-dai-go?target=$target") {
            addHeader(HttpHeaders.Authorization, "Bearer test")
          }.response.run {
            Assert.assertEquals(HttpStatusCode.OK, status())
            Assert.assertEquals(JSONObject(mapOf("text" to "DKB")).toJSONString(), content)
          }
        }
      }

      // success update
      handleRequest(HttpMethod.Post, "/upsert-dai-go") {
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
        handleRequest(HttpMethod.Get, "/get-dai-go?target=$target") {
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
      handleRequest(HttpMethod.Get, "/get-samples").response.run {
        Assert.assertEquals(HttpStatusCode.Unauthorized, status())
      }

      // error auth
      handleRequest(HttpMethod.Get, "/get-samples") {
        addHeader(HttpHeaders.Authorization, "Bearer test2")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.Unauthorized, status())
      }

      // success
      handleRequest(HttpMethod.Get, "/get-samples") {
        addHeader(HttpHeaders.Authorization, "Bearer test")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.OK, status())
        Assert.assertEquals(JSONArray().apply { addAll(samples) }.toJSONString(), content)
      }
    }
  }

  /** update samples test */
  @Test
  fun updateSamples() {
    with(engine) {
      // error no auth
      handleRequest(HttpMethod.Post, "/update-samples").response.run {
        Assert.assertEquals(HttpStatusCode.Unauthorized, status())
      }

      // error auth
      handleRequest(HttpMethod.Post, "/update-samples") {
        addHeader(HttpHeaders.Authorization, "Bearer test2")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.Unauthorized, status())
      }

      // success
      handleRequest(HttpMethod.Post, "/update-samples") {
        addHeader(HttpHeaders.Authorization, "Bearer test")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.OK, status())
        Assert.assertEquals(JSONArray().apply { addAll(samples) }.toJSONString(), content)
      }
    }
  }
}