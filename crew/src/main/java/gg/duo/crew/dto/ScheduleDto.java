package gg.duo.crew.dto;

import gg.duo.crew.domain.schedule.HouseSchedule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public final class ScheduleDto {

    private ScheduleDto() {}

    public record WriteRequest(
            @NotBlank(message = "일정 제목은 필수입니다.") String title,
            @NotNull(message = "일정 시각은 필수입니다.") LocalDateTime scheduledAt,
            Integer maxParticipants) {}

    public record Response(
            Long id,
            Long houseId,
            String title,
            LocalDateTime scheduledAt,
            int maxParticipants,
            int participantCount,
            List<Long> participantUserIds,
            boolean joined) {

        public static Response of(HouseSchedule s, Long viewerId) {
            List<Long> participants = List.copyOf(s.getParticipantUserIds());
            return new Response(
                    s.getId(), s.getHouseId(), s.getTitle(), s.getScheduledAt(),
                    s.getMaxParticipants(), participants.size(), participants,
                    viewerId != null && participants.contains(viewerId));
        }
    }
}
