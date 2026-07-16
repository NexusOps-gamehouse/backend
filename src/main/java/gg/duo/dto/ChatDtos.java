package gg.duo.dto;

import java.time.Instant;
import java.util.List;

public class ChatDtos {

    /** applicationId: 방장이 아닌 멤버의 신청 id (확정 버튼용), 방장은 null */
    public record MemberDto(UserDto user, boolean confirmed, boolean owner, Long applicationId) {}

    public record RoomDto(Long id, Long postId, String postTitle, Long postAuthorId,
                          String postStatus, List<MemberDto> members) {}

    public record RoomDetail(RoomDto room, List<MessageDto> messages) {}

    public record MessageDto(Long id, Long roomId, Long senderId, String senderNickname,
                             String content, Instant createdAt) {}

    public record SendRequest(String content) {}
}
