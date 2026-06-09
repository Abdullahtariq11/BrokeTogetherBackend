package com.broketogether.api.service;

import com.broketogether.api.dto.ExpenseRequest;
import com.broketogether.api.dto.ExpenseResponse;
import com.broketogether.api.dto.ItemRequest;
import com.broketogether.api.dto.ItemResponse;
import com.broketogether.api.model.Home;
import com.broketogether.api.model.ShoppingItem;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.HomeRepository;
import com.broketogether.api.repository.ShoppingItemRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.security.auth.login.AccountNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShoppingItemServiceTest {

    @Mock
    private ShoppingItemRepository shoppingItemRepository;

    @Mock
    private HomeRepository homeRepository;

    @Mock
    private ExpenseService expenseService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ShoppingItemService shoppingItemService;

    private User testUser;
    private User otherUser;
    private Home testHome;
    private ShoppingItem testItem;

    @BeforeEach
    void setUp() {
        testUser = new User("Test User", "test@example.com", "password");
        testUser.setId(1L);

        otherUser = new User("Other User", "other@example.com", "password");
        otherUser.setId(2L);

        testHome = new Home();
        testHome.setId(1L);
        testHome.setName("Test Home");
        testHome.setMembers(new HashSet<>(Set.of(testUser, otherUser)));
        testHome.setCreator(testUser); // testUser is both member and admin

        testItem = new ShoppingItem(LocalDateTime.now(), testHome, null, testUser,
                false, new BigDecimal("10.00"), "Milk");
        testItem.setId(1L);

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(testUser);
        SecurityContextHolder.setContext(securityContext);
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createItem")
    class CreateItemTests {

        @Test
        @DisplayName("Should create item when user is a member of the home")
        void shouldCreateItemWhenUserIsMember() throws AccountNotFoundException {
            ItemRequest request = new ItemRequest("Milk", new BigDecimal("2.99"), 1L);

            when(homeRepository.findById(1L)).thenReturn(Optional.of(testHome));
            when(shoppingItemRepository.save(any(ShoppingItem.class))).thenAnswer(invocation -> {
                ShoppingItem item = invocation.getArgument(0);
                item.setId(1L);
                return item;
            });

            ItemResponse response = shoppingItemService.createItem(request);

            assertNotNull(response);
            assertEquals("Milk", response.name());
            assertEquals(new BigDecimal("2.99"), response.price());
            assertEquals(testUser.getName(), response.addedByName());
            assertNull(response.checkedByName());
            verify(shoppingItemRepository, times(1)).save(any(ShoppingItem.class));
        }

        @Test
        @DisplayName("Should throw exception when user is not a member of the home")
        void shouldThrowWhenUserIsNotMemberOnCreate() {
            ItemRequest request = new ItemRequest("Milk", new BigDecimal("2.99"), 1L);

            Home homeWithoutUser = new Home();
            homeWithoutUser.setId(1L);
            homeWithoutUser.setMembers(new HashSet<>(Set.of(otherUser)));

            when(homeRepository.findById(1L)).thenReturn(Optional.of(homeWithoutUser));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> shoppingItemService.createItem(request));
            assertEquals("You are not a member of this home", ex.getMessage());
            verify(shoppingItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when home is not found on create")
        void shouldThrowWhenHomeNotFoundOnCreate() {
            ItemRequest request = new ItemRequest("Milk", new BigDecimal("2.99"), 999L);

            when(homeRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> shoppingItemService.createItem(request));
            verify(shoppingItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should default isChecked to false when item is created")
        void shouldDefaultIsCheckedToFalseOnCreate() throws AccountNotFoundException {
            ItemRequest request = new ItemRequest("Bread", new BigDecimal("1.50"), 1L);

            when(homeRepository.findById(1L)).thenReturn(Optional.of(testHome));
            when(shoppingItemRepository.save(any(ShoppingItem.class))).thenAnswer(invocation -> {
                ShoppingItem item = invocation.getArgument(0);
                item.setId(2L);
                return item;
            });

            ItemResponse response = shoppingItemService.createItem(request);

            assertFalse(response.isChecked());
        }
    }

    // ─── Get ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllItemsByHomeId")
    class GetItemsTests {

        @Test
        @DisplayName("Should return all items for a home when user is a member")
        void shouldReturnAllItemsForHomeWhenUserIsMember() throws AccountNotFoundException {
            ShoppingItem item2 = new ShoppingItem(LocalDateTime.now(), testHome, null, testUser,
                    false, new BigDecimal("5.00"), "Bread");
            item2.setId(2L);

            when(homeRepository.findById(1L)).thenReturn(Optional.of(testHome));
            when(shoppingItemRepository.findByHomeId(1L)).thenReturn(List.of(testItem, item2));

            List<ItemResponse> responses = shoppingItemService.getAllItemsByHomeId(1L);

            assertEquals(2, responses.size());
        }

        @Test
        @DisplayName("Should throw exception when user is not a member on get")
        void shouldThrowWhenUserIsNotMemberOnGet() {
            Home homeWithoutUser = new Home();
            homeWithoutUser.setId(1L);
            homeWithoutUser.setMembers(new HashSet<>(Set.of(otherUser)));

            when(homeRepository.findById(1L)).thenReturn(Optional.of(homeWithoutUser));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> shoppingItemService.getAllItemsByHomeId(1L));
            assertEquals("You are not a member of this home", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when home is not found on get")
        void shouldThrowWhenHomeNotFoundOnGet() {
            when(homeRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> shoppingItemService.getAllItemsByHomeId(999L));
        }
    }

    // ─── Check / Uncheck ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("markItem")
    class MarkItemTests {

        @Test
        @DisplayName("Should set isChecked to true and record checkedBy when checking an item")
        void shouldCheckItemAndSetCheckedBy() throws AccountNotFoundException {
            when(shoppingItemRepository.findById(1L)).thenReturn(Optional.of(testItem));
            when(shoppingItemRepository.save(any(ShoppingItem.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ItemResponse response = shoppingItemService.markItem(1L);

            assertTrue(response.isChecked());
            assertEquals(testUser.getName(), response.checkedByName());
        }

        @Test
        @DisplayName("Should set isChecked to false and clear checkedBy when unchecking an item")
        void shouldUncheckItemAndClearCheckedBy() throws AccountNotFoundException {
            testItem.setChecked(true);
            testItem.setCheckedBy(testUser);

            when(shoppingItemRepository.findById(1L)).thenReturn(Optional.of(testItem));
            when(shoppingItemRepository.save(any(ShoppingItem.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ItemResponse response = shoppingItemService.markItem(1L);

            assertFalse(response.isChecked());
            assertNull(response.checkedByName());
        }

        @Test
        @DisplayName("Should throw exception when item is not found on check")
        void shouldThrowWhenItemNotFoundOnCheck() {
            when(shoppingItemRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> shoppingItemService.markItem(999L));
            verify(shoppingItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when user is not a member of the home on check")
        void shouldThrowWhenUserIsNotMemberOnCheck() {
            Home homeWithoutUser = new Home();
            homeWithoutUser.setId(1L);
            homeWithoutUser.setMembers(new HashSet<>(Set.of(otherUser)));
            testItem.setHome(homeWithoutUser);

            when(shoppingItemRepository.findById(1L)).thenReturn(Optional.of(testItem));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> shoppingItemService.markItem(1L));
            assertEquals("You are not a member of this home", ex.getMessage());
            verify(shoppingItemRepository, never()).save(any());
        }
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteById")
    class DeleteItemTests {

        @Test
        @DisplayName("Should delete item when user is a member of the home")
        void shouldDeleteItemWhenUserIsMember() {
            when(shoppingItemRepository.findById(1L)).thenReturn(Optional.of(testItem));

            assertDoesNotThrow(() -> shoppingItemService.deleteById(1L));
            verify(shoppingItemRepository, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when item is not found on delete")
        void shouldThrowWhenItemNotFoundOnDelete() {
            when(shoppingItemRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> shoppingItemService.deleteById(999L));
            verify(shoppingItemRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw exception when user is not a member of the home on delete")
        void shouldThrowWhenUserIsNotMemberOnDelete() {
            Home homeWithoutUser = new Home();
            homeWithoutUser.setId(1L);
            homeWithoutUser.setMembers(new HashSet<>(Set.of(otherUser)));
            testItem.setHome(homeWithoutUser);

            when(shoppingItemRepository.findById(1L)).thenReturn(Optional.of(testItem));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> shoppingItemService.deleteById(1L));
            assertEquals("You are not a member of this home", ex.getMessage());
            verify(shoppingItemRepository, never()).deleteById(any());
        }
    }

    // ─── Convert to Expense ───────────────────────────────────────────────────

    @Nested
    @DisplayName("convertToExpense")
    class ConvertToExpenseTests {

        @Test
        @DisplayName("Should create expense split equally among all members when split is true")
        void shouldCreateExpenseWithSplitsWhenSplitIsTrue() throws Exception {
            testItem.setChecked(true);

            ExpenseResponse mockExpenseResponse = new ExpenseResponse(
                    1L, new BigDecimal("10.00"), "Milk", "SHOPPING", Map.of());

            when(shoppingItemRepository.findById(1L)).thenReturn(Optional.of(testItem));
            when(shoppingItemRepository.save(any(ShoppingItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(expenseService.createExpense(any(ExpenseRequest.class))).thenReturn(mockExpenseResponse);

            ExpenseResponse result = shoppingItemService.convertToExpense(1L, true);

            assertNotNull(result);
            assertEquals("SHOPPING", result.getCategory());
            assertTrue(testItem.getConvertedToExpense());
            verify(shoppingItemRepository, times(1)).save(testItem);
            verify(expenseService, times(1)).createExpense(any(ExpenseRequest.class));
            verify(expenseService, never()).createPersonalExpense(any(), any(), any());
        }

        @Test
        @DisplayName("Should create personal expense with no splits when split is false")
        void shouldCreateExpenseWithNoSplitsWhenSplitIsFalse() throws Exception {
            testItem.setChecked(true);

            ExpenseResponse mockExpenseResponse = new ExpenseResponse(
                    1L, new BigDecimal("10.00"), "Milk", "SHOPPING", Map.of());

            when(shoppingItemRepository.findById(1L)).thenReturn(Optional.of(testItem));
            when(shoppingItemRepository.save(any(ShoppingItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(expenseService.createPersonalExpense(any(BigDecimal.class), any(String.class), any(Long.class)))
                    .thenReturn(mockExpenseResponse);

            ExpenseResponse result = shoppingItemService.convertToExpense(1L, false);

            assertNotNull(result);
            assertTrue(testItem.getConvertedToExpense());
            verify(shoppingItemRepository, times(1)).save(testItem);
            verify(expenseService, times(1)).createPersonalExpense(
                    new BigDecimal("10.00"), "Milk", testHome.getId());
            verify(expenseService, never()).createExpense(any(ExpenseRequest.class));
        }

        @Test
        @DisplayName("Should throw exception when item is already converted to an expense")
        void shouldThrowWhenItemAlreadyConverted() throws AccountNotFoundException {
            testItem.setChecked(true);
            testItem.setConvertedToExpense(true);

            when(shoppingItemRepository.findById(1L)).thenReturn(Optional.of(testItem));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> shoppingItemService.convertToExpense(1L, true));
            assertEquals("Item already converted to expense.", ex.getMessage());
            verify(shoppingItemRepository, never()).save(any());
            verify(expenseService, never()).createExpense(any(ExpenseRequest.class));
        }

        @Test
        @DisplayName("Should throw exception when item is not checked before converting")
        void shouldThrowWhenItemNotCheckedOnConvert() throws AccountNotFoundException {
            // testItem.isChecked == false by default from setUp
            when(shoppingItemRepository.findById(1L)).thenReturn(Optional.of(testItem));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> shoppingItemService.convertToExpense(1L, true));
            assertEquals("Item must be checked before converting to an expense.", ex.getMessage());
            verify(expenseService, never()).createExpense(any(ExpenseRequest.class));
        }

        @Test
        @DisplayName("Should throw exception when item is not found on convert")
        void shouldThrowWhenItemNotFoundOnConvert() throws AccountNotFoundException {
            when(shoppingItemRepository.findById(999L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> shoppingItemService.convertToExpense(999L, true));
            assertEquals("No shopping Item found with this id.", ex.getMessage());
            verify(expenseService, never()).createExpense(any(ExpenseRequest.class));
        }

        @Test
        @DisplayName("Should throw exception when user is not a member of the home on convert")
        void shouldThrowWhenUserIsNotMemberOnConvert() throws AccountNotFoundException {
            Home homeWithoutUser = new Home();
            homeWithoutUser.setId(2L);
            homeWithoutUser.setMembers(new HashSet<>(Set.of(otherUser)));
            testItem.setHome(homeWithoutUser);
            testItem.setChecked(true);

            when(shoppingItemRepository.findById(1L)).thenReturn(Optional.of(testItem));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> shoppingItemService.convertToExpense(1L, true));
            assertEquals("You are not a member of this home", ex.getMessage());
            verify(expenseService, never()).createExpense(any(ExpenseRequest.class));
        }
    }
}
