package com.broketogether.api.service;

import com.broketogether.api.model.PasswordResetToken;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.PasswordResetTokenRepository;
import com.broketogether.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

  @Value("${app.frontend-url}")
  private String frontendUrl;

  private final UserRepository userRepository;
  private final PasswordResetTokenRepository tokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;

  public PasswordResetService(UserRepository userRepository,
                               PasswordResetTokenRepository tokenRepository,
                               PasswordEncoder passwordEncoder,
                               EmailService emailService) {
    this.userRepository = userRepository;
    this.tokenRepository = tokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.emailService = emailService;
  }

  /**
   * Initiates a password reset for the given email.
   * Silently succeeds even if the email is not registered (prevents enumeration).
   */
  @Transactional
  public void initiateReset(String email) {
    userRepository.findByEmailIgnoreCase(email.trim()).ifPresent(user -> {
      // Delete any existing tokens for this user
      tokenRepository.deleteByUser(user);

      // Create a new token valid for 15 minutes
      String token = UUID.randomUUID().toString();
      PasswordResetToken resetToken = new PasswordResetToken(
          token,
          user,
          LocalDateTime.now().plusMinutes(15)
      );
      tokenRepository.save(resetToken);

      String resetLink = frontendUrl + "/reset-password?token=" + token;
      emailService.sendPasswordResetEmail(email, resetLink);
    });
  }

  /**
   * Resets the user's password using the provided token.
   *
   * @throws IllegalArgumentException if the token is invalid, expired, or already used
   */
  @Transactional
  public void resetPassword(String token, String newPassword) {
    PasswordResetToken resetToken = tokenRepository.findByToken(token)
        .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset link."));

    if (resetToken.isUsed()) {
      throw new IllegalArgumentException("This reset link has already been used.");
    }

    if (resetToken.isExpired()) {
      throw new IllegalArgumentException("This reset link has expired. Please request a new one.");
    }

    User user = resetToken.getUser();
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);

    resetToken.setUsed(true);
    tokenRepository.save(resetToken);
  }
}
