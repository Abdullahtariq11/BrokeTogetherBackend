package com.broketogether.api.dto;

import com.broketogether.api.model.SubscriptionStatus;

import java.time.LocalDate;

public record BillingStatusResponse(
    Boolean isPremium,
    SubscriptionStatus subscriptionStatus,
    /** Next renewal date (null if no active subscription). */
    LocalDate nextRenewalDate,
    /** Last 4 digits of the payment card on file (null if unknown). */
    String cardLast4,
    /** URL to cancel / manage the subscription (Stripe portal URL). */
    String manageUrl
) {
    /** Backwards-compatible constructor for callers that don't supply billing detail. */
    public BillingStatusResponse(Boolean isPremium, SubscriptionStatus subscriptionStatus) {
        this(isPremium, subscriptionStatus, null, null, null);
    }
}
