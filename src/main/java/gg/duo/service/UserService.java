package gg.duo.service;

import gg.duo.dto.AuthDtos.ProfileUpdateRequest;
import gg.duo.dto.UserDto;
import gg.duo.entity.User;
import gg.duo.entity.UserChampionMastery;
import gg.duo.repository.UserChampionMasteryRepository;
import gg.duo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserChampionMasteryRepository masteryRepository;

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
        user.setTier(req.tier());
        user.setPlayTimes(req.playTimes());
        user.setGameModes(req.gameModes());
        return UserDto.from(user);
    }

    /**
     * 챔피언 숙련도 정보(상위 3개) 동기화/저장
     */
    @Transactional
    public void updateUserMasteries(Long userId, List<UserChampionMastery> newMasteries) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다. id=" + userId));

        // 기존 숙련도 정보 삭제 후 새로 저장
        masteryRepository.deleteByUser(user);

        for (UserChampionMastery mastery : newMasteries) {
            mastery.setUser(user);
            mastery.setSyncedAt(Instant.now());
            masteryRepository.save(mastery);
        }
    }
}
