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
import com.broketogether.api.dto.enums.SplitType;
import com.broketogether.api.exception.ForbiddenException;
import com.broketogether.api.exception.ResourceNotFoundException;
import com.broketogether.api.utility.Utility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    ExpenseResponse res1 = new ExpenseResponse(expenseCreated.getId(), expenseCreated.getAmount(),
        expenseCreated.getDescription(), expenseCreated.getCategory(), splitResponses);
    res1.setCreatedAt(expenseCreated.getCreatedAt());
    return res1;

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

    ExpenseResponse res2 = new ExpenseResponse(expenseCreated.getId(), expenseCreated.getAmount(),
        expenseCreated.getDescription(), expenseCreated.getCategory(), splitResponses);
    res2.setCreatedAt(expenseCreated.getCreatedAt());
    return res2;
  }

  /**
   * Creates Expense for user with equal splits among users
   * @param expenseRequest expense dto used to create expense.
   *
   * @return ExpenseResponse
   * @throws AccountNotFoundException if there is no account
   */
  @Transactional
  public ExpenseResponse createExpenseUpdated(ExpenseRequestUpdated expenseRequest)
          throws AccountNotFoundException {
    User userDetails = getUserDetails();
    Home home = homeRepository.findById(expenseRequest.getHomeId())
            .orElseThrow(() -> new ResourceNotFoundException("Home with this id does not exist."));
    checkUserMemberOfHome(home, userDetails);

    Expense expense = new Expense();
    expense.setAmount(expenseRequest.getTotalAmount());
    expense.setCategory(expenseRequest.getCategory());
    expense.setDescription(expenseRequest.getDescription());
    expense.setHome(home);
    expense.setPayer(userDetails);

    Set<User> participants;
    if (expenseRequest.getSplitType() == SplitType.PERSONAL) {
      // Personal: only the payer, no splits with others
      participants = Set.of(userDetails);
    } else {
      Set<Long> userIds = expenseRequest.getUserIds();
      if (userIds == null || userIds.isEmpty()) {
        // Default: all home members
        participants = home.getMembers();
      } else {
        List<User> selected = userRepository.findAllById(userIds);
        participants = new HashSet<>(selected);
        participants.add(userDetails);
        Set<Long> homeMemberIds = home.getMembers().stream()
                .map(User::getId).collect(Collectors.toSet());
        for (User m : participants) {
          if (!homeMemberIds.contains(m.getId())) {
            throw new ForbiddenException("User " + m.getId() + " is not a member of this home");
          }
        }
      }
      if (participants.isEmpty()) {
        throw new IllegalArgumentException("No members in home");
      }
    }

    List<ExpenseSplit> splits = getSplits(expense, userDetails, participants, expenseRequest);
    expense.setSplits(splits);

    Expense saved = expenseRepository.save(expense);

    Map<Long, ExpenseSplitResponse> splitResponses = new HashMap<>();
    for (ExpenseSplit split : saved.getSplits()) {
      splitResponses.put(split.getUser().getId(),
              new ExpenseSplitResponse(split.getId(), split.getAmount()));
    }

    ExpenseResponse response = new ExpenseResponse(saved.getId(), saved.getAmount(),
            saved.getDescription(), saved.getCategory(), splitResponses);
    response.setPayerId(userDetails.getId());
    response.setPayerName(userDetails.getName());
    response.setCreatedAt(saved.getCreatedAt());
    return response;
  }

  private List<ExpenseSplit>getSplits(Expense expense,
                                                      User payer,Set<User> participants,
                                                      ExpenseRequestUpdated expenseRequest){

    BigDecimal total = expenseRequest.getTotalAmount();
    List<ExpenseSplit> expenseSplits = new ArrayList<>();

    switch (expenseRequest.getSplitType()) {
      case PERSONAL -> {
        // Only records the payer's expense — no splits with other members
        expenseSplits.add(new ExpenseSplit(expense, payer, total));
        break;
      }
      case EQUAL -> {
        BigDecimal share = total.divide(BigDecimal.valueOf(participants.size()), 2, RoundingMode.HALF_UP);
        for (User member : participants) {
          expenseSplits.add(new ExpenseSplit(expense, member, share));
        }
        break;
      }
      case FIXED -> {
        BigDecimal payerAmount = expenseRequest.getPayerFixedAmount();
        if (payerAmount == null || payerAmount.compareTo(BigDecimal.ZERO) < 0 || payerAmount.compareTo(total) > 0) {
          throw new IllegalArgumentException("Invalid payer fixed amount.");
        }

        Set<User> otherMembers = participants.stream()
                .filter(u -> !u.getId().equals(payer.getId()))
                .collect(Collectors.toSet());

        if (otherMembers.isEmpty()) {
          throw new IllegalArgumentException("Need other participants to split the remainder.");
        }

        BigDecimal remaining = total.subtract(payerAmount);
        BigDecimal shareForOthers = remaining.divide(
                BigDecimal.valueOf(otherMembers.size()), 2, RoundingMode.HALF_UP
        );

        expenseSplits.add(new ExpenseSplit(expense, payer, payerAmount));
        for (User other : otherMembers) {
          expenseSplits.add(new ExpenseSplit(expense, other, shareForOthers));
        }
        break;
      }
      case CUSTOM -> {
        Map<Long, BigDecimal> exactMap = expenseRequest.getExactSplits();
        if (exactMap == null || exactMap.isEmpty()) {
          throw new IllegalArgumentException("Exact split breakdown is required.");
        }

        BigDecimal sum = exactMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(total) != 0) {
          throw new IllegalArgumentException("Split amounts sum (" + sum + ") must equal total expense (" + total + ").");
        }

        for (User member : participants) {
          BigDecimal memberShare = exactMap.getOrDefault(member.getId(), BigDecimal.ZERO);
          expenseSplits.add(new ExpenseSplit(expense, member, memberShare));
        }
        break;
      }
    }

    return expenseSplits;
  }


  private List<User> getParticipants(Set<Long> userIds, User userDetails, Home home) {
    if (userIds.isEmpty()) {
      return null;
    }
    List<User> selectedMembers = userRepository.findAllById(userIds);

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
      return selectedMembers;
  }


    /**
     * Method is responsible for returning all expenses for a home
     * @param homeId home id
     * @param page page number
     * @param size size of expenses to display
     * @return  all expenses for a home
     * */
  @Transactional(readOnly = true)
  public PagedExpenseResponse getAllExpensesForHome(Long homeId, int page, int size)
      throws AccountNotFoundException {
    User userDetails = getUserDetails();
    Home home = homeRepository.findById(homeId)
        .orElseThrow(() -> new ResourceNotFoundException("Home not found."));

    checkUserMemberOfHome(home, userDetails);

    Page<Expense> expensePage =
        expenseRepository.findByHomeIdOrderByIdDesc(
            homeId, PageRequest.of(page, size));

    List<ExpenseResponse> expenseResponses = new ArrayList<>();
    for (Expense expense : expensePage.getContent()) {
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
      response.setCreatedAt(expense.getCreatedAt());
      expenseResponses.add(response);
    }

    return new PagedExpenseResponse(expenseResponses, !expensePage.isLast(), page);
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

    ExpenseResponse res3 = new ExpenseResponse(expense.getId(), expense.getAmount(), expense.getDescription(),
        expense.getCategory(), splitResponses);
    res3.setCreatedAt(expense.getCreatedAt());
    return res3;
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
          // Use getOrDefault to handle users who were removed from the home
          // but whose expense records still exist
          balances.put(payerId,
              balances.getOrDefault(payerId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                  .add(amount));

          // Update Member: (Owes money)
          balances.put(memberId,
              balances.getOrDefault(memberId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                  .subtract(amount));
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

    ExpenseResponse res4 = new ExpenseResponse(saved.getId(), saved.getAmount(),
        saved.getDescription(), saved.getCategory(), Map.of());
    res4.setCreatedAt(saved.getCreatedAt());
    return res4;
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

    ExpenseResponse res5 = new ExpenseResponse(saved.getId(), saved.getAmount(),
        saved.getDescription(), saved.getCategory(), splitResponses);
    res5.setCreatedAt(saved.getCreatedAt());
    return res5;
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
