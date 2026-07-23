package gg.duo.controller;

import gg.duo.dto.AuthDtos.ProfileUpdateRequest;
import gg.duo.dto.UserDto;
import gg.duo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<String> findEmail(@RequestParam String nickname) {
        String maskedEmail = userService.findEmailByNickname(nickname);
        return ResponseEntity.ok(maskedEmail);
    }

    // 2. 비밀번호 재설정 (임시 비밀번호 발급) API
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest req) {
        String tempPassword = userService.resetPassword(req.email(), req.nickname());
        return ResponseEntity.ok(tempPassword);
    }

    // ===== 비밀번호 재설정 요청 DTO =====
    public record ResetPasswordRequest(
            String email,
            String nickname
    ) {}
}