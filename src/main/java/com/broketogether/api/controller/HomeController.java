package com.broketogether.api.controller;

import java.util.Set;

import javax.security.auth.login.AccountNotFoundException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.broketogether.api.dto.HomeRequest;
import com.broketogether.api.dto.HomeResponse;
import com.broketogether.api.dto.JoinRequest;
import com.broketogether.api.dto.MemberResponse;
import com.broketogether.api.service.HomeService;

@RestController
@RequestMapping("/api/v1/homes")
public class HomeController {

  private final HomeService homeService;

  public HomeController(HomeService homeService) {
    this.homeService = homeService;
  }

  @PostMapping
  public ResponseEntity<HomeResponse> create(@RequestBody HomeRequest request)
      throws AccountNotFoundException {
    return ResponseEntity.status(201).body(homeService.createHome(request.getName()));
  }

  @PostMapping("/join")
  public ResponseEntity<HomeResponse> join(@RequestBody JoinRequest request)
      throws AccountNotFoundException {
    return ResponseEntity.ok(homeService.joinHome(request.getInviteCode()));
  }

  @GetMapping("/my-homes")
  public ResponseEntity<Set<HomeResponse>> getHomes() throws AccountNotFoundException {
    return ResponseEntity.ok(homeService.getUserHomes());
  }

  @GetMapping("/{homeId}/members")
  public ResponseEntity<Set<MemberResponse>> getMembers(@PathVariable Long homeId) {
    Set<MemberResponse> members = homeService.getHomeMembers(homeId);
    return ResponseEntity.ok(members);
  }

  @GetMapping("/{homeId}")
  public ResponseEntity<HomeResponse> getHomeById(@PathVariable Long homeId) {
    return ResponseEntity.ok(homeService.getHomeById(homeId));
  }

  @DeleteMapping("/{homeId}/members/{userId}")
  public ResponseEntity<Void> removeMember(@PathVariable Long homeId, @PathVariable Long userId)
      throws AccountNotFoundException {
    homeService.removeMembers(homeId, userId);
    return ResponseEntity.noContent().build();

  }

  @GetMapping("/{homeId}/invite-code")
  public ResponseEntity<String> getInviteCode(@PathVariable Long homeId) {
    HomeResponse home = homeService.getHomeById(homeId);
    return ResponseEntity.ok(home.getInviteCode());
  }

}
