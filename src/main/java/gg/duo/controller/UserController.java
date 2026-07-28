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

    /** [아이디 찾기] 본명과 전화번호로 이메일 조회 */
    @GetMapping("/find-email")
    public Map<String, String> findEmail(@RequestParam String name, @RequestParam String phone) {
        String maskedEmail = userService.findEmailByNameAndPhone(name, phone);
        return Map.of("email", maskedEmail);
    }

    /** [비밀번호 재설정] */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequest req) {
        userService.resetPassword(req.email(), req.name(), req.phone(), req.newPassword());
        return ResponseEntity.ok(Map.of("message", "비밀번호가 성공적으로 변경되었습니다."));
    }

    public record ResetPasswordRequest(
            String email,
            String name,
            String phone,
            String newPassword
    ) {}
}