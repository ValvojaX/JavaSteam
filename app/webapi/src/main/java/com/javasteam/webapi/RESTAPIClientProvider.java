package com.javasteam.webapi;

import java.net.URI;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class RESTAPIClientProvider {
  public static final String STEAM_API_BASE_URI = "https://api.steampowered.com";

  public static <T> T getRESTAPIClient(Class<T> clazz, String baseUri) {
    return RestClientBuilder.newBuilder().baseUri(URI.create(baseUri)).build(clazz);
  }

  public static <T> T getRESTAPIClient(
      Class<T> clazz, String baseUri, Map<String, Object> headers) {
    return RestClientBuilder.newBuilder()
        .baseUri(URI.create(baseUri))
        .register(new CustomHeaderProvider(headers))
        .build(clazz);
  }

  public static <T> T getRESTAPIClient(Class<T> clazz) {
    return getRESTAPIClient(clazz, STEAM_API_BASE_URI);
  }

  public static <T> T getRESTAPIClient(Class<T> clazz, Map<String, Object> headers) {
    return getRESTAPIClient(clazz, STEAM_API_BASE_URI, headers);
  }
}
