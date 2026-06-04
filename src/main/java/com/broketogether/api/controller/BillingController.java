package com.broketogether.api.controller;

import com.broketogether.api.dto.BillingStatusResponse;
import com.broketogether.api.model.User;
import com.broketogether.api.service.BillingService;
import com.stripe.exception.StripeException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;


@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private BillingService billingService;

    BillingController(BillingService billingService){
        this.billingService=billingService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<String> checkout(@AuthenticationPrincipal User currentUser) throws StripeException, AccountNotFoundException {
        String url = billingService.createCheckoutSession(currentUser);
        return ResponseEntity.ok(url);
    }

    @GetMapping("/status")
    public ResponseEntity<BillingStatusResponse> getStatus() throws AccountNotFoundException {
        return ResponseEntity.ok(billingService.getStatus()) ;
    }

    @PostMapping("/portal")
    public ResponseEntity<String> portal(@AuthenticationPrincipal User currentUser) throws StripeException {
        String url = billingService.createPortalSession(currentUser);
        return ResponseEntity.ok(url);
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) throws StripeException {
        billingService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok("received");
    }




}
