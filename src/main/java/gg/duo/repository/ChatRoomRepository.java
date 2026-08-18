package gg.duo.repository;

import gg.duo.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByPostId(Long postId);

    /** 목록 변환용 — 글마다 findByPostId 를 부르는 대신 postId 를 모아 한 번에 조회한다. */
    List<ChatRoom> findByPostIdIn(Collection<Long> postIds);
}
