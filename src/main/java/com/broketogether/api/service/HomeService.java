package com.broketogether.api.service;


import java.util.Set;

import javax.security.auth.login.AccountNotFoundException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.broketogether.api.model.Home;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.HomeRepository;
import com.broketogether.api.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

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

  @Transactional
  public void removeMembers(Long homeId, Long memberId) throws AccountNotFoundException {
    Home home = homeRepository.findById(memberId)
        .orElseThrow(() -> new EntityNotFoundException("Home with this Id not found"));
    User user = userRepository.findById(memberId)
        .orElseThrow(() -> new AccountNotFoundException("User with this Id not found"));
    User currentUser = getUserDetails();

    if (!home.getCreator().getId().equals(currentUser.getId())
        && !currentUser.getId().equals(user.getId())) {
      throw new RuntimeException("You do not have permission to remove this member.");
    }

    home.getMembers().removeIf(u -> u.getId().equals(memberId));
    homeRepository.save(home);
  }

  /**
   * @return Homes where user is member.
   * @throws AccountNotFoundException
   */
  public Set<Home> getUserHomes() throws AccountNotFoundException {
    User userDetails = getUserDetails();
    return userDetails.getHomes();
  }

  /**
   * Returns all members of a specific home.
   */
  public Set<User> getHomeMembers(Long homeId) {
    Home home = homeRepository.findById(homeId)
        .orElseThrow(() -> new RuntimeException("Home not found"));
    return home.getMembers();
  }

  /**
   * @return Homes which user owns.
   * @throws AccountNotFoundException
   */
  public Set<Home> getUserOwnedHome() throws AccountNotFoundException {
    User userDetails = getUserDetails();
    return userDetails.getHomesOwned();
  }

  /**
   * @param homeId
   * @return home to the specified id.
   */
  public Home getHomeById(Long homeId) {
    return homeRepository.findById(homeId)
        .orElseThrow(() -> new RuntimeException("Home not found"));
  }

  /**
   * @return User from the security infromation.
   * @throws AccountNotFoundException
   */
  private User getUserDetails() throws AccountNotFoundException {
    User userDetails = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (userDetails == null) {
      throw new AccountNotFoundException("User not found");
    }
    return userDetails;
  }

}
