package com.broketogether.api.dto;

import jakarta.validation.constraints.NotBlank;

public class GoogleMobileRequest {

  @NotBlank(message = "Access token is required")
  private String accessToken;

  public GoogleMobileRequest() {}

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }
}
