package com.broketogether.api.config;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rate limiting filter using Bucket4j.
 * - Global: 300 requests/min per IP (covers normal dashboard usage)
 * - Auth endpoints: 10 requests/min per IP (brute-force protection)
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

  // Global buckets — one per IP
  private final Map<String, Bucket> globalBuckets = new ConcurrentHashMap<>();

  // Strict buckets for sensitive auth endpoints — one per IP
  private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();

  private Bucket createGlobalBucket() {
    Bandwidth limit = Bandwidth.builder()
        .capacity(300)
        .refillGreedy(300, Duration.ofMinutes(1))
        .build();
    return Bucket.builder().addLimit(limit).build();
  }

  private Bucket createAuthBucket() {
    // 10 attempts per minute per IP on auth endpoints
    Bandwidth limit = Bandwidth.builder()
        .capacity(10)
        .refillGreedy(10, Duration.ofMinutes(1))
        .build();
    return Bucket.builder().addLimit(limit).build();
  }

  private boolean isSensitiveAuthEndpoint(String path) {
    return path.equals("/api/v1/auth/login")
        || path.equals("/api/v1/auth/register")
        || path.equals("/api/v1/auth/forgot-password")
        || path.equals("/api/v1/auth/reset-password");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String ip = getClientIp(request);
    String path = request.getRequestURI();

    // Apply stricter limit to auth endpoints first
    if (isSensitiveAuthEndpoint(path)) {
      Bucket authBucket = authBuckets.computeIfAbsent(ip, k -> createAuthBucket());
      if (!authBucket.tryConsume(1)) {
        writeTooManyRequests(response, "Too many attempts. Please wait a minute and try again.");
        return;
      }
    }

    // Apply global limit to all endpoints
    Bucket globalBucket = globalBuckets.computeIfAbsent(ip, k -> createGlobalBucket());
    if (!globalBucket.tryConsume(1)) {
      writeTooManyRequests(response, "Too many requests. Please try again later.");
      return;
    }

    filterChain.doFilter(request, response);
  }

  private void writeTooManyRequests(HttpServletResponse response, String message) throws IOException {
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType("application/json");
    response.getWriter().write(
        "{\"status\":429,\"message\":\"" + message + "\",\"timestamp\":" + System.currentTimeMillis() + "}");
  }

  private String getClientIp(HttpServletRequest request) {
    // With server.forward-headers-strategy=native, Spring has already resolved
    // the real client IP from X-Forwarded-For into getRemoteAddr().
    return request.getRemoteAddr();
  }
}
