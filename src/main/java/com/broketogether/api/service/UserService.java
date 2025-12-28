package com.broketogether.api.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.UserRepository;

@Service
public class UserService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder)
      throws Exception {
    if (userRepository == null) {
      throw new Exception("Repository cannot be null");
    }
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * @return
   */
  public List<User> getAllUser() {
    return userRepository.findAll();
  }

  /**
   * @param email
   * @return
   */
  public Optional<User> getUserByEmail(String email) {
    return this.userRepository.findByEmail(email);
  }

  /**
   * @param user
   * @return
   */
  public User saveUser(User user) {
    if (this.userRepository.existsByEmail(user.getEmail())) {
      throw new RuntimeException("Email already in use!");
    }
    String rawPassword = user.getPassword();
    String encodedPassword = passwordEncoder.encode(rawPassword);
    user.setPassword(encodedPassword);
    return userRepository.save(user);

  }

}
