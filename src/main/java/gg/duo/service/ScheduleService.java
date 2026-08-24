package com.example.gamehouse.service;

import com.example.gamehouse.entity.HouseSchedule;
import com.example.gamehouse.repository.HouseScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final HouseScheduleRepository scheduleRepository;

    @Transactional
    public Long createSchedule(Long houseId, String title, LocalDateTime scheduledAt, Integer maxParticipants) {
        HouseSchedule schedule = HouseSchedule.builder()
                .houseId(houseId)
                .title(title)
                .scheduledAt(scheduledAt)
                .maxParticipants(maxParticipants)
                .build();
        return scheduleRepository.save(schedule).getId();
    }

    @Transactional
    public void joinSchedule(Long scheduleId, Long userId) {
        HouseSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));
        schedule.addParticipant(userId);
    }
}