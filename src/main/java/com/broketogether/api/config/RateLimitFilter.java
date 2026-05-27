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
 * Limits requests per IP address to prevent abuse.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

  // Store a bucket per IP address
  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  private Bucket createNewBucket() {
    // 300 requests per minute per IP (5 req/sec burst tolerance)
    // React StrictMode double-invokes effects in dev, and a dashboard load
    // fires several parallel requests — 30/min was far too restrictive.
    Bandwidth limit = Bandwidth.builder()
        .capacity(300)
        .refillGreedy(300, Duration.ofMinutes(1))
        .build();
    return Bucket.builder().addLimit(limit).build();
  }

  private Bucket resolveBucket(String ip) {
    return buckets.computeIfAbsent(ip, k -> createNewBucket());
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String ip = getClientIp(request);
    Bucket bucket = resolveBucket(ip);

    if (bucket.tryConsume(1)) {
      filterChain.doFilter(request, response);
    } else {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType("application/json");
      response.getWriter().write(
          "{\"status\":429,\"message\":\"Too many requests. Please try again later.\",\"timestamp\":"
              + System.currentTimeMillis() + "}");
    }
  }

  private String getClientIp(HttpServletRequest request) {
    // With server.forward-headers-strategy=native, Spring has already resolved
    // the real client IP from X-Forwarded-For into getRemoteAddr().
    // Reading the raw header directly would allow spoofing to bypass rate limiting.
    return request.getRemoteAddr();
  }
}
