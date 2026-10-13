package sample;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class App {
  public static void main(String[] args) throws IOException {
    int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    String service = System.getenv().getOrDefault("SERVICE_NAME", "sample-maven");
    String version = System.getenv().getOrDefault("APP_VERSION", "0.1.0");
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext("/healthz", ex -> write(ex, 200, "{\"status\":\"ok\"}"));
    server.createContext("/version", ex -> write(ex, 200,
        "{\"version\":\"" + version + "\",\"service\":\"" + service + "\"}"));
    server.start();
    System.out.println(service + " listening on " + port);
  }

  private static void write(com.sun.net.httpserver.HttpExchange ex, int code, String body)
      throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    ex.getResponseHeaders().add("content-type", "application/json");
    ex.sendResponseHeaders(code, bytes.length);
    try (OutputStream os = ex.getResponseBody()) {
      os.write(bytes);
    }
  }

  private App() {}
}
