package com.broketogether.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.security.auth.login.AccountNotFoundException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.broketogether.api.dto.ExpenseRequest;
import com.broketogether.api.dto.ExpenseResponse;
import com.broketogether.api.dto.ExpenseSplitResponse;
import com.broketogether.api.dto.ExpenseWithUserRequest;
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

    // Check if any member in the home has an ID matching the current user's ID
    boolean isMember = home.getMembers().stream()
        .anyMatch(member -> member.getId().equals(userDetails.getId()));

    if (!isMember) {
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

    BigDecimal splitAmount = expense.getAmount().divide(BigDecimal.valueOf(members.size()), 2,
        RoundingMode.HALF_UP);
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
   * Create an espense and divide it among user defined in the parameter
   * 
   * @param expenseRequest
   * @return
   * @throws AccountNotFoundException
   */
  @Transactional
  public ExpenseResponse createExpense(ExpenseWithUserRequest expenseRequest)
      throws AccountNotFoundException {

    User userDetails = getUserDetails();
    Home home = homeRepository.findById(expenseRequest.getHomeId())
        .orElseThrow(() -> new RuntimeException("Home with this id doesnot exist."));

    // Check if any member in the home has an ID matching the current user's ID
    boolean isMember = home.getMembers().stream()
        .anyMatch(member -> member.getId().equals(userDetails.getId()));

    if (!isMember) {
      throw new RuntimeException("You are not a member of this home");
    }

    if (home.getMembers().size() <= 1) {
      throw new RuntimeException("Not enough members in the home to split.");

    }
    List<User> selectedMembers = userRepository.findAllById(expenseRequest.getUserId());

    Set<User> expenseMembers = new HashSet<>(selectedMembers);
    expenseMembers.add(userDetails);

    Set<Long> homeMemberIds = home.getMembers().stream()
        .map(User::getId)
        .collect(Collectors.toSet());


    for (User member : expenseMembers) {
        if (!homeMemberIds.contains(member.getId())) {
            throw new RuntimeException("User " + member.getId() + " is not a member of this home");
        }
    }

    if (expenseMembers.size() < 2) {
      throw new RuntimeException("A split requires at least two participants.");
    }

    Expense expense = new Expense();
    expense.setAmount(expenseRequest.getAmount());
    expense.setCategory(expenseRequest.getCategory());
    expense.setDescription(expenseRequest.getDescription());
    expense.setHome(home);
    expense.setPayer(userDetails);

    BigDecimal splitAmount = expense.getAmount().divide(BigDecimal.valueOf(expenseMembers.size()),
        2, RoundingMode.HALF_UP);
    List<ExpenseSplit> expenseSplits = new ArrayList<>();

    for (User member : expenseMembers) {
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

  @Transactional(readOnly = true)
  public List<ExpenseResponse> getAllExpensesForHome(Long homeId) throws AccountNotFoundException {
    User userDetails = getUserDetails();
    Home home = homeRepository.findById(homeId)
        .orElseThrow(() -> new RuntimeException("Home not found."));

    boolean isMember = home.getMembers().stream()
        .anyMatch(member -> member.getId().equals(userDetails.getId()));

    if (!isMember) {
      throw new RuntimeException("You are not a member of this home");
    }

    List<Expense> expenses = expenseRepository.findByHomeId(homeId);
    List<ExpenseResponse> expenseResponses = new ArrayList<>();

    for (Expense expense : expenses) {
      Map<Long, ExpenseSplitResponse> splitResponses = new HashMap<>();

      for (ExpenseSplit split : expense.getSplits()) {
        splitResponses.put(split.getUser().getId(),
            new ExpenseSplitResponse(split.getId(), split.getAmount()));
      }

      // Use your constructor for a cleaner look
      ExpenseResponse response = new ExpenseResponse(expense.getId(), expense.getAmount(),
          expense.getDescription(), expense.getCategory(), splitResponses);

      expenseResponses.add(response);
    }

    return expenseResponses;
  }

  @Transactional(readOnly = true)
  public ExpenseResponse getExpenseById(Long expenseId) throws AccountNotFoundException {
    Expense expense = expenseRepository.findById(expenseId)
        .orElseThrow(() -> new RuntimeException("No expense exists for this id"));

    User userDetails = getUserDetails();

    boolean isMember = expense.getHome().getMembers().stream()
        .anyMatch(member -> member.getId().equals(userDetails.getId()));

    if (!isMember) {
      throw new RuntimeException("You are not a member of this home");
    }

    Map<Long, ExpenseSplitResponse> splitResponses = expense.getSplits().stream()
        .collect(Collectors.toMap(split -> split.getUser().getId(),
            split -> new ExpenseSplitResponse(split.getId(), split.getAmount())));

    return new ExpenseResponse(expense.getId(), expense.getAmount(), expense.getDescription(),
        expense.getCategory(), splitResponses);
  }

  @Transactional(readOnly = true)
  public Map<Long, BigDecimal> getHomeBalances(Long homeId) throws AccountNotFoundException {
    Home home = homeRepository.findById(homeId)
        .orElseThrow(() -> new RuntimeException("Home with this id does not exist."));

    Map<Long, BigDecimal> balances = new HashMap<>();
    // Initialize all members with 0.00
    for (User member : home.getMembers()) {
      balances.put(member.getId(), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    List<Expense> expenses = expenseRepository.findByHomeId(homeId);

    for (Expense expense : expenses) {
      Long payerId = expense.getPayer().getId();

      for (ExpenseSplit split : expense.getSplits()) {
        Long memberId = split.getUser().getId();
        BigDecimal splitAmount = split.getAmount();

        // LOGIC: We only care about splits where someone paid for someone ELSE
        if (!memberId.equals(payerId)) {
          // 1. The Payer is OWE this money (+)
          balances.put(payerId, balances.get(payerId).add(splitAmount));
          
          // 2. The Member OWES this money (-)
          balances.put(memberId, balances.get(memberId).subtract(splitAmount));
        }
      }
    }
    return balances;
  }
  
  @Transactional
  public void deleteExpense(Long expenseId) throws AccountNotFoundException {
      User userDetails = getUserDetails();
      Expense expense = expenseRepository.findById(expenseId)
          .orElseThrow(() -> new RuntimeException("Expense not found"));

      // Security: Only the person who paid for it can delete it
      if (!expense.getPayer().getId().equals(userDetails.getId())) {
          throw new RuntimeException("Only the payer can delete this expense");
      }

      expenseRepository.delete(expense);
  }
  
  /**
   * Records a direct payment from the logged-in user to another member.
   * This effectively reduces the debt between them.
   */
  @Transactional
  public ExpenseResponse settleUp(Long homeId, Long payeeId, BigDecimal amount) throws AccountNotFoundException {
      User payer = getUserDetails(); // The person paying the money
      User payee = userRepository.findById(payeeId)
          .orElseThrow(() -> new RuntimeException("Payee not found"));
      Home home = homeRepository.findById(homeId)
          .orElseThrow(() -> new RuntimeException("Home not found"));

      // Validation: Both must be in the same home
      boolean isPayerInHome = home.getMembers().stream().anyMatch(m -> m.getId().equals(payer.getId()));
      boolean isPayeeInHome = home.getMembers().stream().anyMatch(m -> m.getId().equals(payee.getId()));
      
      if (!isPayerInHome || !isPayeeInHome) {
          throw new RuntimeException("Both users must be members of the same home to settle up");
      }

      Expense settlement = new Expense();
      settlement.setAmount(amount);
      settlement.setCategory("SETTLEMENT"); // Hardcoded category
      settlement.setDescription("Payment from " + payer.getName() + " to " + payee.getName());
      settlement.setHome(home);
      settlement.setPayer(payer);

      // Create a single split for the Payee
      // Note: We DON'T include the payer in this split because they are the one providing the 100% "Credit"
      ExpenseSplit split = new ExpenseSplit(settlement, payee, amount);
      settlement.setSplits(List.of(split));

      Expense saved = expenseRepository.save(settlement);
      
      // Return response
      Map<Long, ExpenseSplitResponse> splitResponses = Map.of(
          payee.getId(), new ExpenseSplitResponse(saved.getSplits().get(0).getId(), amount)
      );

      return new ExpenseResponse(saved.getId(), saved.getAmount(), 
          saved.getDescription(), saved.getCategory(), splitResponses);
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
