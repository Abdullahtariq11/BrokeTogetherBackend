package com.broketogether.api.service;

import com.broketogether.api.repository.HomeRepository;
import com.broketogether.api.repository.ShoppingItemRepository;
import com.broketogether.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShoppingItemService {
    private final ShoppingItemRepository shoppingItemRepository;
    private final UserRepository userRepository;
    private final HomeRepository homeRepository;

    public ShoppingItemService(ShoppingItemRepository shoppingItemRepository,UserRepository userRepository,
                               HomeRepository homeRepository){
        this.homeRepository=homeRepository;
        this.shoppingItemRepository=shoppingItemRepository;
        this.userRepository=userRepository;
    }

    @Transactional
    public


}
