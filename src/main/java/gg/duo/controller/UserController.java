package gg.duo.controller;

import gg.duo.dto.AuthDtos.ProfileUpdateRequest;
import gg.duo.dto.UserDto;
import gg.duo.service.UserService;
import lombok.RequiredArgsConstructor;
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

    /**
     * [추가] 이름과 전화번호로 이메일(아이디) 찾기
     * 요청 예시: GET /api/users/find-email?name=홍길동&phone=010-5531-3930
     */
    @GetMapping("/find-email")
    public Map<String, String> findEmail(@RequestParam String name, @RequestParam String phone) {
        String maskedEmail = userService.findEmailByNameAndPhone(name, phone);
        return Map.of("email", maskedEmail);
    }
}