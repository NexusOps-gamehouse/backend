package com.example.gamehouse.repository;

import com.example.gamehouse.entity.HouseNotice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseNoticeRepository extends JpaRepository<HouseNotice, Long> {
}