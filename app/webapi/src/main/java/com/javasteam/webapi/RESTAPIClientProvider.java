package com.javasteam.webapi;

import java.net.URI;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

public interface RESTAPIClientProvider {
  static <T> T getRESTAPIClient(Class<T> clazz, String baseUri) {
    return RestClientBuilder.newBuilder().baseUri(URI.create(baseUri)).build(clazz);
  }

  static <T> T getRESTAPIClient(Class<T> clazz) {
    return getRESTAPIClient(clazz, "https://api.steampowered.com");
  }
}
