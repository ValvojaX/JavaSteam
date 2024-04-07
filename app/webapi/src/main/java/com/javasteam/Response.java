package com.javasteam;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Response<T> {
  T response;
}
