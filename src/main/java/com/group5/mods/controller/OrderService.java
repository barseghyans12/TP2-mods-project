package com.group5.mods.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import com.group5.mods.model.BasketProduct;
import com.group5.mods.model.Order;
import com.group5.mods.model.OrderProduct;
import com.group5.mods.model.OrderStatus;
import com.group5.mods.model.User;
import com.group5.mods.repository.OrderProductRepository;
import com.group5.mods.repository.OrderRepository;

public class OrderService {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderProductRepository orderProductRepository;

    public Order createOrder(User user, List<BasketProduct> basketProducts) {
        Order order = new Order();
        order.setUser(user);
        order.setDateCreated(LocalDateTime.now());
        order.setStatus(OrderStatus.INPROGRESS);
        order.setTotalPrice(100.00);

        List<OrderProduct> orderProducts = new ArrayList<>();
        for (BasketProduct basketProduct : basketProducts){
            OrderProduct orderProduct = new OrderProduct();
            orderProduct.setProduct(basketProduct.getProduct());
            orderProduct.setQuantity(basketProduct.getQuantity());
            orderProduct.setOrder(order);
            orderProducts.add(orderProduct);
        }

        order.setOrderProducts(orderProducts);

        // Save the order in orders table
        order = orderRepository.save(order);

        // Save each order product in the order_products table
        for (OrderProduct orderProduct : orderProducts){
            orderProductRepository.save(orderProduct);
        }

        return order;
    }

    public List<Order> getOrdersByUser(User user){
        return orderRepository.findByUser(user);
    }



}
