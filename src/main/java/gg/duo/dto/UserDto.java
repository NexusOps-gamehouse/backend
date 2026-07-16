package gg.duo.dto;

import gg.duo.entity.User;

import java.time.Duration;
import java.time.Instant;

public record UserDto(
        Long id,
        String email,
        String nickname,
        String profileImageUrl,
        String gender,
        String ageRange,
        String game,
        String playStyle,
        String position,
        boolean mic,
        String tier,
        String playTimes,
        String gameModes,
        String riotNickname,
        boolean online
) {
    public static UserDto from(User u) {
        boolean online = u.getLastActiveAt() != null
                && u.getLastActiveAt().isAfter(Instant.now().minus(Duration.ofMinutes(5)));
        return new UserDto(u.getId(), u.getEmail(), u.getNickname(), u.getProfileImageUrl(),
                u.getGender(), u.getAgeRange(), u.getGame(), u.getPlayStyle(), u.getPosition(),
                u.isMic(), u.getTier(), u.getPlayTimes(), u.getGameModes(), u.getRiotNickname(),
                online);
    }
}
