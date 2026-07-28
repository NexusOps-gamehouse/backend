package gg.duo.dto;

import lombok.Data;

public class AuthDtos {

    @Data
    public static class SignupForm {
        private String email;
        private String password;
        private String name;
        private String phoneNumber;
        private String nickname;
        private String gender;
        private String ageRange;
        private String game;
        private String playStyle;
        private String position;
        private boolean mic;
        private String tier;
        private String playTimes;    // 콤마 구분
        private String gameModes;    // 콤마 구분
        private String riotNickname;
    }

    public record LoginRequest(String email, String password) {}

    public record AuthResponse(String token, UserDto user) {}

    public record ProfileUpdateRequest(
            String nickname, String gender, String ageRange, String game,
            String playStyle, String position, boolean mic, String tier,
            String playTimes, String gameModes, String riotNickname) {}

    // 아이디 찾기 요청/응답 DTO
    public record FindIdRequest(String name, String phoneNumber) {}
    public record FindIdResponse(String email) {} // 마스킹된 이메일 반환

    // 비밀번호 재설정 요청/응답 DTO
    public record FindPasswordRequest(String name, String phoneNumber, String email) {}
    public record FindPasswordResponse(String tempPassword) {}
}