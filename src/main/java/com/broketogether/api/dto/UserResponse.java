package com.broketogether.api.dto;

import com.broketogether.api.model.SubscriptionStatus;

public record UserResponse(Long id, String name, String email, Boolean isPremium, SubscriptionStatus subscriptionStatus) {
}
