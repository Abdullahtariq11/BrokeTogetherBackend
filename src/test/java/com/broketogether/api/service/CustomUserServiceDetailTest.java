package com.broketogether.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.broketogether.api.model.User;
import com.broketogether.api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class CustomUserServiceDetailTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private CustomUserServiceDetail customUserServiceDetail;

  @Test
  @DisplayName("Should return UserDetails when user exists")
  void shouldReturnUserDetailsWhenUserExists() {
    User user = new User("Test User", "test@example.com", "password");
    user.setId(1L);

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

    UserDetails result = customUserServiceDetail.loadUserByUsername("test@example.com");

    assertNotNull(result);
    assertEquals("test@example.com", result.getUsername());
    verify(userRepository, times(1)).findByEmail("test@example.com");
  }

  @Test
  @DisplayName("Should throw UsernameNotFoundException when user not found")
  void shouldThrowExceptionWhenUserNotFound() {
    when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

    UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
        () -> customUserServiceDetail.loadUserByUsername("missing@example.com"));

    assertEquals("User not found with email: missing@example.com", exception.getMessage());
    verify(userRepository, times(1)).findByEmail("missing@example.com");
  }
}
