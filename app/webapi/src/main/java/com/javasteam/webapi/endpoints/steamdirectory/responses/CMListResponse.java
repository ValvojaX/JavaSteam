package com.javasteam.webapi.endpoints.steamdirectory.responses;

import com.javasteam.webapi.endpoints.steamdirectory.models.SteamCMServer;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CMListResponse {
  private List<SteamCMServer> serverlist;
  private List<SteamCMServer> serverlistWebsockets;
  private Integer result;
  private String message;
}
