package com.broketogether.api.service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ShoppingItemServiceTest {
    // ─── Create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should create item when user is a member of the home")
    void shouldCreateItemWhenUserIsMember() {
        fail("Implement this method");
    }

    @Test
    @DisplayName("Should throw exception when user is not a member of the home")
    void shouldThrowWhenUserIsNotMemberOnCreate() {
        fail("Implement this method");
    }

    @Test
    @DisplayName("Should throw exception when home is not found on create")
    void shouldThrowWhenHomeNotFoundOnCreate() {
        fail("Implement this method");
    }

    @Test
    @DisplayName("Should default isChecked to false when item is created")
    void shouldDefaultIsCheckedToFalseOnCreate() {
        fail("Implement this method");
    }

    // ─── Get ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return all items for a home when user is a member")
    void shouldReturnAllItemsForHomeWhenUserIsMember() {
        fail("Implement this method");
    }

    @Test
    @DisplayName("Should throw exception when user is not a member on get")
    void shouldThrowWhenUserIsNotMemberOnGet() {
        fail("Implement this method");
    }

    @Test
    @DisplayName("Should throw exception when home is not found on get")
    void shouldThrowWhenHomeNotFoundOnGet() {
        fail("Implement this method");
    }

    // ─── Check / Uncheck ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Should set isChecked to true and record checkedBy when checking an item")
    void shouldCheckItemAndSetCheckedBy() {
        fail("Implement this method");
    }

    @Test
    @DisplayName("Should set isChecked to false and clear checkedBy when unchecking an item")
    void shouldUncheckItemAndClearCheckedBy() {
        fail("Implement this method");
    }

    @Test
    @DisplayName("Should throw exception when item is not found on check")
    void shouldThrowWhenItemNotFoundOnCheck() {
        fail("Implement this method");
    }

    @Test
    @DisplayName("Should throw exception when user is not a member of the home on check")
    void shouldThrowWhenUserIsNotMemberOnCheck() {
        fail("Implement this method");
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should delete item when user is a member of the home")
    void shouldDeleteItemWhenUserIsMember() {
        fail("Implement this method");
    }

    @Test
    @DisplayName("Should throw exception when item is not found on delete")
    void shouldThrowWhenItemNotFoundOnDelete() {
        fail("Implement this method");
    }

    @Test
    @DisplayName("Should throw exception when user is not a member of the home on delete")
    void shouldThrowWhenUserIsNotMemberOnDelete() {
        fail("Implement this method");
    }

    // ─── Convert to Expense ───────────────────────────────────────────────────

    @Test
    @DisplayName("Should create expense split equally among all members when split is true")
    void shouldCreateExpenseWithSplitsWhenSplitIsTrue() {
        fail("Implement this method");
    }

    @Test
    @DisplayName("Should create personal expense with no splits when split is false")
    void shouldCreateExpenseWithNoSplitsWhenSplitIsFalse() {
        fail("Implement this method");
    }

    @Test
    @DisplayName("Should throw exception when item is not checked before converting")
    void shouldThrowWhenItemNotCheckedOnConvert() {
        fail("Implement this method");
    }

    @Test
    @DisplayName("Should throw exception when item is not found on convert")
    void shouldThrowWhenItemNotFoundOnConvert() {
        fail("Implement this method");
    }

    @Test
    @DisplayName("Should throw exception when user is not a member of the home on convert")
    void shouldThrowWhenUserIsNotMemberOnConvert() {
        fail("Implement this method");
    }



}