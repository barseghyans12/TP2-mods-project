package com.group5.mods.controller;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group5.mods.model.Order;
import com.group5.mods.model.OrderProduct;
import com.group5.mods.model.OrderStatus;
import com.group5.mods.model.Product;
import com.group5.mods.model.User;
import com.group5.mods.repository.OrderRepository;
import com.group5.mods.repository.ProductRepository;
import com.group5.mods.repository.UserRepository;

@Controller
public class AdminController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(Model model) {
        List<Order> allOrders = orderRepository.findAll();
        List<Order> monthlyOrders = new ArrayList<>();
        List<Order> cancelledOrders = new ArrayList<>();
        BigDecimal monthlyRevenue = BigDecimal.ZERO;
        Map<LocalDate, Integer> ordersByDate = new HashMap<>();
        // All monthly orders
        for(Order order : allOrders){
            if(order.getDateCreated().isAfter(LocalDateTime.now().minusMonths(1))){
                monthlyOrders.add(order);
            }
        }
        // All monthyl Cancelled and Delivered orders (revenue)
        for(Order order : monthlyOrders){
            if(order.getStatus().equals(OrderStatus.CANCELLED)){
                cancelledOrders.add(order);
            }
            if(order.getStatus().equals(OrderStatus.DELIVERED)){
                monthlyRevenue = monthlyRevenue.add(order.getTotalPrice());
            }
        }
        // Loop over all the orders and add the quantity of each order product to the date
        for(Order order : allOrders){
            int orderProductsQuantity = 0;
            for(OrderProduct orderProduct : order.getOrderProducts()){
                orderProductsQuantity += orderProduct.getQuantity();
            }

            LocalDate orderDate = order.getDateCreated().toLocalDate();
            ordersByDate.put(orderDate,ordersByDate.getOrDefault(orderDate, 0) + orderProductsQuantity);
            System.out.println(orderProductsQuantity);
        }

        // Split ordersByDate map into separate arrays for chart.js
        List<String> dates = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        List<LocalDate> sortedDates = new ArrayList<>(ordersByDate.keySet());
        Collections.sort(sortedDates);
        for(LocalDate date : sortedDates){
            int quantity = ordersByDate.get(date);
            dates.add(date.toString());
            quantities.add(quantity);
    }

        model.addAttribute("monthlyOrders", monthlyOrders);
        model.addAttribute("cancelledOrders", cancelledOrders);
        model.addAttribute("monthlyRevenue", monthlyRevenue);
        model.addAttribute("dates", dates);
        model.addAttribute("quantities", quantities);
        return "admin/admin_dashboard";
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminUsers(Model model) {
        List<User> userList = new ArrayList<>();
        userList = userRepository.findAll();
        model.addAttribute("users", userList);
        return "admin/admin_users";
    }

    @GetMapping("/admin/products")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminProducts(Model model) {
        List<Product> productList = new ArrayList<>();
        productList = productRepository.findAll();
        model.addAttribute("products", productList);
        return "admin/admin_products";
    }

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminOrders(Model model) {
        List<Order> orderList = new ArrayList<>();
        orderList = orderRepository.findAll();
        model.addAttribute("orders", orderList);
        model.addAttribute("orderStatusEnum", OrderStatus.values());
        return "admin/admin_orders";
    }

    @PostMapping("/admin/orders/updateStatus")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateOrderStatus(@RequestParam("orderId") Long orderId, @RequestParam("orderStatus") OrderStatus orderStatus) {
        // Retrieve the order using the orderId
        Optional<Order> order = orderRepository.findById(orderId);
        // update the status in the updatedOrder
        order.get().setStatus(orderStatus);
        // Save updated order to database
        orderRepository.save(order.get());

        return "redirect:/admin/orders";
    }

    @PostMapping("/admin/users/updateRole")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateUserRole(@RequestParam("userId") Long userId, @RequestParam("userRole") String userRole) {
        // Retrieve the user using the userId
        Optional<User> user = userRepository.findById(userId);
        // update the role in the updatedOrder
        user.get().setRoles(userRole);
        // Save updated order to database
        userRepository.save(user.get());

        return "redirect:/admin/users";
    }

    @GetMapping("/admin/products/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String showAddProduct(Model model) {
        Map<String, List<String>> makeModelMap = new HashMap<>();
        makeModelMap.put("Audi", Arrays.asList("A5", "Q5", "S5"));
        makeModelMap.put("BMW", Arrays.asList("Z4", "X5", "M4"));

        Set<String> makeSet = makeModelMap.keySet();
        model.addAttribute("makeModelMap", makeModelMap);
        model.addAttribute("makeSet", makeSet);
        model.addAttribute("product", new Product());
        
        return "admin/admin_addProduct";
    }

    @GetMapping("/admin/products/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String editProduct(@PathVariable Long id, Model model) {
        // Retrieve product from database with id
        Optional<Product> product = productRepository.findById(id);
        model.addAttribute("product", product.get());
        // Pass the list of makes and models for the dropdowns
        Map<String, List<String>> makeModelMap = new HashMap<>();
        makeModelMap.put("Audi", Arrays.asList("A5", "Q5", "S5"));
        makeModelMap.put("BMW", Arrays.asList("Z4", "X5", "M4"));

        Set<String> makeSet = makeModelMap.keySet();
        model.addAttribute("makeModelMap", makeModelMap);
        model.addAttribute("makeSet", makeSet);
        
        return "admin/admin_editProduct";
    }

    @PostMapping("/admin/products/addProduct")
    @PreAuthorize("hasRole('ADMIN')")
    public String addProduct(@ModelAttribute("product") Product product, @RequestParam("file") MultipartFile file, BindingResult result, Model model, RedirectAttributes redirectAttributes) throws IOException {
        Map<String, List<String>> makeModelMap = new HashMap<>();
        makeModelMap.put("Audi", Arrays.asList("A5", "Q5", "S5"));
        makeModelMap.put("BMW", Arrays.asList("Z4", "X5", "M4"));
        Set<String> makeSet = makeModelMap.keySet();
        model.addAttribute("makeModelMap", makeModelMap);
        model.addAttribute("makeSet", makeSet);

        // Check if product name already exists, if it doesn then send error to thymeleaf page
        Optional<Product> existingProduct = productRepository.findByName(product.getName());
        if(existingProduct.isPresent()){
            model.addAttribute("ProductNameExists", "There already exists a product with the same name, please change the name!");
            return "admin/admin_addProduct";
        }

        //Save the image file
        if(file!=null && !file.isEmpty()){
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            Path filePath = Paths.get("src/main/resources/static/images/" + fileName);
            try (InputStream inputStream = file.getInputStream()){
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
    
            // Set the image value in the product
            String imageURL = "/images/" + fileName;
            product.setImage(imageURL);
        }else{
            product.setImage("/images/image-missing-stars-mods.jpg");
        }

        // Add product to repository
        productRepository.save(product);


        redirectAttributes.addFlashAttribute("addProductSuccess", "Product successfully added!");
        return "redirect:/admin/products/add";
    }

    @PostMapping("/admin/products/updateAddProductForm")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateAddProductForm(@ModelAttribute("product") Product product, BindingResult result, Model model) {
        Map<String, List<String>> makeModelMap = new HashMap<>();
        makeModelMap.put("Audi", Arrays.asList("A5", "Q5", "S5"));
        makeModelMap.put("BMW", Arrays.asList("Z4", "X5", "M4"));
        Set<String> makeSet = makeModelMap.keySet();
        List<String> models = makeModelMap.get(product.getMake());

        model.addAttribute("models", models);
        model.addAttribute("makeSet", makeSet);
        model.addAttribute("makeModelMap", makeModelMap);
        model.addAttribute("product", product);

        // Check if product name already exists, if it doesn then send error to thymeleaf page
        Optional<Product> existingProduct = productRepository.findByName(product.getName());
        if(existingProduct.isPresent()){
            model.addAttribute("ProductNameExists", "There already exists a product with the same name, please change the name!");
        }
        return "/admin/admin_addProduct";
    }

    @PostMapping("/admin/products/editProduct")
    @PreAuthorize("hasRole('ADMIN')")
    public String editProduct(@ModelAttribute("product") Product product, 
                                @RequestParam(value = "file", required = false) MultipartFile file, 
                                BindingResult result, Model model, 
                                RedirectAttributes redirectAttributes) throws IOException {
        Map<String, List<String>> makeModelMap = new HashMap<>();
        makeModelMap.put("Audi", Arrays.asList("A5", "Q5", "S5"));
        makeModelMap.put("BMW", Arrays.asList("Z4", "X5", "M4"));
        Set<String> makeSet = makeModelMap.keySet();
        model.addAttribute("makeModelMap", makeModelMap);
        model.addAttribute("makeSet", makeSet);
        // Check if product name already exists, if it doesn then send error to thymeleaf page
        Optional<Product> existingProduct = productRepository.findByNameAndIdNot(product.getName(), product.getId());
        if(existingProduct.isPresent()){
            model.addAttribute("ProductNameExists", "There already exists a product with the same name, please change the name!");
            return "admin/admin_editProduct";
        }
        //Save the image file if a new image was uploaded
        if(file != null && !file.isEmpty()){
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            Path filePath = Paths.get("src/main/resources/static/images/" + fileName);
            try (InputStream inputStream = file.getInputStream()){
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            // Set the image value in the product
            String imageURL = "/images/" + fileName;
            product.setImage(imageURL);
        }
        // Add product to repository
        productRepository.save(product);

        redirectAttributes.addFlashAttribute("editProductSuccess", "Product successfully updated!");
        return "redirect:/admin/products/edit/" + product.getId();
    }

    @PostMapping("/admin/products/updateEditProductForm")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateEditProductForm(@ModelAttribute("product") Product product, BindingResult result, Model model) {
        Map<String, List<String>> makeModelMap = new HashMap<>();
        makeModelMap.put("Audi", Arrays.asList("A5", "Q5", "S5"));
        makeModelMap.put("BMW", Arrays.asList("Z4", "X5", "M4"));
        Set<String> makeSet = makeModelMap.keySet();
        List<String> models = makeModelMap.get(product.getMake());

        model.addAttribute("models", models);
        model.addAttribute("makeSet", makeSet);
        model.addAttribute("makeModelMap", makeModelMap);
        model.addAttribute("product", product);

        // Check if product name already exists, if it doesn then send error to thymeleaf page
        Optional<Product> existingProduct = productRepository.findByNameAndIdNot(product.getName(), product.getId());
        if(existingProduct.isPresent()){
            model.addAttribute("ProductNameExists", "There already exists a product with the same name, please change the name!");
            return "admin/admin_editProduct";
        }
        return "/admin/admin_editProduct";
    }
}
