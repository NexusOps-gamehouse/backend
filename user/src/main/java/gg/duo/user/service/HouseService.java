package com.example.gamehouse.service;

import com.example.gamehouse.entity.*;
import com.example.gamehouse.repository.HouseMemberRepository;
import com.example.gamehouse.repository.HouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HouseService {

    private final HouseRepository houseRepository;
    private final HouseMemberRepository houseMemberRepository;

    @Transactional
    public Long createHouse(Long leaderId, String name, String desc, HouseType type) {
        House house = House.builder()
                .name(name)
                .description(desc)
                .type(type)
                .leaderId(leaderId)
                .build();
        houseRepository.save(house);

        HouseMember leader = HouseMember.builder()
                .house(house)
                .userId(leaderId)
                .role(MemberRole.LEADER)
                .status(JoinStatus.APPROVED)
                .build();
        houseMemberRepository.save(leader);

        return house.getId();
    }

    @Transactional
    public void applyToHouse(Long userId, Long houseId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new IllegalArgumentException("House를 찾을 수 없습니다."));

        HouseMember member = HouseMember.builder()
                .house(house)
                .userId(userId)
                .role(MemberRole.MEMBER)
                .status(house.getType() == HouseType.PUBLIC ? JoinStatus.PENDING : JoinStatus.APPROVED)
                .build();

        houseMemberRepository.save(member);
    }

    @Transactional
    public void approveMember(Long requesterId, Long houseId, Long targetUserId) {
        HouseMember requester = houseMemberRepository.findByHouseIdAndUserId(houseId, requesterId)
                .orElseThrow(() -> new IllegalArgumentException("권한이 없습니다."));

        if (requester.getRole() == MemberRole.MEMBER || requester.getStatus() != JoinStatus.APPROVED) {
            throw new IllegalStateException("승인 권한이 없습니다.");
        }

        HouseMember target = houseMemberRepository.findByHouseIdAndUserId(houseId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("신청 내역이 없습니다."));

        target.approve();
    }
}