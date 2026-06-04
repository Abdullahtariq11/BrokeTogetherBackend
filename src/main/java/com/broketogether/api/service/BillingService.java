package com.broketogether.api.service;

import com.broketogether.api.dto.BillingStatusResponse;
import com.broketogether.api.model.SubscriptionStatus;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.UserRepository;
import com.broketogether.api.utility.Utility;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.security.auth.login.AccountNotFoundException;

@Service
public class BillingService extends Utility {

    private final UserRepository userRepository;

    public BillingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.price-id}")
    private String priceId;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    public String createCheckoutSession(User user) throws StripeException {
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
        User user = getUserDetails();
        return new BillingStatusResponse(user.getPremium(), user.getSubscriptionStatus());
    }

    public void handleWebhook(String payload, String sigHeader) throws StripeException {
        // 1. Verify the request actually came from Stripe
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe signature");
        }

        // 2. Deserialize the event data object
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = deserializer.getObject().orElse(null);
        if (stripeObject == null) return;

        // 3. Handle each event type
        switch (event.getType()) {

            case "checkout.session.completed" -> {
                Session session = (Session) stripeObject;
                userRepository.findByEmail(session.getCustomerEmail()).ifPresent(user -> {
                    user.setPremium(true);
                    user.setStripeCustomerId(session.getCustomer());
                    user.setSubscriptionId(session.getSubscription());
                    user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
                    userRepository.save(user);
                });
            }

            case "invoice.payment_succeeded" -> {
                Invoice invoice = (Invoice) stripeObject;
                userRepository.findByStripeCustomerId(invoice.getCustomer()).ifPresent(user -> {
                    user.setPremium(true);
                    user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
                    userRepository.save(user);
                });
            }

            case "invoice.payment_failed" -> {
                Invoice invoice = (Invoice) stripeObject;
                userRepository.findByStripeCustomerId(invoice.getCustomer()).ifPresent(user -> {
                    user.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
                    userRepository.save(user);
                });
            }

            case "customer.subscription.deleted" -> {
                Subscription subscription = (Subscription) stripeObject;
                userRepository.findByStripeCustomerId(subscription.getCustomer()).ifPresent(user -> {
                    user.setPremium(false);
                    user.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
                    userRepository.save(user);
                });
            }
        }
    }
}
