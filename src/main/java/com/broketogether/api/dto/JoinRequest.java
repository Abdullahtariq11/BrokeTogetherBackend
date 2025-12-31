package com.broketogether.api.dto;

public class JoinRequest {
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
