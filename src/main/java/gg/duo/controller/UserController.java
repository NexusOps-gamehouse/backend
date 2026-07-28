package gg.duo.controller;

import gg.duo.dto.AuthDtos.ProfileUpdateRequest;
import gg.duo.dto.UserDto;
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
        return userService.me((Long) auth.getPrincipal());
    }

    @PutMapping("/me")
    public UserDto update(Authentication auth, @RequestBody ProfileUpdateRequest req) {
        return userService.updateProfile((Long) auth.getPrincipal(), req);
    }

    /** 타 유저 프로필 조회 */
    @GetMapping("/{id}")
    public UserDto get(@PathVariable Long id) {
        return userService.get(id);
    }

    /** [이메일 찾기 1] 이름 + 전화번호 기준 */
    @GetMapping(value = "/find-email", params = {"name", "phone"})
    public Map<String, String> findEmailByNameAndPhone(@RequestParam String name, @RequestParam String phone) {
        String maskedEmail = userService.findEmailByNameAndPhone(name, phone);
        return Map.of("email", maskedEmail);
    }

    /** [이메일 찾기 2] 닉네임 기준 (팀원 기존 코드) */
    @GetMapping(value = "/find-email", params = "nickname")
    public ResponseEntity<Map<String, String>> findEmailByNickname(@RequestParam String nickname) {
        String maskedEmail = userService.findEmailByNickname(nickname);
        return ResponseEntity.ok(Map.of("email", maskedEmail));
    }

    /** 비밀번호 재설정 (이름+전화번호 기준) */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequest req) {
        userService.resetPassword(req.email(), req.name(), req.phone(), req.newPassword());
        return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다."));
    }

    public record ResetPasswordRequest(
            String email,
            String name,
            String phone,
            String newPassword
    ) {}

    /** 라이엇 프로필 동기화 */
    @PostMapping("/riot/sync")
    public ResponseEntity<Object> syncRiotProfile(
            Authentication authentication,
            @RequestBody RiotSyncRequestDTO request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        Object response = userService.syncRiotProfile(
                userId,
                request.gameName(),
                request.tagLine()
        );
        return ResponseEntity.ok(response);
    }

    public record RiotSyncRequestDTO(
            String gameName,
            String tagLine
    ) {}
}