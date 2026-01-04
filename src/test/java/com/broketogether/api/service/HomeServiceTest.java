package com.broketogether.api.service;

import com.broketogether.api.dto.HomeResponse;
import com.broketogether.api.model.Home;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.HomeRepository;
import com.broketogether.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @BeforeEach
    public void setup() {
        testUser = new User("Test User", "test@example.com", "password");
        testUser.setId(1L);

        // Mock SecurityContext
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    public void shouldCreateHomeSuccessfully() throws Exception {
        // Arrange
        Home home = new Home();
        home.setId(1L);
        home.setName("My Apartment");
        home.setInviteCode("ABC12345");
        home.setCreator(testUser);

        when(homeRepository.save(any(Home.class))).thenReturn(home);

        // Act
        HomeResponse response = homeService.createHome("My Apartment");

        // Assert
        assertNotNull(response);
        assertEquals("My Apartment", response.getName());
        assertEquals("ABC12345", response.getInviteCode());
        verify(homeRepository, times(1)).save(any(Home.class));
    }

    @Test
    public void shouldGetUserHomes() throws Exception {
        // Arrange
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

        // Act
        Set<HomeResponse> homes = homeService.getUserHomes();

        // Assert
        assertEquals(2, homes.size());
        verify(homeRepository, times(1)).findByMembersContaining(testUser);
    }
}