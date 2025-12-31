package com.broketogether.api.dto;

public class HomeRequest {

  private String name;

  public HomeRequest() {
  }

  public HomeRequest(String name) {
    this.setName(name);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
