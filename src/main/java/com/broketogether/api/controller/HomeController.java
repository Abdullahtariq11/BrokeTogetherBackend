package com.broketogether.api.controller;

import javax.security.auth.login.AccountNotFoundException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.broketogether.api.dto.HomeRequest;
import com.broketogether.api.dto.JoinRequest;
import com.broketogether.api.model.Home;
import com.broketogether.api.service.HomeService;

@RestController
@RequestMapping("/api/v1/homes")
public class HomeController {

  private final HomeService homeService;

  public HomeController(HomeService homeService) {
    this.homeService = homeService;
  }

  @PostMapping
  public ResponseEntity<Home> create(@RequestBody HomeRequest request)
      throws AccountNotFoundException {
    return ResponseEntity.status(201).body(homeService.createHome(request.getName()));
  }

  @PostMapping("/join")
  public ResponseEntity<Home> join(@RequestBody JoinRequest request)
      throws AccountNotFoundException {
    return ResponseEntity.ok(homeService.joinHome(request.getInviteCode()));
  }

}
