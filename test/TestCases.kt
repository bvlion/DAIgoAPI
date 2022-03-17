import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import net.ambitious.daigoapi.module
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
      handleRequest(HttpMethod.Get, "/get-dai-go"){
        addHeader(HttpHeaders.Authorization, "Bearer test2")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.Unauthorized, status())
      }
      handleRequest(HttpMethod.Get, "/get-dai-go?target=努力大事"){
        addHeader(HttpHeaders.Authorization, "Bearer test2")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.Unauthorized, status())
      }

      // error no parameter
      handleRequest(HttpMethod.Get, "/get-dai-go"){
        addHeader(HttpHeaders.Authorization, "Bearer test")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.BadRequest, status())
        Assert.assertEquals(JSONObject(mapOf("text" to "target is empty")).toJSONString(), content)
      }

      // success
      handleRequest(HttpMethod.Get, "/get-dai-go?target=努力大事"){
        addHeader(HttpHeaders.Authorization, "Bearer test")
      }.response.run {
        Assert.assertEquals(HttpStatusCode.OK, status())
        Assert.assertEquals(JSONObject(mapOf("text" to "DD")).toJSONString(), content)
      }
    }
  }
}