package com.example.gamehouse.service;

import com.example.gamehouse.repository.GameMatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HouseRecommendationService {

    private final GameMatchRepository gameMatchRepository;

    @Transactional(readOnly = true)
    public List<Long> getRecommendedHouseMembers(Long userId) {
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(7);
        return gameMatchRepository.findFrequentPlaymates(userId, thresholdDate, 3L);
    }
}