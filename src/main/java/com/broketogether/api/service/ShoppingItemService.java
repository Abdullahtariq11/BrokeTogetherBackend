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
import com.broketogether.api.utility.Utility;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.login.AccountNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ShoppingItemService extends Utility {
    private final ShoppingItemRepository shoppingItemRepository;
    private final HomeRepository homeRepository;
    private final ExpenseService expenseService;

    public ShoppingItemService(ShoppingItemRepository shoppingItemRepository,
                               HomeRepository homeRepository,
                               ExpenseService expenseService) {
        this.homeRepository = homeRepository;
        this.shoppingItemRepository = shoppingItemRepository;
        this.expenseService = expenseService;
    }

    /**
     * Create shopping list item.
     */
    @Transactional
    public ItemResponse createItem(ItemRequest itemRequest) throws AccountNotFoundException {
        Home home = homeRepository.findById(itemRequest.homeId())
                .orElseThrow(() -> new RuntimeException("Home does not exist with this Id"));
        User userDetails = getUserDetails();
        checkUserMemberOfHome(home, userDetails);

        ShoppingItem shoppingItem = new ShoppingItem(LocalDateTime.now(), home,
                null, userDetails, false,
                itemRequest.price(), itemRequest.name());

        shoppingItemRepository.save(shoppingItem);

        return new ItemResponse(shoppingItem.getId(), shoppingItem.getName(), shoppingItem.getPrice(),
                shoppingItem.getChecked(), shoppingItem.getAddedBy().getName(),
                shoppingItem.getCheckedBy() != null ? shoppingItem.getCheckedBy().getName() : null,
                shoppingItem.getCreatedAt(),shoppingItem.getConvertedToExpense());
    }

    /**
     * Finds shopping item using an id.
     *
     * @param itemId id used to fetch details
     * @return ItemResponse
     */
    @Transactional(readOnly = true)
    public ItemResponse getItemById(Long itemId) throws AccountNotFoundException {
        ShoppingItem shoppingItem = shoppingItemRepository
                .findById(itemId)
                .orElseThrow(() -> new RuntimeException("No shopping Item found with this id."));
        User userDetails = getUserDetails();
        checkUserMemberOfHome(shoppingItem.getHome(), userDetails);
        return new ItemResponse(shoppingItem.getId(), shoppingItem.getName(), shoppingItem.getPrice(),
                shoppingItem.getChecked(), shoppingItem.getAddedBy().getName(),
                shoppingItem.getCheckedBy() != null ? shoppingItem.getCheckedBy().getName() : null,
                shoppingItem.getCreatedAt(),shoppingItem.getConvertedToExpense());
    }

    /**
     * Return all items for a home.
     *
     * @param homeId user home id to fetch all items
     * @return List<ItemResponse> all items are returned as an array list
     */
    @Transactional(readOnly = true)
    public List<ItemResponse> getAllItemsByHomeId(Long homeId) throws AccountNotFoundException {
        Home home = homeRepository.findById(homeId)
                .orElseThrow(() -> new RuntimeException("Home not found."));
        User userDetails = getUserDetails();
        checkUserMemberOfHome(home, userDetails);
        List<ShoppingItem> shoppingItems = shoppingItemRepository.findByHomeId(homeId);
        List<ItemResponse> items = new ArrayList<>();
        for (ShoppingItem shoppingItem : shoppingItems) {
            ItemResponse itemResponse = new ItemResponse(shoppingItem.getId(), shoppingItem.getName(), shoppingItem.getPrice(),
                    shoppingItem.getChecked(), shoppingItem.getAddedBy().getName(),
                    shoppingItem.getCheckedBy() != null ? shoppingItem.getCheckedBy().getName() : null,
                    shoppingItem.getCreatedAt(),shoppingItem.getConvertedToExpense());
            items.add(itemResponse);
        }
        return items;
    }

    /**
     * Deletes an item.
     *
     * @param itemId fetches item by its id
     */
    @Transactional
    public void deleteById(Long itemId) throws AccountNotFoundException {
        ShoppingItem shoppingItem = shoppingItemRepository
                .findById(itemId)
                .orElseThrow(() -> new RuntimeException("No shopping Item found with this id."));
        User userDetails = getUserDetails();
        checkUserMemberOfHome(shoppingItem.getHome(), userDetails);
        shoppingItemRepository.deleteById(itemId);
    }

    /**
     * Edits item detail.
     *
     * @param itemId      to fetch item
     * @param itemRequest changes needs to be made in item
     * @return ItemResponse
     */
    @Transactional
    public ItemResponse editItem(Long itemId, ItemRequest itemRequest) throws AccountNotFoundException {
        ShoppingItem shoppingItem = shoppingItemRepository
                .findById(itemId)
                .orElseThrow(() -> new RuntimeException("No shopping Item found with this id."));
        User userDetails = getUserDetails();
        checkUserMemberOfHome(shoppingItem.getHome(), userDetails);
        shoppingItem.setName(itemRequest.name());
        shoppingItem.setPrice(itemRequest.price());

        shoppingItemRepository.save(shoppingItem);
        return new ItemResponse(shoppingItem.getId(), shoppingItem.getName(), shoppingItem.getPrice(),
                shoppingItem.getChecked(), shoppingItem.getAddedBy().getName(),
                shoppingItem.getCheckedBy() != null ? shoppingItem.getCheckedBy().getName() : null,
                shoppingItem.getCreatedAt(),shoppingItem.getConvertedToExpense());
    }

    /**
     * Marks item bought or not bought.
     *
     * @param itemId to fetch item
     * @return ItemResponse
     */
    @Transactional
    public ItemResponse markItem(Long itemId) throws AccountNotFoundException {
        ShoppingItem shoppingItem = shoppingItemRepository
                .findById(itemId)
                .orElseThrow(() -> new RuntimeException("No shopping Item found with this id."));
        User userDetails = getUserDetails();
        checkUserMemberOfHome(shoppingItem.getHome(), userDetails);
        if (shoppingItem.getCheckedBy() == null) {
            shoppingItem.setCheckedBy(userDetails);
        } else {
            shoppingItem.setCheckedBy(null);
        }

        shoppingItem.setChecked(!shoppingItem.getChecked());

        shoppingItemRepository.save(shoppingItem);
        return new ItemResponse(shoppingItem.getId(), shoppingItem.getName(), shoppingItem.getPrice(),
                shoppingItem.getChecked(), shoppingItem.getAddedBy().getName(),
                shoppingItem.getCheckedBy() != null ? shoppingItem.getCheckedBy().getName() : null,
                shoppingItem.getCreatedAt(),shoppingItem.getConvertedToExpense());
    }

    /**
     * Converts a checked shopping item to an expense.
     *
     * @param itemId id of the item to convert
     * @param split  true = equal split among all home members, false = personal expense (no splits)
     * @return ExpenseResponse
     */
    @Transactional
    public ExpenseResponse convertToExpense(Long itemId, boolean split) throws AccountNotFoundException {
        ShoppingItem shoppingItem = shoppingItemRepository
                .findById(itemId)
                .orElseThrow(() -> new RuntimeException("No shopping Item found with this id."));
        User userDetails = getUserDetails();
        checkUserMemberOfHome(shoppingItem.getHome(), userDetails);

        if(shoppingItem.getConvertedToExpense()){
            throw new RuntimeException("Item already converted to expense.");
        }
        if (!shoppingItem.getChecked()) {
            throw new RuntimeException("Item must be checked before converting to an expense.");
        }
        if (shoppingItem.getPrice() == null) {
            throw new RuntimeException("Item must have a price to be converted to an expense.");
        }

        shoppingItem.setConvertedToExpense(true);
        shoppingItemRepository.save(shoppingItem);

        if (split) {
            ExpenseRequest request = new ExpenseRequest(
                    shoppingItem.getPrice(),
                    shoppingItem.getName(),
                    "SHOPPING",
                    shoppingItem.getHome().getId());
            return expenseService.createExpense(request);
        } else {
            return expenseService.createPersonalExpense(
                    shoppingItem.getPrice(),
                    shoppingItem.getName(),
                    shoppingItem.getHome().getId());
        }
    }
}
