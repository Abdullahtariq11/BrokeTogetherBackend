package com.broketogether.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class EmailService {

  @Value("${resend.api-key}")
  private String apiKey;

  @Value("${app.from-email}")
  private String fromEmail;

  private final RestTemplate restTemplate = new RestTemplate();

  /**
   * Sends a password reset email via the Resend API.
   *
   * @param toEmail   recipient's email address
   * @param resetLink full URL of the reset page including the token
   */
  public void sendPasswordResetEmail(String toEmail, String resetLink) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(apiKey);

    String html = """
        <div style="font-family: sans-serif; max-width: 480px; margin: 0 auto; padding: 32px 24px; background: #f8fafc; border-radius: 12px;">
          <h2 style="color: #1e293b; margin-bottom: 8px;">Reset your password</h2>
          <p style="color: #64748b; margin-bottom: 24px;">
            We received a request to reset your BrokeTogether password. Click the button below to choose a new one.
            This link expires in <strong>15 minutes</strong>.
          </p>
          <a href="%s"
             style="display: inline-block; background: #6366f1; color: white; font-weight: 600;
                    text-decoration: none; padding: 12px 28px; border-radius: 8px; font-size: 15px;">
            Reset Password
          </a>
          <p style="color: #94a3b8; font-size: 13px; margin-top: 24px;">
            If you didn't request this, you can safely ignore this email.
          </p>
        </div>
        """.formatted(resetLink);

    Map<String, Object> body = Map.of(
        "from", fromEmail,
        "to", new String[]{toEmail},
        "subject", "Reset your BrokeTogether password",
        "html", html
    );

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    restTemplate.postForEntity("https://api.resend.com/emails", request, String.class);
  }
}
