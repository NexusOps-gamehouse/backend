package gg.duo.dto;

import gg.duo.entity.User;

public record UserDto(
        Long id,
        String email,
        String name,
        String phone,
        String nickname,
        String profileImageUrl,
        String gender,
        String ageRange,
        String game,
        String playStyle,
        String position,
        Boolean mic,
        String tier,
        String playTimes,
        String gameModes,
        String puuid,
        String gameName,
        String tagLine
) {
    public static UserDto from(User u) {
        if (u == null) return null;
        return new UserDto(
                u.getId(),
                u.getEmail(),
                u.getName(),
                u.getPhone(),
                u.getNickname(),
                u.getProfileImageUrl(),
                u.getGender(),
                u.getAgeRange(),
                u.getGame(),
                u.getPlayStyle(),
                u.getPosition(),
                u.getMic(), // isMic() -> getMic()로 변경
                u.getTier(),
                u.getPlayTimes(),
                u.getGameModes(),
                u.getPuuid(),
                u.getGameName(),
                u.getTagLine()
        );
    }
}
