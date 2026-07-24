package gg.duo.repository;

import gg.duo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일 관련
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);

    // 닉네임 관련
    boolean existsByNickname(String nickname);
    Optional<User> findByNickname(String nickname); // 닉네임으로 조회 추가

    // 라이엇 정보 기반 조회
    Optional<User> findByPuuid(String puuid);
    Optional<User> findByGameNameAndTagLine(String gameName, String tagLine);
}