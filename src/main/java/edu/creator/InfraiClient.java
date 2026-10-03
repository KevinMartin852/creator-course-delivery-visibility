package edu.creator;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.regex.*;

/** Small REST client shared by scheduling, queues, and error capture. */
public final class InfraiClient {
  private final HttpClient http = HttpClient.newHttpClient();
  private final String baseUrl;
  private final String key;

  public InfraiClient(String baseUrl, String key) {
    this.baseUrl = baseUrl;
    this.key = key;
  }

  public String request(String method, String path, String body) throws Exception {
    for (int attempt = 0; attempt < 4; attempt++) {
      HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path))
          .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + key)
          .header("Content-Type", "application/json");
      b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
      HttpResponse<String> response = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
      String envelope = response.body();
      if (envelope.contains("\"ok\":false")) throw new IllegalStateException(envelope);
      if (response.statusCode() == 429 && attempt < 3) {
        String retry = response.headers().firstValue("Retry-After").orElse("1");
        Thread.sleep(Math.min(8000, Long.parseLong(retry) * 1000L * (1L << attempt)));
        continue;
      }
      if (response.statusCode() >= 500) throw new IllegalStateException("server response: " + response.statusCode());
      return envelope;
    }
    throw new IllegalStateException("request retries exhausted");
  }

  public static String value(String envelope, String field) {
    Matcher m = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(envelope);
    return m.find() ? m.group(1) : "";
  }
}
