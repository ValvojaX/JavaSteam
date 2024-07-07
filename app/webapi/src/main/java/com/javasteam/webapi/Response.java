package com.javasteam.webapi;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Response wrapper for Web API responses.
 *
 * @param <T> The type of the response.
 */
@Getter
@Setter
@ToString
public class Response<T> {
  T response;
}
