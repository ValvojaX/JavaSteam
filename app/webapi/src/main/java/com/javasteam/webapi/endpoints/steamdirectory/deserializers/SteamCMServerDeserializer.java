package com.javasteam.webapi.endpoints.steamdirectory.deserializers;

import com.javasteam.webapi.endpoints.steamdirectory.models.SteamCMServer;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;
import java.lang.reflect.Type;

public class SteamCMServerDeserializer implements JsonbDeserializer<SteamCMServer> {
  @Override
  public SteamCMServer deserialize(
      JsonParser jsonParser, DeserializationContext deserializationContext, Type type) {
    String[] parts = jsonParser.getString().split(":");
    return new SteamCMServer(parts[0], Integer.parseInt(parts[1]));
  }
}
