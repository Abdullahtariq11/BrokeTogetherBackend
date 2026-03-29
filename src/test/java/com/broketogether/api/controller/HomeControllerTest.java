package com.broketogether.api.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.broketogether.api.dto.HomeResponse;
import com.broketogether.api.dto.MemberResponse;
import com.broketogether.api.exception.GlobalExceptionHandler;
import com.broketogether.api.service.HomeService;

@ExtendWith(MockitoExtension.class)
public class HomeControllerTest {

  private MockMvc mockMvc;

  @Mock
  private HomeService homeService;

  @InjectMocks
  private HomeController homeController;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(homeController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  // ==================== Create Home Tests ====================

  @Nested
  @DisplayName("POST /api/v1/homes")
  class CreateHomeTests {

    @Test
    @DisplayName("Should create home successfully")
    void shouldCreateHomeSuccessfully() throws Exception {
      HomeResponse response = new HomeResponse(1L, "My Apartment", "ABC12345");
      when(homeService.createHome("My Apartment")).thenReturn(response);

      mockMvc.perform(post("/api/v1/homes")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"name\":\"My Apartment\"}"))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.name").value("My Apartment"))
          .andExpect(jsonPath("$.inviteCode").value("ABC12345"));
    }

    @Test
    @DisplayName("Should return 400 when name is missing")
    void shouldReturn400WhenNameMissing() throws Exception {
      mockMvc.perform(post("/api/v1/homes")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when name is blank")
    void shouldReturn400WhenNameBlank() throws Exception {
      mockMvc.perform(post("/api/v1/homes")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"name\":\"\"}"))
          .andExpect(status().isBadRequest());
    }
  }

  // ==================== Join Home Tests ====================

  @Nested
  @DisplayName("POST /api/v1/homes/join")
  class JoinHomeTests {

    @Test
    @DisplayName("Should join home successfully")
    void shouldJoinHomeSuccessfully() throws Exception {
      HomeResponse response = new HomeResponse(1L, "Shared Flat", "INVITE01");
      when(homeService.joinHome("INVITE01")).thenReturn(response);

      mockMvc.perform(post("/api/v1/homes/join")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"inviteCode\":\"INVITE01\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.name").value("Shared Flat"));
    }

    @Test
    @DisplayName("Should return 400 when invite code is missing")
    void shouldReturn400WhenInviteCodeMissing() throws Exception {
      mockMvc.perform(post("/api/v1/homes/join")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return error when invite code is invalid")
    void shouldReturnErrorWhenInviteCodeInvalid() throws Exception {
      when(homeService.joinHome("INVALID"))
          .thenThrow(new RuntimeException("Invalid invite code."));

      mockMvc.perform(post("/api/v1/homes/join")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"inviteCode\":\"INVALID\"}"))
          .andExpect(status().isBadRequest());
    }
  }

  // ==================== Get User Homes Tests ====================

  @Nested
  @DisplayName("GET /api/v1/homes/my-homes")
  class GetUserHomesTests {

    @Test
    @DisplayName("Should return user homes")
    void shouldReturnUserHomes() throws Exception {
      Set<HomeResponse> homes = Set.of(
          new HomeResponse(1L, "Home 1", "CODE1"),
          new HomeResponse(2L, "Home 2", "CODE2"));
      when(homeService.getUserHomes()).thenReturn(homes);

      mockMvc.perform(get("/api/v1/homes/my-homes"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Should return empty set when no homes")
    void shouldReturnEmptySetWhenNoHomes() throws Exception {
      when(homeService.getUserHomes()).thenReturn(Set.of());

      mockMvc.perform(get("/api/v1/homes/my-homes"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }
  }

  // ==================== Get Home By ID Tests ====================

  @Nested
  @DisplayName("GET /api/v1/homes/{homeId}")
  class GetHomeByIdTests {

    @Test
    @DisplayName("Should return home by ID")
    void shouldReturnHomeById() throws Exception {
      HomeResponse response = new HomeResponse(1L, "Test Home", "TEST01");
      when(homeService.getHomeById(1L)).thenReturn(response);

      mockMvc.perform(get("/api/v1/homes/1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.name").value("Test Home"))
          .andExpect(jsonPath("$.inviteCode").value("TEST01"));
    }

    @Test
    @DisplayName("Should return error when home not found")
    void shouldReturnErrorWhenHomeNotFound() throws Exception {
      when(homeService.getHomeById(999L))
          .thenThrow(new RuntimeException("Home not found"));

      mockMvc.perform(get("/api/v1/homes/999"))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 403 when user is not a member")
    void shouldReturn403WhenNotMember() throws Exception {
      when(homeService.getHomeById(1L))
          .thenThrow(new RuntimeException("You are not a member of this home"));

      mockMvc.perform(get("/api/v1/homes/1"))
          .andExpect(status().isForbidden());
    }
  }

  // ==================== Get Home Members Tests ====================

  @Nested
  @DisplayName("GET /api/v1/homes/{homeId}/members")
  class GetHomeMembersTests {

    @Test
    @DisplayName("Should return home members")
    void shouldReturnHomeMembers() throws Exception {
      Set<MemberResponse> members = Set.of(
          new MemberResponse(1L, "User 1"),
          new MemberResponse(2L, "User 2"));
      when(homeService.getHomeMembers(1L)).thenReturn(members);

      mockMvc.perform(get("/api/v1/homes/1/members"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Should return 403 when user is not a member")
    void shouldReturn403WhenNotMember() throws Exception {
      when(homeService.getHomeMembers(1L))
          .thenThrow(new RuntimeException("You are not a member of this home"));

      mockMvc.perform(get("/api/v1/homes/1/members"))
          .andExpect(status().isForbidden());
    }
  }

  // ==================== Remove Member Tests ====================

  @Nested
  @DisplayName("DELETE /api/v1/homes/{homeId}/members/{userId}")
  class RemoveMemberTests {

    @Test
    @DisplayName("Should remove member successfully")
    void shouldRemoveMemberSuccessfully() throws Exception {
      doNothing().when(homeService).removeMembers(1L, 2L);

      mockMvc.perform(delete("/api/v1/homes/1/members/2"))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 403 when not creator")
    void shouldReturn403WhenNotCreator() throws Exception {
      doThrow(new RuntimeException("You do not have permission to remove this member."))
          .when(homeService).removeMembers(1L, 2L);

      mockMvc.perform(delete("/api/v1/homes/1/members/2"))
          .andExpect(status().isForbidden());
    }
  }

  // ==================== Rename Home Tests ====================

  @Nested
  @DisplayName("PUT /api/v1/homes/{homeId}")
  class RenameHomeTests {

    @Test
    @DisplayName("Should rename home successfully")
    void shouldRenameHomeSuccessfully() throws Exception {
      HomeResponse response = new HomeResponse(1L, "New Name", "CODE1");
      when(homeService.renameHome(1L, "New Name")).thenReturn(response);

      mockMvc.perform(put("/api/v1/homes/1")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"name\":\"New Name\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    @DisplayName("Should return 400 when name is blank")
    void shouldReturn400WhenNameBlank() throws Exception {
      mockMvc.perform(put("/api/v1/homes/1")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"name\":\"\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 403 when not creator")
    void shouldReturn403WhenNotCreator() throws Exception {
      when(homeService.renameHome(anyLong(), anyString()))
          .thenThrow(new RuntimeException("You do not have permission to rename this home."));

      mockMvc.perform(put("/api/v1/homes/1")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"name\":\"New Name\"}"))
          .andExpect(status().isForbidden());
    }
  }

  // ==================== Leave Home Tests ====================

  @Nested
  @DisplayName("DELETE /api/v1/homes/{homeId}/leave")
  class LeaveHomeTests {

    @Test
    @DisplayName("Should leave home successfully")
    void shouldLeaveHomeSuccessfully() throws Exception {
      doNothing().when(homeService).leaveHome(1L);

      mockMvc.perform(delete("/api/v1/homes/1/leave"))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return error when creator tries to leave")
    void shouldReturnErrorWhenCreatorTriesToLeave() throws Exception {
      doThrow(new RuntimeException("The home creator cannot leave. Transfer ownership or delete the home."))
          .when(homeService).leaveHome(1L);

      mockMvc.perform(delete("/api/v1/homes/1/leave"))
          .andExpect(status().isInternalServerError());
    }
  }

  // ==================== Get Invite Code Tests ====================

  @Nested
  @DisplayName("GET /api/v1/homes/{homeId}/invite-code")
  class GetInviteCodeTests {

    @Test
    @DisplayName("Should return invite code")
    void shouldReturnInviteCode() throws Exception {
      HomeResponse response = new HomeResponse(1L, "Test Home", "ABC12345");
      when(homeService.getHomeById(1L)).thenReturn(response);

      mockMvc.perform(get("/api/v1/homes/1/invite-code"))
          .andExpect(status().isOk())
          .andExpect(content().string("ABC12345"));
    }
  }
}
