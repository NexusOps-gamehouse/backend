package gg.duo.controller;

import gg.duo.dto.AuthDtos.ProfileUpdateRequest;
import gg.duo.dto.UserDto;
import gg.duo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
}
