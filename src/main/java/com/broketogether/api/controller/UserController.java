package com.broketogether.api.controller;

import java.util.List;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.broketogether.api.model.User;
import com.broketogether.api.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  public ResponseEntity<List<User>> getUsers() {
    return ResponseEntity.ok(userService.getAllUser());
  }

  @PostMapping
  public ResponseEntity<User> createUser(@RequestBody User user) throws NotFoundException {
    User savedUser = userService.saveUser(user);

    return ResponseEntity.status(201).body(savedUser);
  }

}
