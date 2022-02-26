import io.ktor.config.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import org.junit.*
import org.slf4j.LoggerFactory

open class TestBase {
  private lateinit var engine: TestApplicationEngine

  @Before
  fun setUp() {
    engine = TestApplicationEngine(applicationEngineEnvironment {
      config = MapApplicationConfig(
        "app.auth_header" to "test"
      )
      log = LoggerFactory.getLogger("ktor.test")
    }).apply {
      start(wait = false)
    }
  }

  @After
  fun tearDown() {
    engine.stop(0, 0)
  }
}