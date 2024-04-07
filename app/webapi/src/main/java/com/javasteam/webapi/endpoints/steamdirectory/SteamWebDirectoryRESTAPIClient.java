package com.javasteam.webapi.endpoints.steamdirectory;

import com.javasteam.webapi.RESTAPIClientProvider;
import com.javasteam.webapi.Response;
import com.javasteam.webapi.endpoints.steamdirectory.responses.CMListResponse;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
public interface SteamWebDirectoryRESTAPIClient extends RESTAPIClientProvider {
  @GET
  @Path("/ISteamDirectory/GetCMList/v1")
  Response<CMListResponse> getCMList(@QueryParam("cellid") @DefaultValue("0") int cellid);
}
