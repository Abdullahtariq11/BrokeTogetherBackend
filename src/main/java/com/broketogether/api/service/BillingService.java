package com.broketogether.api.service;

import com.broketogether.api.dto.BillingStatusResponse;
import com.broketogether.api.model.User;

import com.broketogether.api.utility.Utility;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.security.auth.login.AccountNotFoundException;

@Service
public class BillingService extends Utility {
    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.price-id}")
    private String priceId;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    public String createCheckoutSession(User user) throws StripeException, AccountNotFoundException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomerEmail(user.getEmail())
                .setSuccessUrl(frontendUrl + "/billing?success=true")
                .setCancelUrl(frontendUrl + "/billing?cancelled=true")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(priceId)
                                .setQuantity(1L)
                                .build()
                )
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }

    public BillingStatusResponse getStatus() throws AccountNotFoundException {
        User user=getUserDetails();
        return new BillingStatusResponse(user.getPremium(),user.getSubscriptionStatus());
    }


}
