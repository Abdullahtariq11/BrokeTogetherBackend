package com.broketogether.api.dto;

import jakarta.validation.constraints.NotBlank;

public class JoinRequest {

  @NotBlank(message = "Invite code is required")
  private String inviteCode;

  public JoinRequest() {
  }

  public JoinRequest(String inviteCode) {
    this.inviteCode = inviteCode;
  }

  public String getInviteCode() {
    return inviteCode;
  }

  public void setInviteCode(String inviteCode) {
    this.inviteCode = inviteCode;
  }

}
