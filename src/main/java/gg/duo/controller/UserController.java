package gg.duo.controller;

import gg.duo.dto.AuthDtos.ProfileUpdateRequest;
import gg.duo.dto.UserDto;
import gg.duo.riot.dto.RiotSyncRequestDTO;
import gg.duo.riot.dto.response.RiotProfileResponseDTO;
import gg.duo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserDto me(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return userService.me(userId);
    }

    @PutMapping("/me")
    public UserDto update(Authentication auth, @RequestBody ProfileUpdateRequest req) {
        Long userId = (Long) auth.getPrincipal();
        return userService.updateProfile(userId, req);
    }

    /** 타 유저 프로필 조회 */
    @GetMapping("/{id}")
    public UserDto get(@PathVariable Long id) {
        return userService.get(id);
    }

    // 1. 닉네임으로 아이디(이메일) 찾기 API
    @GetMapping("/find-email")
    public ResponseEntity<Map<String, String>> findEmail(@RequestParam String nickname) {
        String maskedEmail = userService.findEmailByNickname(nickname);
        return ResponseEntity.ok(Map.of("email", maskedEmail));
    }

    // 2. 비밀번호 재설정 API
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequest req) {
        userService.resetPassword(req.email(), req.nickname(), req.newPassword());
        return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다."));
    }

    // ===== 비밀번호 재설정 요청 DTO =====
    public record ResetPasswordRequest(
            String email,
            String nickname,
            String newPassword
    ) {}

    @PostMapping("/riot/sync")
    public ResponseEntity<RiotProfileResponseDTO> syncRiotProfile(
            Authentication authentication,
            @RequestBody RiotSyncRequestDTO request
    ) {

        Long userId = (Long) authentication.getPrincipal();

        RiotProfileResponseDTO response =
                userService.syncRiotProfile(
                        userId,
                        request.gameName(),
                        request.tagLine()
                );

        return ResponseEntity.ok(response);
    }
}
