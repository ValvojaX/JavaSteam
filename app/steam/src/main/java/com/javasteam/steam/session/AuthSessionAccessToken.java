package com.javasteam.steam.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;

/** Represents an access token for an authentication session. */
@Getter
public class AuthSessionAccessToken {
  @JsonProperty("iss")
  private String issuer;

  @JsonProperty("sub")
  private String subject;

  @JsonProperty("aud")
  private List<String> audience;

  @JsonProperty("exp")
  private long expiration;

  @JsonProperty("nbf")
  private long notBefore;

  @JsonProperty("iat")
  private long issuedAt;

  @JsonProperty("jti")
  private String jwtId;

  @JsonProperty("oat")
  private String originalAccessToken;

  @JsonProperty("rt_exp")
  private long refreshTokenExpiration;

  @JsonProperty("per") // Not sure about this one
  private int permissions;

  @JsonProperty("ip_subject")
  private String ipSubject;

  @JsonProperty("ip_confirmer")
  private String ipConfirmer;
}
