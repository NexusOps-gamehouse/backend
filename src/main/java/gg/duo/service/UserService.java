package gg.duo.service;

import gg.duo.dto.AuthDtos.ProfileUpdateRequest;
import gg.duo.dto.UserDto;
import gg.duo.entity.User;
import gg.duo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        return UserDto.from(userRepository.findById(userId).orElseThrow());
    }

    /** 타 유저 프로필 조회 */
    @Transactional(readOnly = true)
    public UserDto get(Long userId) {
        return UserDto.from(userRepository.findById(userId).orElseThrow());
    }

    @Transactional
    public UserDto updateProfile(Long userId, ProfileUpdateRequest req) {
        User user = userRepository.findById(userId).orElseThrow();
        if (req.nickname() != null && !req.nickname().isBlank()
                && !req.nickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(req.nickname()))
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            user.setNickname(req.nickname());
        }
        user.setGender(req.gender());
        user.setAgeRange(req.ageRange());
        user.setGame(req.game());
        user.setPlayStyle(req.playStyle());
        user.setPosition(req.position());
        user.setMic(req.mic());
        user.setTier(req.tier());
        user.setPlayTimes(req.playTimes());
        user.setGameModes(req.gameModes());
        user.setRiotNickname(req.riotNickname());
        return UserDto.from(user);
    }

    /** [이메일 찾기] 이름 + 전화번호 기준 */
    @Transactional(readOnly = true)
    public String findEmailByNameAndPhone(String name, String phone) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("이름을 입력해주세요.");
        if (phone == null || phone.isBlank()) throw new IllegalArgumentException("전화번호를 입력해주세요.");

        String cleanPhone = phone.replaceAll("-", "");

        User user = userRepository.findByNameAndPhone(name, phone)
                .orElseGet(() -> userRepository.findByNameAndPhone(name, cleanPhone)
                        .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보를 찾을 수 없습니다.")));

        return maskEmail(user.getEmail());
    }

    /** [비밀번호 재설정] 이메일 + 이름 + 전화번호로 본인 확인 후 새 비밀번호로 변경 */
    @Transactional
    public void resetPassword(String email, String name, String phone,
                              String newPassword, String newPasswordConfirm) {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("이름을 입력해주세요.");
        if (phone == null || phone.isBlank())
            throw new IllegalArgumentException("전화번호를 입력해주세요.");
        if (newPassword == null || newPassword.length() < 4)
            throw new IllegalArgumentException("비밀번호는 4자 이상이어야 합니다.");
        if (!newPassword.equals(newPasswordConfirm))
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");

        String cleanPhone = phone.replaceAll("-", "");

        User user = userRepository.findByEmailAndNameAndPhone(email, name, phone)
                .orElseGet(() -> userRepository.findByEmailAndNameAndPhone(email, name, cleanPhone)
                        .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보를 찾을 수 없습니다.")));

        if (passwordEncoder.matches(newPassword, user.getPassword()))
            throw new IllegalArgumentException("기존 비밀번호와 다른 비밀번호를 입력해주세요.");

        user.setPassword(passwordEncoder.encode(newPassword));
    }

    /** [라이엇 프로필 동기화] */
    @Transactional
    public UserDto syncRiotProfile(Long userId, String gameName, String tagLine) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setGameName(gameName);
        user.setTagLine(tagLine);
        return UserDto.from(user);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String id = parts[0];
        String domain = parts[1];

        if (id.length() <= 2) {
            return id.charAt(0) + "*@" + domain;
        } else {
            return id.substring(0, 2) + "*".repeat(id.length() - 2) + "@" + domain;
        }
    }
}