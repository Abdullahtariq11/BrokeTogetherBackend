package com.broketogether.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.broketogether.api.config.JwtUtils;
import com.broketogether.api.exception.GlobalExceptionHandler;
import com.broketogether.api.model.User;
import com.broketogether.api.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private UserService userService;

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private JwtUtils jwtUtils;

  @InjectMocks
  private AuthController authController;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(authController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  // ==================== Login Tests ====================

  @Nested
  @DisplayName("POST /api/v1/auth/login")
  class LoginTests {

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfully() throws Exception {
      User testUser = new User("Test User", "test@example.com", "password123");
      testUser.setId(1L);

      Authentication authentication = mock(Authentication.class);
      when(authentication.getPrincipal()).thenReturn(testUser);
      when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
          .thenReturn(authentication);
      when(jwtUtils.generateToken(authentication)).thenReturn("mock-jwt-token");

      mockMvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"username\":\"test@example.com\",\"password\":\"password123\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.token").value("mock-jwt-token"))
          .andExpect(jsonPath("$.username").value("test@example.com"))
          .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    @DisplayName("Should return 400 when email is missing")
    void shouldReturn400WhenEmailMissing() throws Exception {
      mockMvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"password\":\"password123\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when password is missing")
    void shouldReturn400WhenPasswordMissing() throws Exception {
      mockMvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"username\":\"test@example.com\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when email is blank")
    void shouldReturn400WhenEmailBlank() throws Exception {
      mockMvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"username\":\"\",\"password\":\"password123\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when body is empty")
    void shouldReturn400WhenBodyEmpty() throws Exception {
      mockMvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return error when credentials are invalid")
    void shouldReturnErrorWhenCredentialsInvalid() throws Exception {
      when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
          .thenThrow(new BadCredentialsException("Bad credentials"));

      mockMvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"username\":\"test@example.com\",\"password\":\"wrongpassword\"}"))
          .andExpect(status().isInternalServerError());
    }
  }

  // ==================== Register Tests ====================

  @Nested
  @DisplayName("POST /api/v1/auth/register")
  class RegisterTests {

    @Test
    @DisplayName("Should register successfully with valid data")
    void shouldRegisterSuccessfully() throws Exception {
      when(userService.saveUser(any(User.class))).thenReturn(new User("Test User", "test@example.com", "encoded"));

      mockMvc.perform(post("/api/v1/auth/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"name\":\"Test User\",\"username\":\"test@example.com\",\"password\":\"password123\"}"))
          .andExpect(status().isCreated())
          .andExpect(content().string("Account created successfully."));
    }

    @Test
    @DisplayName("Should return 400 when name is missing")
    void shouldReturn400WhenNameMissing() throws Exception {
      mockMvc.perform(post("/api/v1/auth/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"username\":\"test@example.com\",\"password\":\"password123\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when email is missing")
    void shouldReturn400WhenEmailMissing() throws Exception {
      mockMvc.perform(post("/api/v1/auth/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"name\":\"Test User\",\"password\":\"password123\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when password is missing")
    void shouldReturn400WhenPasswordMissing() throws Exception {
      mockMvc.perform(post("/api/v1/auth/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"name\":\"Test User\",\"username\":\"test@example.com\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when email format is invalid")
    void shouldReturn400WhenEmailInvalid() throws Exception {
      mockMvc.perform(post("/api/v1/auth/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"name\":\"Test User\",\"username\":\"not-an-email\",\"password\":\"password123\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when password is too short")
    void shouldReturn400WhenPasswordTooShort() throws Exception {
      mockMvc.perform(post("/api/v1/auth/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"name\":\"Test User\",\"username\":\"test@example.com\",\"password\":\"12345\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when body is empty")
    void shouldReturn400WhenBodyEmpty() throws Exception {
      mockMvc.perform(post("/api/v1/auth/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return error when email already exists")
    void shouldReturnErrorWhenEmailExists() throws Exception {
      when(userService.saveUser(any(User.class)))
          .thenThrow(new IllegalArgumentException("Email already registered"));

      mockMvc.perform(post("/api/v1/auth/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"name\":\"Test User\",\"username\":\"test@example.com\",\"password\":\"password123\"}"))
          .andExpect(status().isBadRequest());
    }
  }
}
