package com.broketogether.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EditNameRequest {

  @NotBlank(message = "Name is required")
  @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
  private String name;

  public EditNameRequest() {
  }

  public EditNameRequest(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
