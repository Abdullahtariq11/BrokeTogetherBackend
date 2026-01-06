package com.broketogether.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.broketogether.api.model.Home;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.ExpenseRepository;
import com.broketogether.api.repository.HomeRepository;
import com.broketogether.api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

  @Mock
  private ExpenseRepository expenseRepository;
  @Mock
  private HomeRepository homeRepository;
  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private ExpenseService expenseService;

  private User mockUser;
  private Home mockHome;

  @BeforeEach
  void setUp() throws Exception {
    mockUser = new User();
    mockUser.setId(1L);
    mockUser.setEmail("test@test.com");

    mockHome = new Home();
    mockHome.setId(10L);
    mockHome.getMembers().add(mockUser);

    // 2. Mock the Security components
    Authentication authentication = mock(Authentication.class);
    SecurityContext securityContext = mock(SecurityContext.class);
    
    // 3. Define the behavior: Context -> Authentication -> Principal (Our User)
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(mockUser);
    
    // 4. Set the mocked context globally for this test
    SecurityContextHolder.setContext(securityContext);

  }

  @Test
  void createExpenseSucessfully() {
    fail("Not yet implemented");
  }
  
  @Test
  void createExpenseEqualSpiltWhenNoArguments() {
    fail("Not yet implemented");
  }
  
  @Test
  void createExpenseSplitAmongUsersFromParameter() {
    fail("Not yet implemented");
  }
  

}
