package com.broketogether.api.service;

import com.broketogether.api.dto.RecurringExpenseRequest;
import com.broketogether.api.dto.RecurringExpenseResponse;
import com.broketogether.api.model.Home;
import com.broketogether.api.model.RecurringExpense;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.HomeRepository;
import com.broketogether.api.repository.RecurringExpenseRepository;
import com.broketogether.api.utility.Utility;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.security.auth.login.AccountNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecurringExpenseService extends Utility {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final HomeRepository homeRepository;

    public RecurringExpenseService(RecurringExpenseRepository recurringExpenseRepository,
                                   HomeRepository homeRepository) {
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.homeRepository = homeRepository;
    }

    public RecurringExpenseResponse create(RecurringExpenseRequest request)
            throws AccountNotFoundException {
        User user = getUserDetails();

        if (!user.getPremium()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Upgrade to Premium to create recurring expenses.");
        }

        Home home = homeRepository.findById(request.getHomeId())
            .orElseThrow(() -> new RuntimeException("Home not found."));

        boolean isMember = home.getMembers().stream()
            .anyMatch(m -> m.getId().equals(user.getId()));
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You are not a member of this home.");
        }

        RecurringExpense recurring = new RecurringExpense();
        recurring.setHomeId(request.getHomeId());
        recurring.setDescription(request.getDescription());
        recurring.setAmount(request.getAmount());
        recurring.setCategory(request.getCategory());
        recurring.setPayerId(user.getId());
        recurring.setFrequency(request.getFrequency());
        recurring.setSplitType(request.getSplitType() != null ? request.getSplitType() : com.broketogether.api.model.RecurringSplitType.SPLIT);
        recurring.setNextDueDate(LocalDate.now());
        recurring.setActive(true);

        RecurringExpense saved = recurringExpenseRepository.save(recurring);
        return toResponse(saved);
    }

    public List<RecurringExpenseResponse> getByHome(Long homeId) throws AccountNotFoundException {
        User user = getUserDetails();

        if (!user.getPremium()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Upgrade to Premium to view recurring expenses.");
        }

        Home home = homeRepository.findById(homeId)
            .orElseThrow(() -> new RuntimeException("Home not found."));

        boolean isMember = home.getMembers().stream()
            .anyMatch(m -> m.getId().equals(user.getId()));
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You are not a member of this home.");
        }

        return recurringExpenseRepository.findByHomeIdAndActiveTrue(homeId)
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public void deactivate(Long id) throws AccountNotFoundException {
        User user = getUserDetails();

        RecurringExpense recurring = recurringExpenseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Recurring expense not found."));

        if (!recurring.getPayerId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You do not have permission to deactivate this recurring expense.");
        }

        recurring.setActive(false);
        recurringExpenseRepository.save(recurring);
    }

    private RecurringExpenseResponse toResponse(RecurringExpense r) {
        return new RecurringExpenseResponse(
            r.getId(), r.getHomeId(), r.getDescription(), r.getAmount(),
            r.getCategory(), r.getPayerId(), r.getFrequency(), r.getSplitType(),
            r.getNextDueDate(), r.isActive()
        );
    }
}
