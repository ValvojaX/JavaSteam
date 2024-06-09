package com.javasteam;

import lombok.Getter;
import lombok.Setter;

/**
 * Response wrapper for Web API responses.
 *
 * @param <T> The type of the response.
 */
@Getter
@Setter
public class Response<T> {
  T response;
}
