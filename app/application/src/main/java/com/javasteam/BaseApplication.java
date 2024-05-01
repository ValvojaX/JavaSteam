package com.javasteam;

import com.javasteam.protobufs.EnumsClientserver;
import com.javasteam.steam.LoginParameters;
import com.javasteam.steam.SteamClient;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseApplication {
  private static final Dotenv dotenv = Dotenv.load();

  public static void main(String... args) {
    log.info("Application started");
    SteamClient steamClient = new SteamClient();
    steamClient.login(
        LoginParameters.with(dotenv.get("STEAM_USERNAME"), dotenv.get("STEAM_PASSWORD")));

    steamClient.addMessageListener(
        EnumsClientserver.EMsg.k_EMsgClientLogOnResponse_VALUE,
        msg -> {
          log.info("Logged in: {}", msg);
          steamClient.setGamesPlayed(List.of(730));
        });
  }
}
