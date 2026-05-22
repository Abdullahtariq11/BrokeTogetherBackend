package com.broketogether.api.dto;

public class HomeResponse {

  public Long id;
  public String name;
  public String inviteCode;
  public Long creatorId;

  public HomeResponse(Long id, String name, String inviteCode, Long creatorId) {
    this.id = id;
    this.name = name;
    this.inviteCode = inviteCode;
    this.creatorId = creatorId;
  }

  public Long getCreatorId() {
    return creatorId;
  }

  public void setCreatorId(Long creatorId) {
    this.creatorId = creatorId;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the inviteCode
   */
  public String getInviteCode() {
    return inviteCode;
  }

  /**
   * @param inviteCode the inviteCode to set
   */
  public void setInviteCode(String inviteCode) {
    this.inviteCode = inviteCode;
  }

}
