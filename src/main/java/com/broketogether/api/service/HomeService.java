package com.broketogether.api.service;

import java.util.Set;
import java.util.stream.Collectors;

import javax.security.auth.login.AccountNotFoundException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.broketogether.api.dto.HomeResponse;
import com.broketogether.api.dto.MemberResponse;
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

  @Transactional
  public HomeResponse createHome(String name) throws AccountNotFoundException {
    User userDetails = getUserDetails();
    
    User managedUser = userRepository.findById(userDetails.getId()).get();

    Home home = new Home();
    home.setName(name);
    home.setCreator(managedUser);
    home.getMembers().add(userDetails);
    Home homeCreated = homeRepository.save(home);
    return new HomeResponse(homeCreated.getId(), homeCreated.getName(),
        homeCreated.getInviteCode());
  }

  @Transactional
  public HomeResponse joinHome(String inviteCode) throws AccountNotFoundException {
    Home home = homeRepository.findByInviteCode(inviteCode)
        .orElseThrow(() -> new RuntimeException("Invalid invite code."));
    User userDetails = getUserDetails();
    home.getMembers().add(userDetails);
    Home homeCreated = homeRepository.save(home);
    return new HomeResponse(homeCreated.getId(), homeCreated.getName(),
        homeCreated.getInviteCode());
  }

  @Transactional
  public void removeMembers(Long homeId, Long memberId) throws AccountNotFoundException {
    Home home = homeRepository.findById(homeId) 
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
   * Get all homes where user is a member.
   */
  @Transactional(readOnly = true)
  public Set<HomeResponse> getUserHomes() throws AccountNotFoundException {
    User currentUser = getUserDetails();

    Set<Home> homes = homeRepository.findByMembersContaining(currentUser);

    return homes.stream().map(h -> new HomeResponse(h.getId(), h.getName(), h.getInviteCode()))
        .collect(Collectors.toSet());
  }

  /**
   * Returns all members of a specific home.
   */
  @Transactional(readOnly = true)
  public Set<MemberResponse> getHomeMembers(Long homeId) {
    Home home = homeRepository.findById(homeId)
        .orElseThrow(() -> new RuntimeException("Home not found"));
    return home.getMembers().stream().map(h -> new MemberResponse(h.getId(), h.getName()))
        .collect(Collectors.toSet());
  }

  /**
   * Get homes owned by user.
   */
  @Transactional(readOnly = true)
  public Set<HomeResponse> getUserOwnedHome() throws AccountNotFoundException {
    User currentUser = getUserDetails();

    Set<Home> homes = homeRepository.findByCreatorId(currentUser.getId());

    return homes.stream().map(h -> new HomeResponse(h.getId(), h.getName(), h.getInviteCode()))
        .collect(Collectors.toSet());
  }

  @Transactional(readOnly = true)
  public HomeResponse getHomeById(Long homeId) {
    Home home = homeRepository.findById(homeId)
        .orElseThrow(() -> new RuntimeException("Home not found"));
    return new HomeResponse(home.getId(), home.getName(), home.getInviteCode());
  }
  
  

  private User getUserDetails() throws AccountNotFoundException {
    User userDetails = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (userDetails == null) {
      throw new AccountNotFoundException("User not found");
    }
    return userDetails;
  }
}