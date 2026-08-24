package com.example.gamehouse.repository;

import com.example.gamehouse.entity.House;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseRepository extends JpaRepository<House, Long> {
}