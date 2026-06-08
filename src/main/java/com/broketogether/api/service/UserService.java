package com.broketogether.api.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.security.auth.login.AccountNotFoundException;

import com.broketogether.api.dto.UserResponse;
import com.broketogether.api.exception.ConflictException;
import com.broketogether.api.model.Expense;
import com.broketogether.api.repository.ExpenseSplitRepository;
import com.broketogether.api.repository.PasswordResetTokenRepository;
import com.broketogether.api.utility.Utility;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.broketogether.api.model.Home;
import com.broketogether.api.model.ShoppingItem;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.ExpenseRepository;
import com.broketogether.api.repository.HomeRepository;
import com.broketogether.api.repository.ShoppingItemRepository;
import com.broketogether.api.repository.UserRepository;

@Service
public class UserService extends Utility {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final HomeRepository homeRepository;
  private final ExpenseRepository expenseRepository;
  private final ShoppingItemRepository shoppingItemRepository;
  private final ExpenseSplitRepository expenseSplitRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
      HomeRepository homeRepository, ExpenseRepository expenseRepository,
      ShoppingItemRepository shoppingItemRepository,
      ExpenseSplitRepository expenseSplitRepository,
      PasswordResetTokenRepository passwordResetTokenRepository)
      throws Exception {
    if (userRepository == null) {
      throw new Exception("Repository cannot be null");
    }
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.homeRepository = homeRepository;
    this.expenseRepository = expenseRepository;
    this.shoppingItemRepository = shoppingItemRepository;
    this.expenseSplitRepository = expenseSplitRepository;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
  }

  /**
   * @return List of users
   */
  public List<UserResponse> getAllUser() {
    List<User> users= userRepository.findAll();

    return users.stream().map(u->new UserResponse(u.getId(),u.getName(),u.getEmail(),u.getPremium(),u.getSubscriptionStatus())).toList();
  }

  /**
   * @param email to find user
   * @return UserResponse
   */
  public Optional<UserResponse> getUserByEmail(String email) {
    return this.userRepository.findByEmail(email)
            .map(u-> new UserResponse(u.getId(),u.getName(),u.getEmail(),u.getPremium(),u.getSubscriptionStatus()));
  }

  /**
   * Creates a user
   * @param user details to save a user
   *
   * @return UserResponse
   */
  public UserResponse saveUser(User user) {
    if (this.userRepository.existsByEmail(user.getEmail())) {
      throw new ConflictException("Email already in use!");
    }
    String rawPassword = user.getPassword();
    String encodedPassword = passwordEncoder.encode(rawPassword);
    user.setPassword(encodedPassword);
    User savedUser= userRepository.save(user);

    return new UserResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail(),savedUser.getPremium(),savedUser.getSubscriptionStatus());
  }

  /**
   * Permanently deletes the currently authenticated user's account.
   * - Non-owned homes: removes the user from the members list.
   * - Owned homes: deletes all expenses then deletes the home entirely.
   * - Finally deletes the user record.
   */
  @Transactional
  public void deleteAccount() throws AccountNotFoundException {
    User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    // 1. Delete password reset tokens referencing this user
    passwordResetTokenRepository.deleteByUser(currentUser);

    // 2. Delete expenses in non-owned homes where this user is the payer
    //    (payer_id is non-nullable, so these must be removed before deleting the user)
    List<Expense> expensesAsPayer = expenseRepository.findByPayerId(currentUser.getId());
    expenseRepository.deleteAll(expensesAsPayer); // cascades to their ExpenseSplits

    // 3. Delete any remaining ExpenseSplit rows referencing this user
    expenseSplitRepository.deleteByUserId(currentUser.getId());

    // Non-owned homes — remove user from members.
    // Also clear FK references to this user on any shopping items they added or checked.
    Set<Home> allHomes = homeRepository.findByMembersContaining(currentUser);
    for (Home home : allHomes) {
      if (!home.getCreator().getId().equals(currentUser.getId())) {
        home.getMembers().removeIf(u -> u.getId().equals(currentUser.getId()));
        homeRepository.save(home);
      }
    }

    // Shopping items in non-owned homes: delete ones added by this user;
    // null out checkedBy where this user checked an item someone else added.
    List<ShoppingItem> addedByUser = shoppingItemRepository.findByAddedById(currentUser.getId());
    shoppingItemRepository.deleteAll(addedByUser);

    List<ShoppingItem> checkedByUser = shoppingItemRepository.findByCheckedById(currentUser.getId());
    for (ShoppingItem item : checkedByUser) {
      item.setCheckedBy(null);
      shoppingItemRepository.save(item);
    }

    // Owned homes — delete shopping items, expenses, then the home itself
    Set<Home> ownedHomes = homeRepository.findByCreatorId(currentUser.getId());
    for (Home home : ownedHomes) {
      shoppingItemRepository.deleteAll(shoppingItemRepository.findByHomeId(home.getId()));
      expenseRepository.deleteAll(expenseRepository.findByHomeId(home.getId()));
      home.getMembers().clear();
      homeRepository.save(home);
      homeRepository.delete(home);
    }

    userRepository.delete(currentUser);
  }

  /**
   * Reset's Password
   *
   * @param currentPassword user's current password
   * @param newPassword user's new password
   */
  /**
   * Returns the current user's profile fetched fresh from the DB.
   */
  public UserResponse getProfile() throws AccountNotFoundException {
    User userDetails = getUserDetails();
    // Reload from DB to get the latest field values (e.g. isPremium after webhook)
    User fresh = userRepository.findById(userDetails.getId())
        .orElseThrow(() -> new AccountNotFoundException("User not found"));
    return new UserResponse(fresh.getId(), fresh.getName(), fresh.getEmail(), fresh.getPremium(), fresh.getSubscriptionStatus());
  }

  public void resetPassword(String currentPassword, String newPassword) throws AccountNotFoundException {
    User userDetails = getUserDetails();
    // Reload fresh from DB — SecurityContext principal may not have the latest password hash
    User freshUser = userRepository.findById(userDetails.getId())
        .orElseThrow(() -> new AccountNotFoundException("User not found"));
    if (!passwordEncoder.matches(currentPassword, freshUser.getPassword())) {
      throw new IllegalArgumentException("Password entered doesnt match current password.");
    }
    freshUser.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(freshUser);
  }

  /**
   * Edits user name
   * @param name new user's name
   *
   * @return UserResponse details
   */
  public UserResponse editUserName(String name) throws AccountNotFoundException {
    User userDetails= getUserDetails();

    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Name field cannot be empty.");
    }

    userDetails.setName(name);
    User savedUser =userRepository.save(userDetails);

    return new UserResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail(),savedUser.getPremium(),savedUser.getSubscriptionStatus());
  }

}
