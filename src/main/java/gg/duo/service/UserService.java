package gg.duo.service;

import gg.duo.dto.AuthDtos.ProfileUpdateRequest;
import gg.duo.dto.UserDto;
import gg.duo.entity.User;
import gg.duo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        return UserDto.from(userRepository.findById(userId).orElseThrow());
    }

    /** 타 유저 프로필 조회 (신청자/작성자 프로필 보기) */
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

    /**
     * [수정] 이름과 phoneNumber로 이메일(아이디) 찾기 및 마스킹 처리
     */
    @Transactional(readOnly = true)
    public String findEmailByNameAndPhone(String name, String phoneNumber) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름을 입력해주세요.");
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("전화번호를 입력해주세요.");
        }

        // DB 저장이 하이픈 없이 되어있을 수 있으므로 하이픈 제거 버전과 원본 모두 대응
        String cleanPhone = phoneNumber.replaceAll("-", "");

        User user = userRepository.findByNameAndPhoneNumber(name, phoneNumber)
                .orElseGet(() -> userRepository.findByNameAndPhoneNumber(name, cleanPhone)
                        .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보를 찾을 수 없습니다.")));

        return maskEmail(user.getEmail());
    }

    // 이메일 마스킹 메서드 (예: ab***@naver.com)
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