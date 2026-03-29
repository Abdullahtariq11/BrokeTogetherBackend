package com.broketogether.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

/**
 * Application context loading test.
 *
 * Disabled in CI because it requires a real PostgreSQL database connection.
 * The unit tests (service + controller) run without a database and provide
 * sufficient coverage for the CI pipeline.
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BrokeTogetherBackendApplicationTests {

  @Test
  void contextLoads() {
    // This test verifies the full Spring context loads correctly.
    // It requires DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD, and JWT_SECRET
    // environment variables, plus a running PostgreSQL instance.
    // Run locally only.
  }

}
