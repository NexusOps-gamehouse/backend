package gg.duo.repository;

import gg.duo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByNickname(String nickname);

    // 라이엇 정보 기반 조회 메서드
    Optional<User> findByPuuid(String puuid);
    Optional<User> findByGameNameAndTagLine(String gameName, String tagLine);
}