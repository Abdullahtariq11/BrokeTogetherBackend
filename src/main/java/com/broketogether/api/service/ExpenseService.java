package com.broketogether.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.login.AccountNotFoundException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.broketogether.api.dto.ExpenseRequest;
import com.broketogether.api.dto.ExpenseResponse;
import com.broketogether.api.dto.ExpenseSplitResponse;
import com.broketogether.api.model.Expense;
import com.broketogether.api.model.ExpenseSplit;
import com.broketogether.api.model.Home;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.ExpenseRepository;
import com.broketogether.api.repository.ExpenseSplitRepository;
import com.broketogether.api.repository.HomeRepository;
import com.broketogether.api.repository.UserRepository;

@Service
public class ExpenseService {

  private final ExpenseRepository expenseRepository;
  private final UserRepository userRepository;
  private final HomeRepository homeRepository;
  private final ExpenseSplitRepository expenseSplitRepository;

  public ExpenseService(ExpenseRepository expenseRepository, UserRepository userRepository,
      HomeRepository homeRepository, ExpenseSplitRepository expenseSplitRepository) {
    this.expenseRepository = expenseRepository;
    this.userRepository = userRepository;
    this.homeRepository = homeRepository;
    this.expenseSplitRepository = expenseSplitRepository;
  }

  /**
   * Creates Expense for user with equal splits among users
   * 
   * @return ExpenseResponse
   * @throws AccountNotFoundException
   */
  @Transactional
  public ExpenseResponse createExpense(ExpenseRequest expenseRequest)
      throws AccountNotFoundException {
    User userDetails = getUserDetails();
    Home home = homeRepository.findById(expenseRequest.getHomeId())
        .orElseThrow(() -> new RuntimeException("Home with this id doesnot exist."));

    if (!home.getMembers().contains(userDetails)) {
      throw new RuntimeException("You are not a member of this home");
    }

    Expense expense = new Expense();
    expense.setAmount(expenseRequest.getAmount());
    expense.setCategory(expenseRequest.getCategory());
    expense.setDescription(expenseRequest.getDescription());
    expense.setHome(home);
    expense.setPayer(userDetails);

    // Equal Splits
    Set<User> members = home.getMembers();
    // Fix 3: Prevent division by zero if home is somehow empty
    if (members.isEmpty())
      throw new RuntimeException("No members in home");

    Double splitAmount = expense.getAmount() / members.size();
    List<ExpenseSplit> expenseSplits = new ArrayList<>();

    for (User member : members) {
      expenseSplits.add(new ExpenseSplit(expense, member, splitAmount));
    }
    expense.setSplits(expenseSplits);

    Expense expenseCreated = expenseRepository.save(expense);

    Map<Long, ExpenseSplitResponse> splitResponses = new HashMap<>();
    for (ExpenseSplit split : expenseCreated.getSplits()) {
      splitResponses.put(split.getUser().getId(),
          new ExpenseSplitResponse(split.getId(), split.getAmount()));
    }

    return new ExpenseResponse(expenseCreated.getId(), expenseCreated.getAmount(),
        expenseCreated.getDescription(), expenseCreated.getCategory(), splitResponses);

  }

  /**
   * @return User logged in
   * @throws AccountNotFoundException
   */
  private User getUserDetails() throws AccountNotFoundException {
    User userDetails = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (userDetails == null) {
      throw new AccountNotFoundException("User not found");
    }
    return userDetails;
  }

}
