package com.example.gamehouse.repository;

import gg.duo.entity.HouseMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HouseMemberRepository extends JpaRepository<HouseMember, Long> {
    Optional<HouseMember> findByHouseIdAndUserId(Long houseId, Long userId);
}