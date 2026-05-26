package com.broketogether.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.mockito.ArgumentCaptor;

import com.broketogether.api.dto.UserResponse;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.ExpenseRepository;
import com.broketogether.api.repository.HomeRepository;
import com.broketogether.api.repository.ShoppingItemRepository;
import com.broketogether.api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private HomeRepository homeRepository;

  @Mock
  private ExpenseRepository expenseRepository;

  @Mock
  private ShoppingItemRepository shoppingItemRepository;

  private UserService userService;

  @BeforeEach
  void setUp() throws Exception {
    userService = new UserService(userRepository, passwordEncoder, homeRepository, expenseRepository, shoppingItemRepository);
  }

  // ==================== Constructor Tests ====================

  @Nested
  @DisplayName("Constructor")
  class ConstructorTests {

    @Test
    @DisplayName("Should throw exception when repository is null")
    void shouldThrowExceptionWhenRepositoryIsNull() {
      Exception exception = assertThrows(Exception.class,
          () -> new UserService(null, passwordEncoder, homeRepository, expenseRepository, shoppingItemRepository));

      assertEquals("Repository cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should create service when repository is provided")
    void shouldCreateServiceWhenRepositoryProvided() {
      assertDoesNotThrow(() -> new UserService(userRepository, passwordEncoder, homeRepository, expenseRepository, shoppingItemRepository));
    }
  }

  // ==================== getAllUser Tests ====================

  @Nested
  @DisplayName("getAllUser")
  class GetAllUserTests {

    @Test
    @DisplayName("Should return all users")
    void shouldReturnAllUsers() {
      User user1 = new User("User One", "one@example.com", "password");
      user1.setId(1L);
      User user2 = new User("User Two", "two@example.com", "password");
      user2.setId(2L);

      when(userRepository.findAll()).thenReturn(List.of(user1, user2));

      List<UserResponse> users = userService.getAllUser();

      assertEquals(2, users.size());
      assertEquals("one@example.com", users.get(0).email());
      assertEquals("two@example.com", users.get(1).email());
      verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no users exist")
    void shouldReturnEmptyListWhenNoUsers() {
      when(userRepository.findAll()).thenReturn(Collections.emptyList());

      List<UserResponse> users = userService.getAllUser();

      assertTrue(users.isEmpty());
      verify(userRepository, times(1)).findAll();
    }
  }

  // ==================== getUserByEmail Tests ====================

  @Nested
  @DisplayName("getUserByEmail")
  class GetUserByEmailTests {

    @Test
    @DisplayName("Should return user when email exists")
    void shouldReturnUserWhenEmailExists() {
      User user = new User("Test User", "test@example.com", "password");
      user.setId(1L);

      when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

      Optional<UserResponse> result = userService.getUserByEmail("test@example.com");

      assertTrue(result.isPresent());
      assertEquals(1L, result.get().id());
      assertEquals("Test User", result.get().name());
      assertEquals("test@example.com", result.get().email());
      verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should return empty optional when email not found")
    void shouldReturnEmptyWhenEmailNotFound() {
      when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

      Optional<UserResponse> result = userService.getUserByEmail("missing@example.com");

      assertTrue(result.isEmpty());
      verify(userRepository, times(1)).findByEmail("missing@example.com");
    }
  }

  // ==================== saveUser Tests ====================

  @Nested
  @DisplayName("saveUser")
  class SaveUserTests {

    @Test
    @DisplayName("Should save user with encoded password")
    void shouldSaveUserWithEncodedPassword() {
      User user = new User("New User", "new@example.com", "rawPassword");
      user.setId(1L);

      when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
      when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
      when(userRepository.save(any(User.class))).thenReturn(user);

      UserResponse saved = userService.saveUser(user);

      assertNotNull(saved);
      assertEquals(1L, saved.id());
      assertEquals("New User", saved.name());
      assertEquals("new@example.com", saved.email());
      verify(passwordEncoder, times(1)).encode("rawPassword");
      verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Should throw exception when email already in use")
    void shouldThrowExceptionWhenEmailAlreadyInUse() {
      User user = new User("Duplicate User", "existing@example.com", "password");

      when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> userService.saveUser(user));

      assertEquals("Email already in use!", exception.getMessage());
      verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should set encoded password on user before saving")
    void shouldSetEncodedPasswordOnUser() {
      User user = new User("User", "user@example.com", "myPassword");

      when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
      when(passwordEncoder.encode("myPassword")).thenReturn("$2a$10$encodedHash");
      when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

      userService.saveUser(user);

      ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
      verify(userRepository).save(captor.capture());
      assertEquals("$2a$10$encodedHash", captor.getValue().getPassword());
    }
  }

  // ==================== resetPassword Tests ====================

  @Nested
  @DisplayName("resetPassword")
  class ResetPasswordTests {

    private User mockCurrentUser() {
      User user = new User("Test User", "test@example.com", "encodedOldPassword");
      user.setId(1L);
      return user;
    }

    private void mockSecurityContext(MockedStatic<SecurityContextHolder> securityHolder, User user) {
      SecurityContext context = mock(SecurityContext.class);
      Authentication authentication = mock(Authentication.class);
      securityHolder.when(SecurityContextHolder::getContext).thenReturn(context);
      when(context.getAuthentication()).thenReturn(authentication);
      when(authentication.getPrincipal()).thenReturn(user);
    }

    @Test
    @DisplayName("Should reset password when current password matches")
    void shouldResetPasswordWhenCurrentPasswordMatches() throws Exception {
      User user = mockCurrentUser();

      try (MockedStatic<SecurityContextHolder> securityHolder = mockStatic(SecurityContextHolder.class)) {
        mockSecurityContext(securityHolder, user);
        when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

        userService.resetPassword("oldPassword", "newPassword");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("encodedNewPassword", captor.getValue().getPassword());
      }
    }

    @Test
    @DisplayName("Should throw when current password does not match")
    void shouldThrowWhenCurrentPasswordDoesNotMatch() {
      User user = mockCurrentUser();

      try (MockedStatic<SecurityContextHolder> securityHolder = mockStatic(SecurityContextHolder.class)) {
        mockSecurityContext(securityHolder, user);
        when(passwordEncoder.matches("wrongPassword", "encodedOldPassword")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> userService.resetPassword("wrongPassword", "newPassword"));

        assertEquals("Password entered doesnt match current password.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
      }
    }

    @Test
    @DisplayName("Should encode new password before saving")
    void shouldEncodeNewPasswordBeforeSaving() throws Exception {
      User user = mockCurrentUser();

      try (MockedStatic<SecurityContextHolder> securityHolder = mockStatic(SecurityContextHolder.class)) {
        mockSecurityContext(securityHolder, user);
        when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("$2a$10$newEncodedHash");

        userService.resetPassword("oldPassword", "newPassword");

        verify(passwordEncoder, times(1)).encode("newPassword");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("$2a$10$newEncodedHash", captor.getValue().getPassword());
      }
    }
  }
}
