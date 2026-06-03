package com.broketogether.api.dto;

import com.broketogether.api.model.SubscriptionStatus;

public record BillingStatusResponse(Boolean isPremium, SubscriptionStatus subscriptionStatus) {
}
