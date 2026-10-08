package vn.ticketscenter.support;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

public final class HttpTestClient {
  private final String baseUrl;
  private final HttpClient client =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(5))
          .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
          .build();

  public HttpTestClient(String baseUrl) {
    this.baseUrl = baseUrl.replaceAll("/$", "");
  }

  public HttpResponse<String> get(String path) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(baseUrl + path))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/json")
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  public HttpResponse<String> post(String path, String body, String csrf) throws Exception {
    var builder =
        HttpRequest.newBuilder(URI.create(baseUrl + path))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json");
    if (csrf != null) builder.header("X-CSRF-Token", csrf);
    return client.send(
        builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
        HttpResponse.BodyHandlers.ofString());
  }
}
