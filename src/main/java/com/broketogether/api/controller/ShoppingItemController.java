package com.broketogether.api.controller;

import com.broketogether.api.dto.ConvertExpenseRequest;
import com.broketogether.api.dto.ExpenseResponse;
import com.broketogether.api.dto.ItemRequest;
import com.broketogether.api.dto.ItemResponse;
import com.broketogether.api.service.ShoppingItemService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;
import java.util.List;

@RestController
@RequestMapping("api/v1/shopping-items")
public class ShoppingItemController {

    private final ShoppingItemService itemService;

    public ShoppingItemController(ShoppingItemService itemService){
        this.itemService=itemService;
    }

    @PostMapping
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody ItemRequest itemRequest) throws AccountNotFoundException {
        return ResponseEntity.status(201).body(itemService.createItem(itemRequest));
    }

    @GetMapping("/item/{itemId}")
    public ResponseEntity<ItemResponse> getItemById(@PathVariable Long itemId) throws AccountNotFoundException {
        return ResponseEntity.ok(itemService.getItemById(itemId));
    }

    @GetMapping("/home/{homeId}/items")
    public ResponseEntity<List<ItemResponse>> getItemsByHomeID(@PathVariable Long homeId) throws AccountNotFoundException {
        return ResponseEntity.ok(itemService.getAllItemsByHomeId(homeId));
    }
    @PutMapping("/item/{itemId}")
    public ResponseEntity<ItemResponse> edit(@Valid @RequestBody ItemRequest itemRequest, @PathVariable Long itemId) throws AccountNotFoundException {
        return ResponseEntity.ok(itemService.editItem(itemId,itemRequest));
    }
    @PatchMapping("/item/mark/{itemId}")
    public ResponseEntity<ItemResponse> mark(@PathVariable Long itemId) throws AccountNotFoundException {
        return ResponseEntity.ok(itemService.markItem(itemId));
    }
    @DeleteMapping("item/{itemId}")
    public ResponseEntity<Void> delete(@PathVariable Long itemId) throws AccountNotFoundException {
        itemService.deleteById(itemId);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/item/{itemId}/convert")
    public ResponseEntity<ExpenseResponse> convert(
            @PathVariable Long itemId,
            @Valid @RequestBody ConvertExpenseRequest request) throws AccountNotFoundException {
        return ResponseEntity.status(201).body(itemService.convertToExpense(itemId, request));
    }
}
