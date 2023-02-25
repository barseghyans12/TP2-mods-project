package com.group5.mods.controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.group5.mods.model.Basket;
import com.group5.mods.model.BasketProduct;
import com.group5.mods.model.Order;
import com.group5.mods.model.SecurityUser;
import com.group5.mods.model.User;
import com.group5.mods.service.BasketService;
import com.group5.mods.service.UserService;

@Controller
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private BasketService basketService;

    @PostMapping("/checkout")
    public String placeOrder(Model model){
        // Getting the authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        User user = securityUser.getUser();
        // Getting basket, basket products, and creating a new order object with the user and basket products
        Optional<Basket> basket = basketService.findByUser(user);
        List<BasketProduct> basketProducts = basket.get().getBasketProducts();
        Order order = orderService.createOrder(user, basketProducts);
        // Removing everything from basked since order was created, and saving new empty basket to the database
        basketProducts.clear();
        basketService.save(basket.get());
        return "redirect:/orders";
    }

    @GetMapping("/orders")
    public String getOrders(Model model){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        User user = securityUser.getUser();
        List<Order> orders =  orderService.getOrdersByUser(user);
        model.addAttribute("orders", orders);
        return "orders";
    }
}