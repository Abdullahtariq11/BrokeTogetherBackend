package com.broketogether.api.service;

import java.net.URI;
import java.text.ParseException;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import jakarta.annotation.PostConstruct;

/**
 * Verifies "Sign in with Apple" identity tokens (JWTs) sent by the mobile app.
 *
 * <p>
 * Apple's public keys are fetched (and cached) from their JWKS endpoint, so we
 * never need to store or rotate keys ourselves. Verification checks the RS256
 * signature, issuer, audience (our bundle ID), and expiration.
 */
@Service
public class AppleIdTokenVerifier {

  private static final String APPLE_ISSUER = "https://appleid.apple.com";
  private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";

  @Value("${apple.bundle-id}")
  private String bundleId;

  private ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

  @PostConstruct
  public void init() throws Exception {
    JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(URI.create(APPLE_JWKS_URL).toURL());

    ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
    processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource));
    processor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
        new JWTClaimsSet.Builder().issuer(APPLE_ISSUER).audience(bundleId).build(),
        Set.of("sub", "email", "exp", "iat")));

    this.jwtProcessor = processor;
  }

  /**
   * Verifies the token's signature and claims (issuer, audience, expiry) and
   * returns its claim set.
   *
   * @throws IllegalArgumentException if the token is malformed, expired, or was
   *                                  not issued by Apple for this app
   */
  public JWTClaimsSet verify(String identityToken) {
    try {
      return jwtProcessor.process(identityToken, null);
    } catch (ParseException | BadJOSEException | JOSEException e) {
      throw new IllegalArgumentException("Invalid Apple identity token.", e);
    }
  }
}
