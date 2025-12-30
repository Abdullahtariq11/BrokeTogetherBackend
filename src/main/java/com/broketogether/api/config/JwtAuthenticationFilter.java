package com.broketogether.api.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.broketogether.api.service.CustomUserServiceDetail;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT Authentication Filter - The "Gatekeeper" for Protected Endpoints.
 * 
 * <p>
 * This filter intercepts every HTTP request before it reaches the controllers.
 * It checks for a valid JWT token in the Authorization header and authenticates
 * the user if the token is valid.
 * </p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  @Autowired
  private JwtUtils jwtUtils;

  @Autowired
  private CustomUserServiceDetail userServiceDetail;

  /**
   * Core filter method that processes each request.
   * 
   * <p>
   * This method is called automatically by Spring Security for every incoming
   * HTTP request. It attempts to authenticate the user based on the JWT token in
   * the request header.
   * </p>
   */
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    try {
      String jwt = parseJwt(request);
      if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
        String email = jwtUtils.getUserNameFromJwtToken(jwt);
        UserDetails userDetails = userServiceDetail.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            userDetails, // The authenticated user
            null, // Credentials (not needed after JWT validation)
            userDetails.getAuthorities() // User's roles/permissions
            );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    } catch (Exception e) {
      // Log authentication errors but don't stop the request
      // The request will continue but the user won't be authenticated
      logger.error("Cannot set user authentication: {}", e);
    }
    filterChain.doFilter(request, response);
  }

  /**
   * Extracts JWT token from the Authorization header.
   * 
   * <p>
   * The Authorization header should be in the format:
   * 
   * <pre>
   * Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIn0.signature
   * </pre>
   * </p>
   * 
   * <p>
   * <b>Extraction Process:</b>
   * </p>
   * <ol>
   * <li>Get the "Authorization" header value</li>
   * <li>Check if it exists and starts with "Bearer "</li>
   * <li>Remove the "Bearer " prefix (7 characters)</li>
   * <li>Return the remaining token string</li>
   * </ol>
   * 
   * @param request The HTTP request containing headers
   * @return The JWT token string without "Bearer " prefix, or null if not found
   */
  private String parseJwt(HttpServletRequest request) {
    String headerAuth = request.getHeader("Authorization");

    if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {

      // Remove "Bearer " prefix (first 7 characters) and return the token
      return headerAuth.substring(7);
    }
    return null;
  }

}
