package com.broketogether.api.service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.security.auth.login.AccountNotFoundException;

import com.broketogether.api.exception.ConflictException;
import com.broketogether.api.exception.ForbiddenException;
import com.broketogether.api.exception.ResourceNotFoundException;
import com.broketogether.api.utility.Utility;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.broketogether.api.dto.HomeResponse;
import com.broketogether.api.dto.MemberResponse;
import com.broketogether.api.model.Home;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.HomeRepository;
import com.broketogether.api.repository.UserRepository;

@Service
public class HomeService extends Utility {

  private final HomeRepository homeRepository;
  private final UserRepository userRepository;

  public HomeService(HomeRepository homeRepository, UserRepository userRepository) {
    this.homeRepository = homeRepository;
    this.userRepository = userRepository;
  }

  /**
   * Creates Home for the user.
   * @param name of the house
   *
   * @return HomeResponse entity
   */
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
        homeCreated.getInviteCode(),homeCreated.getCreator().getId());
  }

  /**
   * Regenerate home invite code.
   *
   * @return HomeResponse
   */
  @Transactional
  public HomeResponse regenerateCode(Long homeId) throws AccountNotFoundException {
    Home home = homeRepository.findById(homeId)
            .orElseThrow(() -> new ResourceNotFoundException("Home with this Id not found"));
    User userDetails = getUserDetails();
    if (!home.getCreator().getId().equals(userDetails.getId())) {
      throw new AccessDeniedException("User is not an admin of this home.");
    }
    home.setInviteCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    Home homeSaved = homeRepository.save(home);
    return new HomeResponse(homeSaved.getId(), homeSaved.getName(),
            homeSaved.getInviteCode(),homeSaved.getCreator().getId());
  }

  /**
   * User can join home with the invite code
   * @param inviteCode code to join home.
   *
   * @throws AccountNotFoundException when user account is not found,
   * @return HomeResponse dto
   */
  @Transactional
  public HomeResponse joinHome(String inviteCode) throws AccountNotFoundException {
    Home home = homeRepository.findByInviteCode(inviteCode)
        .orElseThrow(() -> new ResourceNotFoundException("Invalid invite code."));
    User userDetails = getUserDetails();
    if (home.getMembers().contains(userDetails)) {
      throw new ConflictException("User is already a member of this home.");
    }
    home.getMembers().add(userDetails);
    Home homeCreated = homeRepository.save(home);
    return new HomeResponse(homeCreated.getId(), homeCreated.getName(),
        homeCreated.getInviteCode(),homeCreated.getCreator().getId());
  }

  /**
   * Remove members from home
   * @param homeId home id from user being removed
   * @param memberId member's id who is being removed.
   */
  @Transactional
  public void removeMembers(Long homeId, Long memberId) throws AccountNotFoundException {
    Home home = homeRepository.findById(homeId)
        .orElseThrow(() -> new ResourceNotFoundException("Home with this Id not found"));
    User user = userRepository.findById(memberId)
        .orElseThrow(() -> new AccountNotFoundException("User with this Id not found"));
    User currentUser = getUserDetails();

    if (!home.getCreator().getId().equals(currentUser.getId())
        && !currentUser.getId().equals(user.getId())) {
      throw new ForbiddenException("You do not have permission to remove this member.");
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

    return homes.stream()
        .map(h -> new HomeResponse(h.getId(), h.getName(), h.getInviteCode(),h.getCreator().getId()))
        .collect(Collectors.toSet());
  }

  /**
   * Returns all members of a specific home. Only accessible to home members.
   */
  @Transactional(readOnly = true)
  public Set<MemberResponse> getHomeMembers(Long homeId) throws AccountNotFoundException {
    Home home = homeRepository.findById(homeId)
        .orElseThrow(() -> new ResourceNotFoundException("Home not found"));
    verifyMembership(home);
    return home.getMembers().stream()
        .map(h -> new MemberResponse(h.getId(), h.getName()))
        .collect(Collectors.toSet());
  }

  /**
   * Get homes owned by user.
   */
  @Transactional(readOnly = true)
  public Set<HomeResponse> getUserOwnedHome() throws AccountNotFoundException {
    User currentUser = getUserDetails();

    Set<Home> homes = homeRepository.findByCreatorId(currentUser.getId());

    return homes.stream()
        .map(h -> new HomeResponse(h.getId(), h.getName(), h.getInviteCode(),h.getCreator().getId()))
        .collect(Collectors.toSet());
  }

  /**
   * Get home by ID. Only accessible to home members.
   */
  @Transactional(readOnly = true)
  public HomeResponse getHomeById(Long homeId) throws AccountNotFoundException {
    Home home = homeRepository.findById(homeId)
        .orElseThrow(() -> new ResourceNotFoundException("Home not found"));
    verifyMembership(home);
    return new HomeResponse(home.getId(), home.getName(), home.getInviteCode(),home.getCreator().getId());
  }

  /**
   * Rename a home. Only the creator (admin) can rename.
   */
  @Transactional
  public HomeResponse renameHome(Long homeId, String newName) throws AccountNotFoundException {
    Home home = homeRepository.findById(homeId)
        .orElseThrow(() -> new ResourceNotFoundException("Home not found"));
    User currentUser = getUserDetails();

    if (!home.getCreator().getId().equals(currentUser.getId())) {
      throw new ForbiddenException("You do not have permission to rename this home.");
    }

    home.setName(newName);
    Home saved = homeRepository.save(home);
    return new HomeResponse(saved.getId(), saved.getName(), saved.getInviteCode(),saved.getCreator().getId());
  }

  /**
   * Leave a home. The creator cannot leave (they must delete the home instead).
   */
  @Transactional
  public void leaveHome(Long homeId) throws AccountNotFoundException {
    Home home = homeRepository.findById(homeId)
        .orElseThrow(() -> new ResourceNotFoundException("Home not found"));
    User currentUser = getUserDetails();

    verifyMembership(home);

    if (home.getCreator().getId().equals(currentUser.getId())) {
      changeOwnerShip(home,currentUser);
    }

    home.getMembers().removeIf(u -> u.getId().equals(currentUser.getId()));
    homeRepository.save(home);
  }


  private void changeOwnerShip(Home home,User currentUser){
    if(home==null){
      throw new ResourceNotFoundException("Home does not exist.");
    }
    if(currentUser==null){
      throw new ResourceNotFoundException("User does not exist.");
    }
    User member = home
            .getMembers()
            .stream()
            .filter(u -> !u.getId().equals(currentUser.getId()))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("No other member exists. Please delete the home instead."));

    home.setCreator(member);
  }


  /**
   * Verifies the current user is a member of the given home.
   */
  private void verifyMembership(Home home) throws AccountNotFoundException {
    User currentUser = getUserDetails();
    boolean isMember = home.getMembers().stream()
        .anyMatch(m -> m.getId().equals(currentUser.getId()));
    if (!isMember) {
      throw new ForbiddenException("You are not a member of this home");
    }
  }

}
