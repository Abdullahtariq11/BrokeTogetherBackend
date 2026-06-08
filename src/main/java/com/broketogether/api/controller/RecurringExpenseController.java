package com.broketogether.api.controller;

import com.broketogether.api.dto.RecurringExpenseRequest;
import com.broketogether.api.dto.RecurringExpenseResponse;
import com.broketogether.api.service.RecurringExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expenses/recurring")
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;

    public RecurringExpenseController(RecurringExpenseService recurringExpenseService) {
        this.recurringExpenseService = recurringExpenseService;
    }

    @PostMapping
    public ResponseEntity<RecurringExpenseResponse> create(
            @Valid @RequestBody RecurringExpenseRequest request) throws AccountNotFoundException {
        return ResponseEntity.status(201).body(recurringExpenseService.create(request));
    }

    @GetMapping("/home/{homeId}")
    public ResponseEntity<List<RecurringExpenseResponse>> getByHome(
            @PathVariable Long homeId) throws AccountNotFoundException {
        return ResponseEntity.ok(recurringExpenseService.getByHome(homeId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) throws AccountNotFoundException {
        recurringExpenseService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
