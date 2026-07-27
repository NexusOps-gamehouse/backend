package gg.duo.service;

import gg.duo.dto.AuthDtos.ProfileUpdateRequest;
import gg.duo.dto.UserDto;
import gg.duo.riot.dto.ChampionMasteryDTO;
import gg.duo.riot.dto.response.RiotProfileResponseDTO;
import gg.duo.entity.User;
import gg.duo.entity.UserChampionMastery;
import gg.duo.repository.UserChampionMasteryRepository;
import gg.duo.repository.UserRepository;
import gg.duo.riot.service.RiotService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserChampionMasteryRepository masteryRepository;
    private final PasswordEncoder passwordEncoder;
    private final RiotService riotService;

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        return UserDto.from(userRepository.findById(userId).orElseThrow());
    }

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
        user.setPlayTimes(req.playTimes());
        user.setGameModes(req.gameModes());
        return UserDto.from(user);
    }

    @Transactional
    public RiotProfileResponseDTO syncRiotProfile(Long userId,
                                                  String gameName,
                                                  String tagLine) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("해당 유저를 찾을 수 없습니다. id=" + userId));


        // Riot API 조회
        RiotProfileResponseDTO profile =
                riotService.fetchProfile(gameName, tagLine);


        User exist = userRepository.findByPuuid(profile.getPuuid())
                .orElse(null);

        if (exist != null && !exist.getId().equals(user.getId())) {
            throw new IllegalArgumentException("이미 다른 계정과 연동된 Riot 계정입니다.");
        }

        // User 엔티티 업데이트
        user.updateRiotInfo(
                profile.getPuuid(),
                profile.getGameName(),
                profile.getTagLine(),
                profile.getProfileIconId(),
                profile.getSummonerLevel(),
                profile.getTier(),
                profile.getRank(),
                profile.getLeaguePoints()
        );

        // Champion Mastery 엔티티 변환
        List<UserChampionMastery> masteries =
                profile.getChampionMasteries()
                        .stream()
                        .map(dto ->
                                UserChampionMastery.builder()
                                        .ranking(dto.getRanking())
                                        .championId(dto.getChampionId())
                                        .masteryLevel(dto.getChampionMasteryLevel())
                                        .masteryPoints(dto.getChampionMasteryPoints())
                                        .game("LOL")
                                        .build()
                        )
                        .toList();

        updateUserMasteries(user, masteries);

        return profile;
    }

    /**
     * 챔피언 숙련도 정보(상위 3개) 동기화/저장
     */
    @Transactional
    public void updateUserMasteries(User user, List<UserChampionMastery> newMasteries) {


        // 기존 숙련도 삭제
        masteryRepository.deleteByUser(user);

        Instant now = Instant.now();

        // 연관관계 및 동기화 시간 설정
        newMasteries.forEach(mastery -> {
            mastery.setUser(user);
            mastery.setSyncedAt(now);
        });

        // 한 번에 저장
        masteryRepository.saveAll(newMasteries);
    }
    // ==================== [신규 추가] 아이디/비밀번호 찾기 ====================

    /**
     * 1. 닉네임으로 이메일(아이디) 찾기
     */
    @Transactional(readOnly = true)
    public String findEmailByNickname(String nickname) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("해당 닉네임으로 가입된 계정을 찾을 수 없습니다."));

        // 보안을 위해 이메일 마스킹 처리 (예: lor***@gmail.com)
        return maskEmail(user.getEmail());
    }

    /** 2. 비밀번호 재설정 (이메일 & 닉네임 검증 후 새 비밀번호 저장) */
    @Transactional
    public void resetPassword(String email, String nickname, String newPassword) {
        if (newPassword == null || newPassword.length() < 4) {
            throw new IllegalArgumentException("비밀번호는 4자 이상이어야 합니다.");
        }

        // 이메일로 유저를 찾은 뒤 닉네임이 일치하는지 확인
        User user = userRepository.findByEmail(email)
                .filter(u -> u.getNickname().equals(nickname))
                .orElseThrow(() -> new IllegalArgumentException("입력하신 정보와 일치하는 계정이 없습니다."));

        user.setPassword(passwordEncoder.encode(newPassword));
    }

    /**
     * 이메일 마스킹 도우미 메서드 (ex: lora1234@naver.com -> lor***@naver.com)
     */
    private String maskEmail(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex <= 3) return email;
        return email.substring(0, 3) + "***" + email.substring(atIndex);
    }
}
