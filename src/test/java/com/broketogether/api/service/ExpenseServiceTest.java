package com.broketogether.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.security.auth.login.AccountNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.broketogether.api.dto.ExpenseRequest;
import com.broketogether.api.dto.ExpenseResponse;
import com.broketogether.api.dto.ExpenseWithUserRequest;
import com.broketogether.api.model.Expense;
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
    // TODO
  }

  @Test
  void createExpenseEqualSpiltWhenNoArguments() throws AccountNotFoundException {
    ExpenseRequest req = new ExpenseRequest();
    req.setAmount(120.0);
    req.setHomeId(10L);
    req.setDescription("Electricity");

    Home mockHome = new Home();
    mockHome.setId(10L);
    // Add current user + 2 others = 3 total
    User u2 = new User();
    u2.setId(2L);
    User u3 = new User();
    u3.setId(3L);
    mockHome.setMembers(new HashSet<>(Set.of(mockUser, u2, u3)));

    when(homeRepository.findById(10L)).thenReturn(Optional.of(mockHome));
    when(expenseRepository.save(any(Expense.class))).thenAnswer(i -> i.getArguments()[0]);

    // 2. Act
    ExpenseResponse result = expenseService.createExpense(req);

    // 3. Assert
    assertEquals(120.0, result.getAmount());
    assertEquals(3, result.getSplits().size());
    // Check if 120 / 3 = 40
    assertEquals(40.0, result.getSplits().get(mockUser.getId()).getAmount());
    verify(expenseRepository).save(any(Expense.class));
  }

  @Test
  void createExpense_WithSelectedUsers_ShouldSplitOnlyAmongParticipants() throws AccountNotFoundException {
      // 1. Arrange
      ExpenseWithUserRequest req = new ExpenseWithUserRequest();
      req.setAmount(150.0);
      req.setHomeId(10L);
      req.setUserId((Set<Long>) List.of(2L)); // Only User 2 is selected to split with Payer (ID 1)
      req.setDescription("Special Dinner");

      Home mockHome = new Home();
      mockHome.setId(10L);
      
      User user2 = new User(); user2.setId(2L);
      User user3 = new User(); user3.setId(3L); // Resident who isn't part of this meal
      
      // The home has 3 members total
      mockHome.setMembers(new HashSet<>(Set.of(mockUser, user2, user3)));

      when(homeRepository.findById(10L)).thenReturn(Optional.of(mockHome));
      when(userRepository.findAllById(any())).thenReturn(List.of(user2));
      
      // Capture the saved expense to verify splits
      when(expenseRepository.save(any(Expense.class))).thenAnswer(i -> {
          Expense e = i.getArgument(0);
          e.setId(500L);
          return e;
      });

      // 2. Act
      ExpenseResponse resp = expenseService.createExpense(req);

      // 3. Assert
      assertNotNull(resp);
      // 150 / 2 people (Payer + User 2) = 75.0 each
      assertEquals(2, resp.getSplits().size());
      assertEquals(75.0, resp.getSplits().get(2L).getAmount());
      assertEquals(75.0, resp.getSplits().get(1L).getAmount());
      
      // Verify User 3 was correctly ignored
      assertFalse(resp.getSplits().containsKey(3L));
      
      verify(expenseRepository, times(1)).save(any(Expense.class));
  }
  
  @Test
  void createExpenseSplitUsingPercentage() {
    //TODO
    //percentage mentioned along with users 
  }

}
