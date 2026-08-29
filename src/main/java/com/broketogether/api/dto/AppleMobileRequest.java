package com.broketogether.api.dto;

import jakarta.validation.constraints.NotBlank;

public class AppleMobileRequest {

  @NotBlank(message = "Identity token is required")
  private String identityToken;

  // Only present on the user's very first Sign in with Apple authorization;
  // Apple omits it on every subsequent sign-in.
  private String fullName;

  public AppleMobileRequest() {}

  public String getIdentityToken() {
    return identityToken;
  }

  public void setIdentityToken(String identityToken) {
    this.identityToken = identityToken;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }
}
