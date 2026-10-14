package sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Black-box contract test for the sample-maven health app. Boots the real
 * App on an ephemeral port and asserts the /healthz and /version responses
 * defined in STANDARD.md. (HttpServer contexts match all subpaths, so this
 * app has no 404 fallback and none is asserted.)
 */
class AppContractTest {
  private static Process process;
  private static int port;

  @BeforeAll
  static void startApp() throws Exception {
    port = freePort();
    String classpath = System.getProperty("java.class.path");
    ProcessBuilder pb = new ProcessBuilder(
        System.getProperty("java.home") + "/bin/java", "-cp", classpath, "sample.App");
    pb.environment().put("PORT", Integer.toString(port));
    pb.environment().put("SERVICE_NAME", "sample-maven");
    pb.environment().put("APP_VERSION", "9.9.9-test");
    pb.inheritIO();
    process = pb.start();
    waitForReady();
  }

  @AfterAll
  static void stopApp() {
    if (process != null) {
      process.destroy();
    }
  }

  private static int freePort() throws Exception {
    try (var s = new java.net.ServerSocket(0)) {
      return s.getLocalPort();
    }
  }

  private static void waitForReady() throws Exception {
    var deadline = System.currentTimeMillis() + 5000;
    while (System.currentTimeMillis() < deadline) {
      try (var s = new java.net.Socket()) {
        s.connect(new java.net.InetSocketAddress("127.0.0.1", port), 200);
        return;
      } catch (Exception e) {
        Thread.sleep(50);
      }
    }
    throw new IllegalStateException("app did not start in time");
  }

  private HttpResponse<String> get(String path) throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build();
    return client.send(req, HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void healthzReturnsOk() throws Exception {
    var resp = get("/healthz");
    assertEquals(200, resp.statusCode());
    assertTrue(resp.body().contains("\"status\":\"ok\""), resp.body());
  }

  @Test
  void versionReportsServiceAndVersion() throws Exception {
    var resp = get("/version");
    assertEquals(200, resp.statusCode());
    assertTrue(resp.body().contains("\"service\":\"sample-maven\""), resp.body());
    assertTrue(resp.body().contains("\"version\":\"9.9.9-test\""), resp.body());
  }
}
