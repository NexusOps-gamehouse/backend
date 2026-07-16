package gg.duo.controller;

import gg.duo.dto.AuthDtos.AuthResponse;
import gg.duo.dto.AuthDtos.LoginRequest;
import gg.duo.dto.AuthDtos.SignupForm;
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
}
