package gg.duo.crew.dto;

import gg.duo.crew.domain.notice.HouseNotice;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class NoticeDto {

    private NoticeDto() {}

    public record WriteRequest(
            @NotBlank(message = "공지 제목은 필수입니다.") String title,
            String content,
            Boolean pinned) {}

    public record UpdateRequest(
            @NotBlank(message = "공지 제목은 필수입니다.") String title,
            String content) {}

    public record Response(Long id, Long houseId, Long authorId, String title,
                           String content, boolean pinned, Instant createdAt) {

        public static Response of(HouseNotice n) {
            return new Response(n.getId(), n.getHouseId(), n.getAuthorId(), n.getTitle(),
                    n.getContent(), Boolean.TRUE.equals(n.getIsPinned()), n.getCreatedAt());
        }
    }
}
