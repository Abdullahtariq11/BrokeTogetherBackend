package com.broketogether.api.controller;

import com.broketogether.api.dto.BillingStatusResponse;
import com.broketogether.api.model.SubscriptionStatus;
import com.broketogether.api.model.User;
import com.broketogether.api.service.BillingService;
import com.broketogether.api.exception.GlobalExceptionHandler;
import com.stripe.exception.StripeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.security.auth.login.AccountNotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class BillingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BillingService billingService;

    @InjectMocks
    private BillingController billingController;

    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(billingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testUser = new User("Test User", "test@example.com", "password");
        testUser.setId(1L);

        // Simulate authenticated user in security context
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ==================== POST /billing/checkout ====================

    @Nested
    @DisplayName("POST /api/v1/billing/checkout")
    class CheckoutTests {

        @Test
        @DisplayName("Should return checkout URL for authenticated user")
        void shouldReturnCheckoutUrl() throws Exception {
            when(billingService.createCheckoutSession(any(User.class)))
                    .thenReturn("https://checkout.stripe.com/pay/test123");

            mockMvc.perform(post("/api/v1/billing/checkout")
                            .principal(SecurityContextHolder.getContext().getAuthentication()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("https://checkout.stripe.com/pay/test123"));
        }

        @Test
        @DisplayName("Should return 500 when Stripe call fails")
        void shouldReturn500WhenStripeFails() throws Exception {
            when(billingService.createCheckoutSession(any(User.class)))
                    .thenThrow(mock(StripeException.class));

            mockMvc.perform(post("/api/v1/billing/checkout")
                            .principal(SecurityContextHolder.getContext().getAuthentication()))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ==================== GET /billing/status ====================

    @Nested
    @DisplayName("GET /api/v1/billing/status")
    class StatusTests {

        @Test
        @DisplayName("Should return billing status for free user")
        void shouldReturnStatusForFreeUser() throws Exception {
            when(billingService.getStatus())
                    .thenReturn(new BillingStatusResponse(false, SubscriptionStatus.NONE));

            mockMvc.perform(get("/api/v1/billing/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isPremium").value(false))
                    .andExpect(jsonPath("$.subscriptionStatus").value("NONE"));
        }

        @Test
        @DisplayName("Should return billing status for premium user")
        void shouldReturnStatusForPremiumUser() throws Exception {
            when(billingService.getStatus())
                    .thenReturn(new BillingStatusResponse(true, SubscriptionStatus.ACTIVE));

            mockMvc.perform(get("/api/v1/billing/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isPremium").value(true))
                    .andExpect(jsonPath("$.subscriptionStatus").value("ACTIVE"));
        }
    }

    // ==================== POST /billing/portal ====================

    @Nested
    @DisplayName("POST /api/v1/billing/portal")
    class PortalTests {

        @Test
        @DisplayName("Should return portal URL for premium user")
        void shouldReturnPortalUrl() throws Exception {
            when(billingService.createPortalSession(any(User.class)))
                    .thenReturn("https://billing.stripe.com/session/test123");

            mockMvc.perform(post("/api/v1/billing/portal")
                            .principal(SecurityContextHolder.getContext().getAuthentication()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("https://billing.stripe.com/session/test123"));
        }

        @Test
        @DisplayName("Should return 400 when user has no Stripe customer ID")
        void shouldReturn400WhenNoStripeCustomer() throws Exception {
            when(billingService.createPortalSession(any(User.class)))
                    .thenThrow(new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.BAD_REQUEST, "No active subscription found."));

            mockMvc.perform(post("/api/v1/billing/portal")
                            .principal(SecurityContextHolder.getContext().getAuthentication()))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== POST /billing/webhook ====================

    @Nested
    @DisplayName("POST /api/v1/billing/webhook")
    class WebhookTests {

        @Test
        @DisplayName("Should return 200 for valid webhook event")
        void shouldReturn200ForValidWebhook() throws Exception {
            doNothing().when(billingService).handleWebhook(any(), any());

            mockMvc.perform(post("/api/v1/billing/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Stripe-Signature", "t=123,v1=abc")
                            .content("{\"type\":\"checkout.session.completed\"}"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("received"));
        }

        @Test
        @DisplayName("Should return 400 for invalid Stripe signature")
        void shouldReturn400ForInvalidSignature() throws Exception {
            doThrow(new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid Stripe signature"))
                    .when(billingService).handleWebhook(any(), any());

            mockMvc.perform(post("/api/v1/billing/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Stripe-Signature", "invalid")
                            .content("{\"type\":\"checkout.session.completed\"}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
