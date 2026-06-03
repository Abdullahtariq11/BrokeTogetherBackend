package com.broketogether.api.controller;

import com.broketogether.api.dto.BillingStatusResponse;
import com.broketogether.api.model.User;
import com.broketogether.api.service.BillingService;
import com.stripe.exception.StripeException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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


}
