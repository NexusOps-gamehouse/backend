package gg.duo.service;

import gg.duo.dto.AuthDtos.ProfileUpdateRequest;
import gg.duo.dto.UserDto;
import gg.duo.entity.User;
import gg.duo.repository.UserRepository;
import gg.duo.riot.dto.response.RiotProfileResponseDTO;
import gg.duo.riot.service.RiotService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RiotService riotService;

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

        // 저장 쪽(AuthService.normalizePhone)과 같은 규칙으로 맞춘다.
        // "-" 만 지우면 "010 1234 5678" 처럼 공백이 섞인 입력은 여전히 못 찾는다.
        String cleanName = name.trim();
        String cleanPhone = phone.replaceAll("[^0-9]", "");

        // 정규화 이전에 저장된 계정(하이픈 포함)도 있을 수 있어 원본 → 정규화 순으로 두 번 찾는다.
        User user = userRepository.findByNameAndPhone(cleanName, phone)
                .orElseGet(() -> userRepository.findByNameAndPhone(cleanName, cleanPhone)
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

        // findEmailByNameAndPhone 과 동일한 정규화 규칙
        String cleanName = name.trim();
        String cleanPhone = phone.replaceAll("[^0-9]", "");

        User user = userRepository.findByEmailAndNameAndPhone(email, cleanName, phone)
                .orElseGet(() -> userRepository.findByEmailAndNameAndPhone(email, cleanName, cleanPhone)
                        .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보를 찾을 수 없습니다.")));

        if (passwordEncoder.matches(newPassword, user.getPassword()))
            throw new IllegalArgumentException("기존 비밀번호와 다른 비밀번호를 입력해주세요.");

        user.setPassword(passwordEncoder.encode(newPassword));
    }

    /**
     * [라이엇 프로필 동기화]
     *
     * 이전에는 입력값(gameName / tagLine)만 저장하고 라이엇 API 를 호출하지 않아
     * puuid 가 null 로 남고 티어 · LP · 챔피언 숙련도가 화면에 나오지 않았다.
     * 프론트(RiotLinkCard)는 RiotProfileResponseDTO 형태를 기대하는데
     * UserDto 를 돌려주고 있어서, leaguePoints 는 항상 0,
     * championMasteries 는 항상 없음으로 처리되고 있었다.
     *
     * 여기서 실제로 라이엇을 조회하고, 계정 식별에 필요한 값만 DB 에 저장한다.
     *   - puuid : 이후 조회의 기준 키
     *   - gameName / tagLine : 라이엇이 돌려준 정규화된 값(대소문자 등)
     * 티어(user.tier)는 사용자가 프로필에서 직접 고르는 한글 값("다이아몬드")이므로
     * 라이엇의 영문 값("DIAMOND")으로 덮어쓰지 않는다.
     */
    @Transactional
    public RiotProfileResponseDTO syncRiotProfile(Long userId, String gameName, String tagLine) {
        User user = userRepository.findById(userId).orElseThrow();

        RiotProfileResponseDTO profile = riotService.fetchProfile(gameName, tagLine);

        user.setPuuid(profile.getPuuid());
        user.setGameName(profile.getGameName());
        user.setTagLine(profile.getTagLine());

        return profile;
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