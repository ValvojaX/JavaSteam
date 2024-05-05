package com.javasteam.webapi;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CustomHeaderProvider implements ClientRequestFilter {
  private final Map<String, Object> headers;

  @Override
  public void filter(ClientRequestContext clientRequestContext) throws IOException {
    headers.forEach((key, value) -> clientRequestContext.getHeaders().add(key, value));
  }
}
