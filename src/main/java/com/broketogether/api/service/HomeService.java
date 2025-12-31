package com.broketogether.api.service;

import javax.security.auth.login.AccountNotFoundException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.broketogether.api.model.Home;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.HomeRepository;
import com.broketogether.api.repository.UserRepository;

@Service
public class HomeService {

  private final HomeRepository homeRepository;
  private final UserRepository userRepository;

  public HomeService(HomeRepository homeRepository, UserRepository userRepository) {
    this.homeRepository = homeRepository;
    this.userRepository = userRepository;
  }

  /**
   * Creates a new home and sets the current user as the owner and first member.
   * 
   * @throws AccountNotFoundException
   */
  @Transactional
  public Home createHome(String name) throws AccountNotFoundException {
    // Find the currently authenticated user
    User userDetails = getUserDetails();

    Home home = new Home();
    home.setName(name);
    home.setCreator(userDetails);
    home.getMembers().add(userDetails);
    return homeRepository.save(home);
  }

  /**
   * Allows a user to join an existing home using its unique invite code.
   * 
   * @throws AccountNotFoundException
   */
  @Transactional
  public Home joinHome(String inviteCode) throws AccountNotFoundException {
    Home home = homeRepository.findByInviteCode(inviteCode)
        .orElseThrow(() -> new RuntimeException("Invalid invite code."));
    User userDetails = getUserDetails();
    home.getMembers().add(userDetails);
    return homeRepository.save(home);

  }

  private User getUserDetails() throws AccountNotFoundException {
    User userDetails = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (userDetails == null) {
      throw new AccountNotFoundException("User not found");
    }
    return userDetails;
  }

}
