package gg.duo.crew.service;

import gg.duo.common.exception.BusinessException;
import gg.duo.common.exception.ErrorCode;
import gg.duo.crew.domain.schedule.HouseSchedule;
import gg.duo.crew.domain.schedule.HouseScheduleRepository;
import gg.duo.crew.dto.ScheduleDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final HouseScheduleRepository scheduleRepository;
    private final HouseService houseService;

    @Transactional(readOnly = true)
    public List<ScheduleDto.Response> list(Long houseId, Long userId) {
        houseService.requireApprovedMember(houseId, userId);
        return scheduleRepository.findByHouseIdOrderByScheduledAtAsc(houseId).stream()
                .map(s -> ScheduleDto.Response.of(s, userId))
                .toList();
    }

    @Transactional
    public ScheduleDto.Response create(Long houseId, Long userId, ScheduleDto.WriteRequest req) {
        // 일정은 멤버 누구나 만들 수 있다. 관리 권한을 요구하면 부방장 없는 House 가
        // 방장 접속 전까지 아무 일정도 못 잡는다.
        houseService.requireApprovedMember(houseId, userId);

        HouseSchedule schedule = HouseSchedule.builder()
                .houseId(houseId)
                .title(req.title())
                .scheduledAt(req.scheduledAt())
                .maxParticipants(req.maxParticipants())
                .build();
        scheduleRepository.save(schedule);

        // 만든 사람은 자동 참가. 안 그러면 정원 1짜리 일정을 만들고 본인이 못 들어간다.
        schedule.addParticipant(userId);
        return ScheduleDto.Response.of(schedule, userId);
    }

    @Transactional
    public ScheduleDto.Response join(Long houseId, Long scheduleId, Long userId) {
        houseService.requireApprovedMember(houseId, userId);
        HouseSchedule schedule = load(houseId, scheduleId);
        schedule.addParticipant(userId);
        return ScheduleDto.Response.of(schedule, userId);
    }

    @Transactional
    public ScheduleDto.Response leave(Long houseId, Long scheduleId, Long userId) {
        houseService.requireApprovedMember(houseId, userId);
        HouseSchedule schedule = load(houseId, scheduleId);
        schedule.removeParticipant(userId);
        return ScheduleDto.Response.of(schedule, userId);
    }

    @Transactional
    public ScheduleDto.Response update(Long houseId, Long scheduleId, Long userId,
                                       ScheduleDto.WriteRequest req) {
        houseService.requireManagerOf(houseId, userId);
        HouseSchedule schedule = load(houseId, scheduleId);
        schedule.update(req.title(), req.scheduledAt(), req.maxParticipants());
        return ScheduleDto.Response.of(schedule, userId);
    }

    @Transactional
    public void delete(Long houseId, Long scheduleId, Long userId) {
        houseService.requireManagerOf(houseId, userId);
        scheduleRepository.delete(load(houseId, scheduleId));
    }

    private HouseSchedule load(Long houseId, Long scheduleId) {
        HouseSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "일정을 찾을 수 없습니다."));
        // 경로의 houseId 와 일정의 houseId 가 다르면, 다른 House 일정을 남의 멤버십으로
        // 조작할 수 있다. 경로를 신뢰하지 않고 대조한다.
        if (!schedule.getHouseId().equals(houseId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "일정을 찾을 수 없습니다.");
        }
        return schedule;
    }
}
