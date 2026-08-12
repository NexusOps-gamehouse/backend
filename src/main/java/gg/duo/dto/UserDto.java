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
        /**
         * 사용자가 설문에서 직접 고른 티어. 한글 값이다. ("다이아몬드")
         * 자기 신고값이라 라이엇 연동 여부와 무관하게 그대로 보존한다.
         */
        String tier,
        /**
         * 라이엇에서 확인된 티어. 영문 enum 이다. ("DIAMOND")
         * 연동하지 않았으면 null.
         *
         * tier 와 따로 두는 이유: 둘은 성격이 다르다. tier 는 자기 신고,
         * riotTier 는 검증된 값이다. 하나로 합치면 어느 쪽인지 구분할 수 없고,
         * 연동을 해제했을 때 사용자가 고른 값을 복구할 방법도 없어진다.
         *
         * 화면에서는 riotTier 가 있으면 그쪽을 우선 보여준다.
         * (frontend api/riot.js 의 displayTier)
         */
        String riotTier,
        String riotRank,
        String playTimes,
        String gameModes,
        String riotNickname,
        String puuid,
        String gameName,
        String tagLine,
        boolean online
) {
    public static UserDto from(User u) {
        boolean online = u.getLastActiveAt() != null
                && u.getLastActiveAt().isAfter(Instant.now().minus(Duration.ofMinutes(5)));
        return new UserDto(
                u.getId(),
                u.getEmail(),
                u.getNickname(),
                u.getProfileImageUrl(),
                u.getGender(),
                u.getAgeRange(),
                u.getGame(),
                u.getPlayStyle(),
                u.getPosition(),
                u.isMic(),
                u.getTier(),
                u.getRiotTier(),
                u.getRiotRank(),
                u.getPlayTimes(),
                u.getGameModes(),
                u.getRiotNickname(),
                u.getPuuid(),
                u.getGameName(),
                u.getTagLine(),
                online
        );
    }
}