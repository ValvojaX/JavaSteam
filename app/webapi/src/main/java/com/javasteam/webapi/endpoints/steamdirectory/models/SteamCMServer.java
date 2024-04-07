package com.javasteam.webapi.endpoints.steamdirectory.models;

import com.javasteam.webapi.endpoints.steamdirectory.deserializers.SteamCMServerDeserializer;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
@JsonbTypeDeserializer(SteamCMServerDeserializer.class)
public class SteamCMServer {
  private final String host;
  private final int port;
}
