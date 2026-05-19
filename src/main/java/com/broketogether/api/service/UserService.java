package com.broketogether.api.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.security.auth.login.AccountNotFoundException;

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
public class UserService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final HomeRepository homeRepository;
  private final ExpenseRepository expenseRepository;
  private final ShoppingItemRepository shoppingItemRepository;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
      HomeRepository homeRepository, ExpenseRepository expenseRepository,
      ShoppingItemRepository shoppingItemRepository)
      throws Exception {
    if (userRepository == null) {
      throw new Exception("Repository cannot be null");
    }
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.homeRepository = homeRepository;
    this.expenseRepository = expenseRepository;
    this.shoppingItemRepository = shoppingItemRepository;
  }

  /**
   * @return
   */
  public List<User> getAllUser() {
    return userRepository.findAll();
  }

  /**
   * @param email
   * @return
   */
  public Optional<User> getUserByEmail(String email) {
    return this.userRepository.findByEmail(email);
  }

  /**
   * @param user
   * @return
   */
  public User saveUser(User user) {
    if (this.userRepository.existsByEmail(user.getEmail())) {
      throw new RuntimeException("Email already in use!");
    }
    String rawPassword = user.getPassword();
    String encodedPassword = passwordEncoder.encode(rawPassword);
    user.setPassword(encodedPassword);
    return userRepository.save(user);
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

}
