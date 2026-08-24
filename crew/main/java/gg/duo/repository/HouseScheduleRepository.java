package com.example.gamehouse.repository;

import com.example.gamehouse.entity.HouseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseScheduleRepository extends JpaRepository<HouseSchedule, Long> {
}