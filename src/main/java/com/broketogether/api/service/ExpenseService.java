package com.broketogether.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.broketogether.api.model.Expense;
import com.broketogether.api.model.ExpenseSplit;
import com.broketogether.api.model.Home;
import com.broketogether.api.model.SettlementCheckpoint;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.ExpenseRepository;
import com.broketogether.api.repository.HomeRepository;
import com.broketogether.api.repository.SettlementCheckpointRepository;
import com.broketogether.api.repository.UserRepository;

@Service
public class ExpenseService extends Utility {

  private final ExpenseRepository expenseRepository;
  private final UserRepository userRepository;
  private final HomeRepository homeRepository;
  private final SettlementCheckpointRepository settlementCheckpointRepository;


  public ExpenseService(ExpenseRepository expenseRepository, UserRepository userRepository,
      HomeRepository homeRepository, SettlementCheckpointRepository settlementCheckpointRepository) {
    this.expenseRepository = expenseRepository;
    this.userRepository = userRepository;
    this.homeRepository = homeRepository;
    this.settlementCheckpointRepository = settlementCheckpointRepository;
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

    LocalDateTime lastSettledAt = settlementCheckpointRepository
        .findByUserIdAndHomeId(userDetails.getId(), homeId)
        .map(SettlementCheckpoint::getSettledAt)
        .orElse(null);

    return new PagedExpenseResponse(expenseResponses, !expensePage.isLast(), page, lastSettledAt);
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

    // Mark this as the payer's settle-up checkpoint so the activity feed can
    // show everything before it as closed out.
    LocalDateTime now = LocalDateTime.now();
    SettlementCheckpoint checkpoint = settlementCheckpointRepository
        .findByUserIdAndHomeId(payer.getId(), homeId)
        .orElseGet(() -> new SettlementCheckpoint(payer, home, now));
    checkpoint.setSettledAt(now);
    settlementCheckpointRepository.save(checkpoint);

    // Return response
    Map<Long, ExpenseSplitResponse> splitResponses = Map.of(
        payee.getId(), new ExpenseSplitResponse(saved.getSplits().get(0).getId(), amount)
        );

    ExpenseResponse res5 = new ExpenseResponse(saved.getId(), saved.getAmount(),
        saved.getDescription(), saved.getCategory(), splitResponses);
    res5.setCreatedAt(saved.getCreatedAt());
    return res5;
  }

  // ── B1: PATCH expense ────────────────────────────────────────────────────────

  @Transactional
  public ExpenseResponse patchExpense(Long expenseId, PatchExpenseRequest req)
      throws AccountNotFoundException {
    User caller = getUserDetails();
    Expense expense = expenseRepository.findById(expenseId)
        .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

    checkUserMemberOfHome(expense.getHome(), caller);

    // Only payer or home creator may edit
    boolean isPayer   = expense.getPayer().getId().equals(caller.getId());
    boolean isCreator = expense.getHome().getCreator().getId().equals(caller.getId());
    if (!isPayer && !isCreator) {
      throw new ForbiddenException("Only the payer or household admin can edit this expense");
    }

    if (req.getDescription() != null) expense.setDescription(req.getDescription());
    if (req.getCategory()    != null) expense.setCategory(req.getCategory());
    if (req.getAmount()      != null) expense.setAmount(req.getAmount());

    // Payer override
    if (req.getPayerId() != null) {
      User newPayer = userRepository.findById(req.getPayerId())
          .orElseThrow(() -> new ResourceNotFoundException("Payer not found"));
      checkUserMemberOfHome(expense.getHome(), newPayer);
      expense.setPayer(newPayer);
    }

    // Re-split if split-related fields supplied
    if (req.getSplitType() != null || req.getUserIds() != null
        || req.getExactSplits() != null || req.getPayerFixedAmount() != null
        || req.getAmount() != null) {

      SplitType splitType = req.getSplitType() != null
          ? req.getSplitType()
          : SplitType.EQUAL;

      Set<User> participants;
      if (splitType == SplitType.PERSONAL) {
        participants = Set.of(expense.getPayer());
      } else if (req.getUserIds() != null && !req.getUserIds().isEmpty()) {
        List<User> selected = userRepository.findAllById(req.getUserIds());
        participants = new HashSet<>(selected);
        participants.add(expense.getPayer());
        Set<Long> homeMemberIds = expense.getHome().getMembers().stream()
            .map(User::getId).collect(Collectors.toSet());
        for (User m : participants) {
          if (!homeMemberIds.contains(m.getId()))
            throw new ForbiddenException("User " + m.getId() + " is not a member of this home");
        }
      } else {
        participants = expense.getHome().getMembers();
      }

      // Build a temporary ExpenseRequestUpdated to reuse getSplits()
      ExpenseRequestUpdated tmp = new ExpenseRequestUpdated(
          expense.getAmount(), expense.getDescription(), expense.getCategory(),
          participants.stream().map(User::getId).collect(Collectors.toSet()),
          req.getExactSplits(), splitType, req.getPayerFixedAmount());
      tmp.setHomeId(expense.getHome().getId());

      expense.getSplits().clear();
      expense.getSplits().addAll(getSplits(expense, expense.getPayer(), participants, tmp));
    }

    Expense saved = expenseRepository.save(expense);
    return buildResponse(saved);
  }

  // ── B2: record payment received ──────────────────────────────────────────────

  /**
   * Records a payment RECEIVED — the caller is the creditor, the debtor paid them.
   * Mirrors settleUp() but in the reverse direction.
   */
  @Transactional
  public ExpenseResponse recordPaymentReceived(RecordPaymentRequest req)
      throws AccountNotFoundException {
    User creditor = getUserDetails();
    User debtor   = userRepository.findById(req.getDebtorId())
        .orElseThrow(() -> new ResourceNotFoundException("Debtor not found"));
    Home home = homeRepository.findById(req.getHomeId())
        .orElseThrow(() -> new ResourceNotFoundException("Home not found"));

    boolean creditorInHome = home.getMembers().stream().anyMatch(m -> m.getId().equals(creditor.getId()));
    boolean debtorInHome   = home.getMembers().stream().anyMatch(m -> m.getId().equals(debtor.getId()));
    if (!creditorInHome || !debtorInHome) {
      throw new ForbiddenException("Both users must be members of the same home");
    }

    String methodNote = req.getMethod() != null ? " via " + req.getMethod() : "";
    Expense settlement = new Expense();
    settlement.setAmount(req.getAmount());
    settlement.setCategory("SETTLEMENT");
    settlement.setDescription("Payment from " + debtor.getName() + " to " + creditor.getName() + methodNote);
    settlement.setHome(home);
    // The debtor is the payer of this settlement (they paid the creditor)
    settlement.setPayer(debtor);

    // Split lands on the creditor — they received the money
    ExpenseSplit split = new ExpenseSplit(settlement, creditor, req.getAmount());
    settlement.setSplits(List.of(split));

    Expense saved = expenseRepository.save(settlement);

    // Update checkpoint for the debtor (they settled)
    LocalDateTime now = LocalDateTime.now();
    SettlementCheckpoint checkpoint = settlementCheckpointRepository
        .findByUserIdAndHomeId(debtor.getId(), req.getHomeId())
        .orElseGet(() -> new SettlementCheckpoint(debtor, home, now));
    checkpoint.setSettledAt(now);
    settlementCheckpointRepository.save(checkpoint);

    return buildResponse(saved);
  }

  /** Settlement history for a home — all SETTLEMENT-category expenses. */
  @Transactional(readOnly = true)
  public List<SettlementHistoryResponse> getSettlementHistory(Long homeId)
      throws AccountNotFoundException {
    User caller = getUserDetails();
    Home home = homeRepository.findById(homeId)
        .orElseThrow(() -> new ResourceNotFoundException("Home not found"));
    checkUserMemberOfHome(home, caller);

    return expenseRepository.findSettlementsByHomeId(homeId).stream()
        .map(e -> new SettlementHistoryResponse(
            e.getId(), e.getAmount(), e.getDescription(),
            e.getPayer() != null ? e.getPayer().getId()   : null,
            e.getPayer() != null ? e.getPayer().getName() : null,
            e.getCreatedAt()))
        .collect(Collectors.toList());
  }

  /**
   * Remind endpoint — returns outstanding owed-to-you balances.
   * No messages are sent outside the app.
   */
  @Transactional(readOnly = true)
  public List<SettlementSuggestion> getOwedToMe(Long homeId) throws AccountNotFoundException {
    User caller = getUserDetails();
    Map<Long, BigDecimal> balances = getHomeBalances(homeId);
    BigDecimal myBalance = balances.getOrDefault(caller.getId(), BigDecimal.ZERO);
    if (myBalance.compareTo(BigDecimal.ZERO) <= 0) return List.of();

    // Return full settlement suggestions filtered to ones where the creditor is me
    return getSettlements(homeId).stream()
        .filter(s -> s.creditorId().equals(caller.getId()))
        .collect(Collectors.toList());
  }

  // ── B3: search/filter/sort ────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public PagedExpenseResponse searchExpenses(Long homeId, ExpenseSearchRequest req)
      throws AccountNotFoundException {
    User caller = getUserDetails();
    Home home = homeRepository.findById(homeId)
        .orElseThrow(() -> new ResourceNotFoundException("Home not found"));
    checkUserMemberOfHome(home, caller);

    LocalDateTime from = req.getFrom() != null ? req.getFrom().atStartOfDay()          : null;
    LocalDateTime to   = req.getTo()   != null ? req.getTo().atTime(LocalTime.MAX)      : null;

    Sort sort = switch (req.getSort() == null ? "date_desc" : req.getSort()) {
      case "date_asc"    -> Sort.by("createdAt").ascending();
      case "amount_desc" -> Sort.by("amount").descending();
      case "amount_asc"  -> Sort.by("amount").ascending();
      default            -> Sort.by("createdAt").descending();
    };

    Page<Expense> page = expenseRepository.search(
        homeId,
        req.getQuery()    != null && !req.getQuery().isBlank() ? req.getQuery() : null,
        req.getCategory() != null && !req.getCategory().isBlank() ? req.getCategory() : null,
        req.getMemberId(),
        from, to,
        req.getType()     != null && !req.getType().isBlank() ? req.getType().toLowerCase() : null,
        PageRequest.of(req.getPage(), Math.min(req.getSize(), 100), sort)
    );

    List<ExpenseResponse> responses = page.getContent().stream()
        .map(this::buildResponse)
        .collect(Collectors.toList());

    LocalDateTime lastSettledAt = settlementCheckpointRepository
        .findByUserIdAndHomeId(caller.getId(), homeId)
        .map(SettlementCheckpoint::getSettledAt)
        .orElse(null);

    return new PagedExpenseResponse(responses, !page.isLast(), page.getNumber(), lastSettledAt);
  }

  // ── helper: build ExpenseResponse from Expense entity ─────────────────────────

  private ExpenseResponse buildResponse(Expense expense) {
    Map<Long, ExpenseSplitResponse> splitMap = new HashMap<>();
    for (ExpenseSplit s : expense.getSplits()) {
      splitMap.put(s.getUser().getId(), new ExpenseSplitResponse(s.getId(), s.getAmount()));
    }
    ExpenseResponse r = new ExpenseResponse(expense.getId(), expense.getAmount(),
        expense.getDescription(), expense.getCategory(), splitMap);
    if (expense.getPayer() != null) {
      r.setPayerId(expense.getPayer().getId());
      r.setPayerName(expense.getPayer().getName());
    }
    r.setCreatedAt(expense.getCreatedAt());
    return r;
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
