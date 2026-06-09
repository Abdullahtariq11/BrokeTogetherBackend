package com.broketogether.api.scheduler;

import com.broketogether.api.model.Expense;
import com.broketogether.api.model.ExpenseSplit;
import com.broketogether.api.model.Home;
import com.broketogether.api.model.RecurringExpense;
import com.broketogether.api.model.RecurringFrequency;
import com.broketogether.api.model.RecurringSplitType;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.ExpenseRepository;
import com.broketogether.api.repository.HomeRepository;
import com.broketogether.api.repository.RecurringExpenseRepository;
import com.broketogether.api.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import java.time.LocalDate;
import java.util.List;

@Component
public class RecurringExpenseScheduler {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final ExpenseRepository expenseRepository;
    private final HomeRepository homeRepository;
    private final UserRepository userRepository;

    public RecurringExpenseScheduler(RecurringExpenseRepository recurringExpenseRepository,
                                     ExpenseRepository expenseRepository,
                                     HomeRepository homeRepository,
                                     UserRepository userRepository) {
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.expenseRepository = expenseRepository;
        this.homeRepository = homeRepository;
        this.userRepository = userRepository;
    }

    /**
     * Runs every day at midnight.
     * Finds all due recurring expenses and creates actual Expense records.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processDueRecurringExpenses() {
        List<RecurringExpense> dueExpenses = recurringExpenseRepository
            .findByActiveTrueAndNextDueDateLessThanEqual(LocalDate.now());

        for (RecurringExpense recurring : dueExpenses) {
            try {
                Home home = homeRepository.findById(recurring.getHomeId()).orElse(null);
                User payer = userRepository.findById(recurring.getPayerId()).orElse(null);

                if (home == null || payer == null) continue;

                // Create the actual expense
                Expense expense = new Expense();
                expense.setDescription(recurring.getDescription());
                expense.setAmount(recurring.getAmount());
                expense.setCategory(recurring.getCategory());
                expense.setPayer(payer);
                expense.setHome(home);

                // Split based on splitType
                if (recurring.getSplitType() == RecurringSplitType.SPLIT) {
                    // Divide equally among all home members
                    Set<User> members = home.getMembers();
                    if (!members.isEmpty()) {
                        BigDecimal splitAmount = recurring.getAmount()
                            .divide(BigDecimal.valueOf(members.size()), 2, RoundingMode.HALF_UP);
                        List<ExpenseSplit> splits = new ArrayList<>();
                        for (User member : members) {
                            splits.add(new ExpenseSplit(expense, member, splitAmount));
                        }
                        expense.setSplits(splits);
                    }
                } else {
                    // Personal — only the payer owes the full amount
                    List<ExpenseSplit> splits = new ArrayList<>();
                    splits.add(new ExpenseSplit(expense, payer, recurring.getAmount()));
                    expense.setSplits(splits);
                }

                expenseRepository.save(expense);

                // Advance the next due date
                LocalDate nextDate = recurring.getFrequency() == RecurringFrequency.WEEKLY
                    ? recurring.getNextDueDate().plusWeeks(1)
                    : recurring.getNextDueDate().plusMonths(1);

                recurring.setNextDueDate(nextDate);
                recurringExpenseRepository.save(recurring);

            } catch (Exception e) {
                // Log and continue — don't let one failure block the rest
                System.err.println("Failed to process recurring expense id=" +
                    recurring.getId() + ": " + e.getMessage());
            }
        }
    }
}
