package gg.duo.repository;

import gg.duo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByNickname(String nickname);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);

    // [아이디 찾기] 이름과 전화번호로 조회
    Optional<User> findByNameAndPhone(String name, String phone);

    // [비밀번호 재설정] 이메일, 이름, 전화번호로 조회
    Optional<User> findByEmailAndNameAndPhone(String email, String name, String phone);
}