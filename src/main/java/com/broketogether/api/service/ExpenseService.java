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

import com.broketogether.api.dto.*;
import com.broketogether.api.exception.ForbiddenException;
import com.broketogether.api.exception.ResourceNotFoundException;
import com.broketogether.api.utility.Utility;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.broketogether.api.model.Expense;
import com.broketogether.api.model.ExpenseSplit;
import com.broketogether.api.model.Home;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.ExpenseRepository;
import com.broketogether.api.repository.HomeRepository;
import com.broketogether.api.repository.UserRepository;

@Service
public class ExpenseService extends Utility {

  private final ExpenseRepository expenseRepository;
  private final UserRepository userRepository;
  private final HomeRepository homeRepository;


  public ExpenseService(ExpenseRepository expenseRepository, UserRepository userRepository,
      HomeRepository homeRepository) {
    this.expenseRepository = expenseRepository;
    this.userRepository = userRepository;
    this.homeRepository = homeRepository;
  }

  /**
   * Creates Expense for user with equal splits among users
   * @param expenseRequest expense dto used to create expense.
   *
   * @return ExpenseResponse
   * @throws AccountNotFoundException if there is no account
   */
  @Transactional
  public ExpenseResponse createExpense(ExpenseRequest expenseRequest)
      throws AccountNotFoundException {
    User userDetails = getUserDetails();
    Home home = homeRepository.findById(expenseRequest.getHomeId())
        .orElseThrow(() -> new ResourceNotFoundException("Home with this id does not exist."));

    // Check if any member in the home has an ID matching the current user's ID
    checkUserMemberOfHome(home,userDetails);

    Expense expense = new Expense();
    expense.setAmount(expenseRequest.getAmount());
    expense.setCategory(expenseRequest.getCategory());
    expense.setDescription(expenseRequest.getDescription());
    expense.setHome(home);
    expense.setPayer(userDetails);

    // Equal Splits
    Set<User> members = home.getMembers();
    // Fix 3: Prevent division by zero if home is somehow empty
    if (members.isEmpty()) {
      throw new IllegalArgumentException("No members in home");
    }

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
   * Create an expense and divide it among user defined in the parameter
   * @param expenseRequest dto used to create expense request
   *
   * @return ExpenseResponse
   * @throws AccountNotFoundException when account is not found.
   */
  @Transactional
  public ExpenseResponse createExpense(ExpenseWithUserRequest expenseRequest)
      throws AccountNotFoundException {

    User userDetails = getUserDetails();
    Home home = homeRepository.findById(expenseRequest.getHomeId())
        .orElseThrow(() -> new ResourceNotFoundException("Home with this id does not exist."));

    // Check if any member in the home has an ID matching the current user's ID
    checkUserMemberOfHome(home,userDetails);

    if (home.getMembers().size() <= 1) {
      throw new IllegalArgumentException("Not enough members in the home to split.");

    }
    List<User> selectedMembers = userRepository.findAllById(expenseRequest.getUserId());

    Set<User> expenseMembers = new HashSet<>(selectedMembers);
    expenseMembers.add(userDetails);

    Set<Long> homeMemberIds = home.getMembers().stream()
        .map(User::getId)
        .collect(Collectors.toSet());


    for (User member : expenseMembers) {
      if (!homeMemberIds.contains(member.getId())) {
        throw new ForbiddenException("User " + member.getId() + " is not a member of this home");
      }
    }

    if (expenseMembers.size() < 2) {
      throw new IllegalArgumentException("A split requires at least two participants.");
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
        .orElseThrow(() -> new ResourceNotFoundException("Home not found."));

    // Check if any member in the home has an ID matching the current user's ID
    checkUserMemberOfHome(home,userDetails);

    List<Expense> expenses = expenseRepository.findByHomeId(homeId);
    List<ExpenseResponse> expenseResponses = new ArrayList<>();

    for (Expense expense : expenses) {
      Map<Long, ExpenseSplitResponse> splitResponses = new HashMap<>();

      for (ExpenseSplit split : expense.getSplits()) {
        splitResponses.put(split.getUser().getId(),
            new ExpenseSplitResponse(split.getId(), split.getAmount()));
      }

      ExpenseResponse response = new ExpenseResponse(expense.getId(), expense.getAmount(),
          expense.getDescription(), expense.getCategory(), splitResponses);
      if (expense.getPayer() != null) {
        response.setPayerId(expense.getPayer().getId());
        response.setPayerName(expense.getPayer().getName());
      }

      expenseResponses.add(response);
    }

    return expenseResponses;
  }

  /**
   * Retrieves expense by id.
   * @param expenseId id of the expense
   *
   * @return ExpenseResponse
   * */
  @Transactional(readOnly = true)
  public ExpenseResponse getExpenseById(Long expenseId) throws AccountNotFoundException {
    Expense expense = expenseRepository.findById(expenseId)
        .orElseThrow(() -> new ResourceNotFoundException("No expense exists for this id"));

    User userDetails = getUserDetails();

    // Check if any member in the home has an ID matching the current user's ID
    checkUserMemberOfHome(expense.getHome(),userDetails);

    Map<Long, ExpenseSplitResponse> splitResponses = expense.getSplits().stream()
        .collect(Collectors.toMap(split -> split.getUser().getId(),
            split -> new ExpenseSplitResponse(split.getId(), split.getAmount())));

    return new ExpenseResponse(expense.getId(), expense.getAmount(), expense.getDescription(),
        expense.getCategory(), splitResponses);
  }

  /**
   * Retrieves all home expense by homeId.
   * @param homeId id of the home
   *
   * @return ExpenseResponse
   * */
  @Transactional(readOnly = true)
  public Map<Long, BigDecimal> getHomeBalances(Long homeId) throws AccountNotFoundException {
    User userDetails = getUserDetails();
    Home home = homeRepository.findById(homeId)
        .orElseThrow(() -> new ResourceNotFoundException("Home not found."));

    // Check if any member in the home has an ID matching the current user's ID
    checkUserMemberOfHome(home,userDetails);

    Map<Long, BigDecimal> balances = new HashMap<>();
    for (User member : home.getMembers()) {
      balances.put(member.getId(), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    List<Expense> expenses = expenseRepository.findByHomeId(homeId);

    for (Expense expense : expenses) {
      if (expense.getSplits() == null || expense.getPayer() == null) {
        continue;
      }

      Long payerId = expense.getPayer().getId();

      for (ExpenseSplit split : expense.getSplits()) {
        Long memberId = split.getUser().getId();
        BigDecimal amount = split.getAmount();

        if (!memberId.equals(payerId)) {
          // Update Payer: (Owed money)
          balances.compute(payerId, (k, v) -> v.add(amount));

          // Update Member: (Owes money)
          balances.compute(memberId, (k, v) -> v.subtract(amount));
        }
      }
    }
    return balances;
  }

  /**
   * Deletes expense using id
   * @param expenseId id of the expense that needs to be deleted
   *
   * @throws AccountNotFoundException when no user account found
   */
  @Transactional
  public void deleteExpense(Long expenseId) throws AccountNotFoundException {
    User userDetails = getUserDetails();
    Expense expense = expenseRepository.findById(expenseId)
        .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

    // Security: Only the person who paid for it can delete it
    if (!expense.getPayer().getId().equals(userDetails.getId())) {
      throw new ForbiddenException("Only the payer can delete this expense");
    }

    expenseRepository.delete(expense);
  }

  /**
   * Creates a personal expense with no splits — only records what the payer spent.
   *
   * @param amount      amount paid
   * @param description item name / description
   * @param homeId      home the expense belongs to
   * @return ExpenseResponse with empty splits map
   */
  @Transactional
  public ExpenseResponse createPersonalExpense(BigDecimal amount, String description, Long homeId)
      throws AccountNotFoundException {
    User payer = getUserDetails();
    Home home = homeRepository.findById(homeId)
        .orElseThrow(() -> new ResourceNotFoundException("Home not found."));
    checkUserMemberOfHome(home, payer);

    Expense expense = new Expense();
    expense.setAmount(amount);
    expense.setDescription(description);
    expense.setCategory("SHOPPING");
    expense.setHome(home);
    expense.setPayer(payer);
    expense.setSplits(new ArrayList<>());

    Expense saved = expenseRepository.save(expense);

    return new ExpenseResponse(saved.getId(), saved.getAmount(),
        saved.getDescription(), saved.getCategory(), Map.of());
  }

  /**
   * Records a direct payment from the logged-in user to another member.
   * This effectively reduces the debt between them.
   */
  @Transactional
  public ExpenseResponse settleUp(Long homeId, Long payeeId, BigDecimal amount) throws AccountNotFoundException {
    User payer = getUserDetails(); // The person paying the money
    User payee = userRepository.findById(payeeId)
        .orElseThrow(() -> new ResourceNotFoundException("Payee not found"));
    Home home = homeRepository.findById(homeId)
        .orElseThrow(() -> new ResourceNotFoundException("Home not found"));

    // Validation: Both must be in the same home
    boolean isPayerInHome = home.getMembers().stream().anyMatch(m -> m.getId().equals(payer.getId()));
    boolean isPayeeInHome = home.getMembers().stream().anyMatch(m -> m.getId().equals(payee.getId()));

    if (!isPayerInHome || !isPayeeInHome) {
      throw new ForbiddenException("Both users must be members of the same home to settle up");
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

  @Transactional(readOnly = true)
  public List<SettlementSuggestion> getSettlements(Long homeId) throws AccountNotFoundException {
    // 1. Get net balances (auth + membership check happens inside)
    Map<Long, BigDecimal> netBalances = this.getHomeBalances(homeId);

    // Build a name lookup map from home members
    Home home = homeRepository.findById(homeId)
            .orElseThrow(() -> new ResourceNotFoundException("Home not found."));
    Map<Long, String> nameMap = home.getMembers().stream()
            .collect(Collectors.toMap(User::getId, User::getName));

    // 2. Work on a mutable copy so we don't touch the original
    Map<Long, BigDecimal> balances = new HashMap<>(netBalances);
    List<SettlementSuggestion> suggestions = new ArrayList<>();

    // 3. Greedy min-cash-flow: each round settle the biggest debtor → biggest creditor
    while (true) {
      Long creditorId = null, debtorId = null;
      BigDecimal maxCredit = BigDecimal.ZERO, maxDebt = BigDecimal.ZERO;

      for (Map.Entry<Long, BigDecimal> entry : balances.entrySet()) {
        BigDecimal val = entry.getValue();
        if (val.compareTo(maxCredit) > 0)          { maxCredit = val;          creditorId = entry.getKey(); }
        if (val.negate().compareTo(maxDebt) > 0)   { maxDebt   = val.negate(); debtorId   = entry.getKey(); }
      }

      // Stop when nothing meaningful remains (rounding dust < 1 cent)
      if (creditorId == null || debtorId == null || maxCredit.compareTo(new BigDecimal("0.01")) < 0) break;

      BigDecimal amount = maxCredit.min(maxDebt).setScale(2, RoundingMode.HALF_UP);

      suggestions.add(new SettlementSuggestion(
              debtorId,   nameMap.getOrDefault(debtorId,   "Unknown"),
              creditorId, nameMap.getOrDefault(creditorId, "Unknown"),
              amount
      ));

      // Reduce both balances by the settled amount
      balances.put(creditorId, balances.get(creditorId).subtract(amount));
      balances.put(debtorId,   balances.get(debtorId).add(amount));
    }

    return suggestions;
  }




}
