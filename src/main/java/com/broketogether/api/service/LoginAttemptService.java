package com.broketogether.api.service;

import com.broketogether.api.model.User;
import com.broketogether.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Manages failed login attempt tracking and account lockout.
 * Lock policy: 5 failed attempts → locked for 15 minutes.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final UserRepository userRepository;

    public LoginAttemptService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Called on successful login — resets the failed attempt counter.
     */
    @Transactional
    public void loginSucceeded(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
        });
    }

    /**
     * Called on failed login — increments counter and locks if threshold reached.
     */
    @Transactional
    public void loginFailed(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            }
            userRepository.save(user);
        });
    }

    /**
     * Returns remaining lock time message if account is locked.
     */
    public String getLockMessage(String email) {
        return userRepository.findByEmailIgnoreCase(email)
            .filter(u -> u.getLockedUntil() != null && u.getLockedUntil().isAfter(LocalDateTime.now()))
            .map(u -> "Account locked due to too many failed attempts. Try again after "
                + u.getLockedUntil().toLocalTime().withSecond(0).withNano(0) + ".")
            .orElse(null);
    }
}
