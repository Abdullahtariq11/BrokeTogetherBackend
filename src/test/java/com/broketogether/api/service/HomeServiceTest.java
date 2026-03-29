package com.broketogether.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.security.auth.login.AccountNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.broketogether.api.dto.HomeResponse;
import com.broketogether.api.dto.MemberResponse;
import com.broketogether.api.model.Home;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.HomeRepository;
import com.broketogether.api.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
public class HomeServiceTest {

  @Mock
  private HomeRepository homeRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private SecurityContext securityContext;

  @Mock
  private Authentication authentication;

  @InjectMocks
  private HomeService homeService;

  private User testUser;
  private User otherUser;

  @BeforeEach
  public void setup() {
    testUser = new User("Test User", "test@example.com", "password");
    testUser.setId(1L);

    otherUser = new User("Other User", "other@example.com", "password");
    otherUser.setId(2L);

    lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
    lenient().when(authentication.getPrincipal()).thenReturn(testUser);
    SecurityContextHolder.setContext(securityContext);
  }

  // ==================== createHome Tests ====================

  @Nested
  @DisplayName("createHome")
  class CreateHomeTests {

    @Test
    @DisplayName("Should create home successfully")
    public void shouldCreateHomeSuccessfully() throws Exception {
      Home home = new Home();
      home.setId(1L);
      home.setName("My Apartment");
      home.setInviteCode("ABC12345");
      home.setCreator(testUser);

      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(homeRepository.save(any(Home.class))).thenReturn(home);

      HomeResponse response = homeService.createHome("My Apartment");

      assertNotNull(response);
      assertEquals("My Apartment", response.getName());
      assertEquals("ABC12345", response.getInviteCode());
      verify(homeRepository, times(1)).save(any(Home.class));
    }
  }

  // ==================== joinHome Tests ====================

  @Nested
  @DisplayName("joinHome")
  class JoinHomeTests {

    @Test
    @DisplayName("Should join home with valid invite code")
    void shouldJoinHomeWithValidInviteCode() throws AccountNotFoundException {
      Home home = new Home();
      home.setId(1L);
      home.setName("Shared Flat");
      home.setInviteCode("INVITE01");
      home.setMembers(new HashSet<>(Set.of(otherUser)));

      when(homeRepository.findByInviteCode("INVITE01")).thenReturn(Optional.of(home));
      when(homeRepository.save(any(Home.class))).thenReturn(home);

      HomeResponse response = homeService.joinHome("INVITE01");

      assertNotNull(response);
      assertEquals("Shared Flat", response.getName());
      assertTrue(home.getMembers().contains(testUser));
      verify(homeRepository, times(1)).save(home);
    }

    @Test
    @DisplayName("Should throw exception for invalid invite code")
    void shouldThrowExceptionForInvalidInviteCode() {
      when(homeRepository.findByInviteCode("INVALID")).thenReturn(Optional.empty());

      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> homeService.joinHome("INVALID"));

      assertEquals("Invalid invite code.", exception.getMessage());
    }
  }

  // ==================== removeMembers Tests ====================

  @Nested
  @DisplayName("removeMembers")
  class RemoveMembersTests {

    @Test
    @DisplayName("Should remove member when current user is creator")
    void shouldRemoveMemberWhenCreator() throws Exception {
      Home home = new Home();
      home.setId(1L);
      home.setName("My Home");
      home.setCreator(testUser);
      home.setMembers(new HashSet<>(Set.of(testUser, otherUser)));

      when(homeRepository.findById(1L)).thenReturn(Optional.of(home));
      when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));

      assertDoesNotThrow(() -> homeService.removeMembers(1L, 2L));

      assertFalse(home.getMembers().stream().anyMatch(u -> u.getId().equals(2L)));
      verify(homeRepository, times(1)).save(home);
    }

    @Test
    @DisplayName("Should allow user to remove themselves")
    void shouldAllowUserToRemoveThemselves() throws Exception {
      Home home = new Home();
      home.setId(1L);
      home.setName("My Home");
      home.setCreator(otherUser);
      home.setMembers(new HashSet<>(Set.of(testUser, otherUser)));

      when(homeRepository.findById(1L)).thenReturn(Optional.of(home));
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

      assertDoesNotThrow(() -> homeService.removeMembers(1L, 1L));
      verify(homeRepository, times(1)).save(home);
    }

    @Test
    @DisplayName("Should throw exception when non-creator tries to remove another member")
    void shouldThrowExceptionWhenNotCreator() {
      Home home = new Home();
      home.setId(1L);
      home.setName("My Home");
      home.setCreator(otherUser);
      home.setMembers(new HashSet<>(Set.of(testUser, otherUser)));

      when(homeRepository.findById(1L)).thenReturn(Optional.of(home));
      when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));

      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> homeService.removeMembers(1L, 2L));

      assertEquals("You do not have permission to remove this member.", exception.getMessage());
      verify(homeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when home not found")
    void shouldThrowExceptionWhenHomeNotFound() {
      when(homeRepository.findById(999L)).thenReturn(Optional.empty());
      assertThrows(EntityNotFoundException.class,
          () -> homeService.removeMembers(999L, 2L));
    }

    @Test
    @DisplayName("Should throw exception when member not found")
    void shouldThrowExceptionWhenMemberNotFound() {
      Home home = new Home();
      home.setId(1L);
      home.setCreator(testUser);

      when(homeRepository.findById(1L)).thenReturn(Optional.of(home));
      when(userRepository.findById(999L)).thenReturn(Optional.empty());

      assertThrows(AccountNotFoundException.class,
          () -> homeService.removeMembers(1L, 999L));
    }
  }

  // ==================== getUserHomes Tests ====================

  @Nested
  @DisplayName("getUserHomes")
  class GetUserHomesTests {

    @Test
    @DisplayName("Should return all homes where user is a member")
    public void shouldGetUserHomes() throws Exception {
      Home home1 = new Home();
      home1.setId(1L);
      home1.setName("Home 1");
      home1.setInviteCode("CODE1");

      Home home2 = new Home();
      home2.setId(2L);
      home2.setName("Home 2");
      home2.setInviteCode("CODE2");

      when(homeRepository.findByMembersContaining(testUser))
          .thenReturn(Set.of(home1, home2));

      Set<HomeResponse> homes = homeService.getUserHomes();

      assertEquals(2, homes.size());
      verify(homeRepository, times(1)).findByMembersContaining(testUser);
    }

    @Test
    @DisplayName("Should return empty set when user has no homes")
    void shouldReturnEmptySetWhenNoHomes() throws AccountNotFoundException {
      when(homeRepository.findByMembersContaining(testUser)).thenReturn(Set.of());

      Set<HomeResponse> homes = homeService.getUserHomes();

      assertTrue(homes.isEmpty());
    }
  }

  // ==================== getHomeMembers Tests ====================

  @Nested
  @DisplayName("getHomeMembers")
  class GetHomeMembersTests {

    @Test
    @DisplayName("Should return all members when user is a member")
    void shouldReturnAllMembers() throws AccountNotFoundException {
      Home home = new Home();
      home.setId(1L);
      home.setName("Test Home");
      home.setMembers(new HashSet<>(Set.of(testUser, otherUser)));

      when(homeRepository.findById(1L)).thenReturn(Optional.of(home));

      Set<MemberResponse> members = homeService.getHomeMembers(1L);

      assertEquals(2, members.size());
    }

    @Test
    @DisplayName("Should throw exception when user is not a member")
    void shouldThrowExceptionWhenNotMember() {
      Home home = new Home();
      home.setId(1L);
      home.setName("Test Home");
      home.setMembers(new HashSet<>(Set.of(otherUser))); // testUser not in home

      when(homeRepository.findById(1L)).thenReturn(Optional.of(home));

      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> homeService.getHomeMembers(1L));
      assertEquals("You are not a member of this home", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when home not found")
    void shouldThrowExceptionWhenHomeNotFound() {
      when(homeRepository.findById(999L)).thenReturn(Optional.empty());

      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> homeService.getHomeMembers(999L));
      assertEquals("Home not found", exception.getMessage());
    }
  }

  // ==================== getHomeById Tests ====================

  @Nested
  @DisplayName("getHomeById")
  class GetHomeByIdTests {

    @Test
    @DisplayName("Should return home when user is a member")
    void shouldReturnHomeById() throws AccountNotFoundException {
      Home home = new Home();
      home.setId(1L);
      home.setName("Test Home");
      home.setInviteCode("TEST01");
      home.setMembers(new HashSet<>(Set.of(testUser)));

      when(homeRepository.findById(1L)).thenReturn(Optional.of(home));

      HomeResponse response = homeService.getHomeById(1L);

      assertNotNull(response);
      assertEquals("Test Home", response.getName());
      assertEquals("TEST01", response.getInviteCode());
    }

    @Test
    @DisplayName("Should throw exception when user is not a member")
    void shouldThrowExceptionWhenNotMember() {
      Home home = new Home();
      home.setId(1L);
      home.setName("Test Home");
      home.setInviteCode("TEST01");
      home.setMembers(new HashSet<>(Set.of(otherUser)));

      when(homeRepository.findById(1L)).thenReturn(Optional.of(home));

      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> homeService.getHomeById(1L));
      assertEquals("You are not a member of this home", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when home not found")
    void shouldThrowExceptionWhenHomeNotFound() {
      when(homeRepository.findById(999L)).thenReturn(Optional.empty());

      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> homeService.getHomeById(999L));
      assertEquals("Home not found", exception.getMessage());
    }
  }

  // ==================== renameHome Tests ====================

  @Nested
  @DisplayName("renameHome")
  class RenameHomeTests {

    @Test
    @DisplayName("Should rename home when user is creator")
    void shouldRenameHomeWhenCreator() throws AccountNotFoundException {
      Home home = new Home();
      home.setId(1L);
      home.setName("Old Name");
      home.setInviteCode("CODE1");
      home.setCreator(testUser);
      home.setMembers(new HashSet<>(Set.of(testUser)));

      when(homeRepository.findById(1L)).thenReturn(Optional.of(home));
      when(homeRepository.save(any(Home.class))).thenAnswer(inv -> inv.getArgument(0));

      HomeResponse response = homeService.renameHome(1L, "New Name");

      assertNotNull(response);
      assertEquals("New Name", response.getName());
      verify(homeRepository, times(1)).save(home);
    }

    @Test
    @DisplayName("Should throw exception when user is not creator")
    void shouldThrowExceptionWhenNotCreator() {
      Home home = new Home();
      home.setId(1L);
      home.setName("Home");
      home.setCreator(otherUser); // testUser is NOT the creator
      home.setMembers(new HashSet<>(Set.of(testUser, otherUser)));

      when(homeRepository.findById(1L)).thenReturn(Optional.of(home));

      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> homeService.renameHome(1L, "New Name"));
      assertEquals("You do not have permission to rename this home.", exception.getMessage());
      verify(homeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when home not found")
    void shouldThrowExceptionWhenHomeNotFound() {
      when(homeRepository.findById(999L)).thenReturn(Optional.empty());

      assertThrows(RuntimeException.class,
          () -> homeService.renameHome(999L, "Name"));
    }
  }

  // ==================== leaveHome Tests ====================

  @Nested
  @DisplayName("leaveHome")
  class LeaveHomeTests {

    @Test
    @DisplayName("Should allow non-creator to leave home")
    void shouldAllowNonCreatorToLeave() throws AccountNotFoundException {
      Home home = new Home();
      home.setId(1L);
      home.setName("Home");
      home.setCreator(otherUser); // testUser is NOT the creator
      home.setMembers(new HashSet<>(Set.of(testUser, otherUser)));

      when(homeRepository.findById(1L)).thenReturn(Optional.of(home));

      assertDoesNotThrow(() -> homeService.leaveHome(1L));

      assertFalse(home.getMembers().stream().anyMatch(u -> u.getId().equals(1L)));
      verify(homeRepository, times(1)).save(home);
    }

    @Test
    @DisplayName("Should throw exception when creator tries to leave")
    void shouldThrowExceptionWhenCreatorTriesToLeave() {
      Home home = new Home();
      home.setId(1L);
      home.setName("Home");
      home.setCreator(testUser); // testUser IS the creator
      home.setMembers(new HashSet<>(Set.of(testUser, otherUser)));

      when(homeRepository.findById(1L)).thenReturn(Optional.of(home));

      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> homeService.leaveHome(1L));
      assertTrue(exception.getMessage().contains("creator cannot leave"));
    }

    @Test
    @DisplayName("Should throw exception when user is not a member")
    void shouldThrowExceptionWhenNotMember() {
      Home home = new Home();
      home.setId(1L);
      home.setCreator(otherUser);
      home.setMembers(new HashSet<>(Set.of(otherUser))); // testUser not in home

      when(homeRepository.findById(1L)).thenReturn(Optional.of(home));

      assertThrows(RuntimeException.class, () -> homeService.leaveHome(1L));
    }

    @Test
    @DisplayName("Should throw exception when home not found")
    void shouldThrowExceptionWhenHomeNotFound() {
      when(homeRepository.findById(999L)).thenReturn(Optional.empty());

      assertThrows(RuntimeException.class, () -> homeService.leaveHome(999L));
    }
  }

  // ==================== getUserOwnedHome Tests ====================

  @Nested
  @DisplayName("getUserOwnedHome")
  class GetUserOwnedHomeTests {

    @Test
    @DisplayName("Should return homes owned by user")
    void shouldReturnOwnedHomes() throws AccountNotFoundException {
      Home home = new Home();
      home.setId(1L);
      home.setName("My Home");
      home.setInviteCode("OWN001");
      home.setCreator(testUser);

      when(homeRepository.findByCreatorId(1L)).thenReturn(Set.of(home));

      Set<HomeResponse> ownedHomes = homeService.getUserOwnedHome();

      assertEquals(1, ownedHomes.size());
    }

    @Test
    @DisplayName("Should return empty set when user owns no homes")
    void shouldReturnEmptySetWhenNoOwnedHomes() throws AccountNotFoundException {
      when(homeRepository.findByCreatorId(1L)).thenReturn(Set.of());

      Set<HomeResponse> ownedHomes = homeService.getUserOwnedHome();

      assertTrue(ownedHomes.isEmpty());
    }
  }
}
