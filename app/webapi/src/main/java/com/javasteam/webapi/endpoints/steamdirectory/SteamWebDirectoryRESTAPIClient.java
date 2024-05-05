package com.javasteam.webapi.endpoints.steamdirectory;

import com.javasteam.webapi.RESTAPIClientProvider;
import com.javasteam.webapi.Response;
import com.javasteam.webapi.endpoints.steamdirectory.responses.CMListResponse;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@Path("/ISteamDirectory")
public interface SteamWebDirectoryRESTAPIClient {
  @GET
  @Path("/GetCMList/v1")
  Response<CMListResponse> getCMList(@QueryParam("cellid") @DefaultValue("0") int cellid);

  static SteamWebDirectoryRESTAPIClient getInstance() {
    return RESTAPIClientProvider.getRESTAPIClient(SteamWebDirectoryRESTAPIClient.class);
  }

  static SteamWebDirectoryRESTAPIClient getInstance(Map<String, Object> headers) {
    return RESTAPIClientProvider.getRESTAPIClient(SteamWebDirectoryRESTAPIClient.class, headers);
  }
}
