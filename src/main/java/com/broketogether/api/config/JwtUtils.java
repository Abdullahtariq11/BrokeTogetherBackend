package com.broketogether.api.config;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

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
   * Token expiration time in milliseconds. Injected from application.properties.
   * Default: 86400000 ms = 24 hours.
   * 
   * <p>
   * After this time, the token becomes invalid and the user must log in again.
   * </p>
   */
  @Value("${jwt.expiration}")
  private int jwtExpirationMs;

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
    // Extract the UserDetails from the authenticated user
    UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

    return Jwts.builder().setSubject(userPrincipal.getUsername()) // Store user's email as the
                                                                  // subject
        .setIssuedAt(new Date()) // Set token creation timestamp
        .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs)) // Set expiration time
        .signWith(getSigningKey()) // Sign the token with our secret key (uses HS512 by default)
        .compact(); // Build and serialize to a compact string format
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
    } catch (io.jsonwebtoken.security.SecurityException e) {
      // Thrown when the signature doesn't match (token was tampered with)
      System.err.println("Invalid JWT signature: " + e.getMessage());
    } catch (io.jsonwebtoken.MalformedJwtException e) {
      // Thrown when the token structure is invalid (not properly formatted)
      System.err.println("Invalid JWT token: " + e.getMessage());
    } catch (io.jsonwebtoken.ExpiredJwtException e) {
      // Thrown when the token's expiration time has passed
      System.err.println("JWT token is expired: " + e.getMessage());
    } catch (io.jsonwebtoken.UnsupportedJwtException e) {
      // Thrown when the token uses an unsupported algorithm or format
      System.err.println("JWT token is unsupported: " + e.getMessage());
    } catch (IllegalArgumentException e) {
      // Thrown when the token string is empty or null
      System.err.println("JWT claims string is empty: " + e.getMessage());
    }
    return false; // Token is invalid
  }
}