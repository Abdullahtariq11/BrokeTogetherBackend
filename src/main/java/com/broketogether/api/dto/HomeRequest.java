package com.broketogether.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class HomeRequest {

  @NotBlank(message = "Home name is required")
  @Size(min = 1, max = 50, message = "Home name must be between 1 and 50 characters")
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
