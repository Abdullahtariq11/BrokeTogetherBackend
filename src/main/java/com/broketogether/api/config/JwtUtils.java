package com.broketogether.api.config;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Utility class for JWT (JSON Web Token) operations.
 *
 * <p>
 * This class handles three main JWT operations:
 * <ul>
 * <li>Generation: Creating new JWT tokens when users log in</li>
 * <li>Validation: Verifying that tokens are valid and haven't been tampered
 * with</li>
 * <li>Parsing: Extracting user information (email) from tokens</li>
 * </ul>
 * </p>
 *
 * <p>
 * JWT tokens are used for stateless authentication. When a user logs in, they
 * receive a token that they include in subsequent requests. This eliminates the
 * need for server-side session storage.
 * </p>
 *
 * @see io.jsonwebtoken.Jwts
 * @see org.springframework.security.core.Authentication
 */
@Component
public class JwtUtils {

  /**
   * Secret key used to sign JWT tokens. Injected from application.properties.
   * Must be at least 64 characters (512 bits) for HS512 algorithm.
   *
   * <p>
   * SECURITY NOTE: In production, this should be stored as an environment
   * variable, not directly in application.properties.
   * </p>
   */
  @Value("${jwt.secret}")
  private String jwtSecret;

  /**
   * Token expiration time in milliseconds for web/default clients. Injected from
   * application.properties. Default: 86400000 ms = 24 hours.
   *
   * <p>
   * After this time, the token becomes invalid and the user must log in again.
   * </p>
   */
  @Value("${jwt.expiration}")
  private long jwtExpirationMs;

  /**
   * Token expiration time in milliseconds for the mobile app, which stays signed
   * in across app restarts instead of expiring daily like the web client.
   * Default: 2592000000 ms = 30 days.
   */
  @Value("${jwt.mobile-expiration}")
  private long mobileJwtExpirationMs;

  /**
   * Generates a cryptographically secure signing key from the configured secret
   * string.
   *
   * <p>
   * The key is generated using HMAC-SHA algorithm and must be at least 512 bits
   * for HS512 signature algorithm. This key is used to both sign (create) and
   * verify (validate) JWT tokens.
   * </p>
   *
   * @return SecretKey object for signing/verifying JWTs
   */
  @PostConstruct
  public void validateSecret() {
    if (jwtSecret == null || jwtSecret.length() < 64) {
      throw new IllegalStateException(
          "JWT_SECRET must be at least 64 characters for HS512. Current length: " +
          (jwtSecret == null ? "null" : jwtSecret.length()));
    }
  }

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Generates a new JWT token for an authenticated user.
   *
   * <p>
   * The token contains:
   * <ul>
   * <li><b>Subject (sub):</b> User's email address</li>
   * <li><b>Issued At (iat):</b> Timestamp when token was created</li>
   * <li><b>Expiration (exp):</b> Timestamp when token expires</li>
   * <li><b>Signature:</b> Cryptographic signature to prevent tampering</li>
   * </ul>
   * </p>
   *
   * <p>
   * Example token structure:
   *
   * <pre>
   * eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNzAzMTg4ODAwLCJleHAiOjE3MDMyNzUyMDB9.signature
   * [    Header    ].[                        Payload                         ].[Signature]
   * </pre>
   * </p>
   *
   * @param authentication Spring Security Authentication object containing user
   *                       details from successful login
   * @return A compact, URL-safe JWT token string
   */
  public String generateToken(Authentication authentication) {
    return generateToken(authentication, false);
  }

  /**
   * @param longLived when true, issues a mobile-length token (see
   *                  {@link #mobileJwtExpirationMs}) instead of the default
   *                  web-length token.
   */
  public String generateToken(Authentication authentication, boolean longLived) {
    UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
    return buildToken(userPrincipal.getUsername(), longLived);
  }

  public String generateToken(UserDetails userDetails) {
    return generateToken(userDetails, false);
  }

  /**
   * @param longLived when true, issues a mobile-length token (see
   *                  {@link #mobileJwtExpirationMs}) instead of the default
   *                  web-length token.
   */
  public String generateToken(UserDetails userDetails, boolean longLived) {
    return buildToken(userDetails.getUsername(), longLived);
  }

  private String buildToken(String subject, boolean longLived) {
    long expirationMs = longLived ? mobileJwtExpirationMs : jwtExpirationMs;
    return Jwts.builder()
        .setSubject(subject)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
        .signWith(getSigningKey())
        .compact();
  }

  /**
   * Extracts the username (email) from a JWT token.
   *
   * <p>
   * This method:
   * <ol>
   * <li>Parses the JWT token string</li>
   * <li>Verifies the signature using our secret key</li>
   * <li>Extracts the claims (payload) from the token</li>
   * <li>Returns the subject claim (user's email)</li>
   * </ol>
   * </p>
   *
   * <p>
   * This is used by the authentication filter to identify which user is making a
   * request.
   * </p>
   *
   * @param token The JWT token string (without "Bearer " prefix)
   * @return The username (email) stored in the token's subject claim
   * @throws io.jsonwebtoken.JwtException if the token is invalid or expired
   */
  public String getUserNameFromJwtToken(String token) {
    return Jwts.parserBuilder().setSigningKey(getSigningKey()) // Provide the key to verify the
        // signature
        .build() // Build the parser
        .parseClaimsJws(token) // Parse and verify the signed token
        .getBody() // Get the claims (payload)
        .getSubject(); // Extract the subject (username/email)
  }

  /**
   * Validates a JWT token by checking its signature, format, and expiration.
   *
   * <p>
   * Validation checks:
   * <ul>
   * <li><b>Signature:</b> Ensures token wasn't tampered with</li>
   * <li><b>Format:</b> Ensures token structure is valid</li>
   * <li><b>Expiration:</b> Ensures token hasn't expired</li>
   * <li><b>Algorithm:</b> Ensures correct signing algorithm was used</li>
   * </ul>
   * </p>
   *
   * <p>
   * Common failure scenarios:
   * <ul>
   * <li>User modifies the token payload → Signature validation fails</li>
   * <li>Token was created more than 24 hours ago → Expiration check fails</li>
   * <li>Token format is corrupted → Malformed JWT exception</li>
   * </ul>
   * </p>
   *
   * @param authToken The JWT token string to validate
   * @return true if the token is valid; false if invalid, expired, or malformed
   */
  public boolean validateJwtToken(String authToken) {
    try {
      // Attempt to parse and validate the token
      Jwts.parserBuilder().setSigningKey(getSigningKey()) // Use our secret key for signature
      // verification
      .build().parseClaimsJws(authToken); // This throws an exception if token is invalid
      return true; // Token is valid
    } catch (io.jsonwebtoken.security.SecurityException
        | io.jsonwebtoken.MalformedJwtException
        | io.jsonwebtoken.ExpiredJwtException
        | io.jsonwebtoken.UnsupportedJwtException
        | IllegalArgumentException e) {
      // Invalid token — silently reject (do not log token details)
    }
    return false; // Token is invalid
  }
}