package com.group5.mods.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.group5.mods.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

}