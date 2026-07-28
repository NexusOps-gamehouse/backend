package gg.duo.repository;

import gg.duo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);

    // [추가] 이름과 전화번호로 유저 조회
    Optional<User> findByNameAndPhoneNumber(String name, String phoneNumber);
}