package gg.duo.controller;

import gg.duo.dto.AuthDtos.*;
import gg.duo.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AuthResponse signup(@ModelAttribute SignupForm form,
                               @RequestParam(value = "image", required = false) MultipartFile image) {
        return authService.signup(form, image);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** 이메일 중복 확인 */
    @GetMapping("/check-email")
    public Map<String, Boolean> checkEmail(@RequestParam String email) {
        return Map.of("available", authService.emailAvailable(email));
    }

    /** 닉네임 중복 확인 */
    @GetMapping("/check-nickname")
    public Map<String, Boolean> checkNickname(@RequestParam String nickname) {
        return Map.of("available", authService.nicknameAvailable(nickname));
    }

    /** 아이디(이메일) 찾기 (본명 + 전화번호) */
    @PostMapping("/find-id")
    public FindIdResponse findId(@RequestBody FindIdRequest request) {
        return authService.findId(request);
    }

    /** 비밀번호 찾기 (본명 + 전화번호 + 이메일 -> 임시 비밀번호 반환) */
    @PostMapping("/find-password")
    public FindPasswordResponse findPassword(@RequestBody FindPasswordRequest request) {
        return authService.findPassword(request);
    }
}