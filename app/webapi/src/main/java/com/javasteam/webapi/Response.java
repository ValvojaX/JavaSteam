package com.javasteam.webapi;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Response<T> {
  T response;
}
