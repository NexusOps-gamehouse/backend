package gg.duo.service;

import gg.duo.dto.AuthDtos.*;
import gg.duo.dto.UserDto;
import gg.duo.entity.User;
import gg.duo.repository.UserRepository;
import gg.duo.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final FileStorageService fileStorageService;

    @Transactional
    public AuthResponse signup(SignupForm form, MultipartFile image) {
        if (form.getEmail() == null || form.getEmail().isBlank())
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        if (form.getPassword() == null || form.getPassword().length() < 4)
            throw new IllegalArgumentException("비밀번호는 4자 이상이어야 합니다.");
        if (form.getName() == null || form.getName().isBlank())
            throw new IllegalArgumentException("이름을 입력해주세요.");
        if (form.getPhoneNumber() == null || form.getPhoneNumber().isBlank())
            throw new IllegalArgumentException("전화번호를 입력해주세요.");
        if (form.getNickname() == null || form.getNickname().isBlank())
            throw new IllegalArgumentException("닉네임을 입력해주세요.");
        if (userRepository.existsByEmail(form.getEmail()))
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        if (userRepository.existsByNickname(form.getNickname()))
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");

        User user = new User();
        user.setEmail(form.getEmail());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setName(form.getName());
        user.setPhoneNumber(form.getPhoneNumber());
        user.setNickname(form.getNickname());
        user.setProfileImageUrl(fileStorageService.store(image));
        user.setGender(form.getGender());
        user.setAgeRange(form.getAgeRange());
        user.setGame(form.getGame());
        user.setPlayStyle(form.getPlayStyle());
        user.setPosition(form.getPosition());
        user.setMic(form.isMic());
        user.setTier(form.getTier());
        user.setPlayTimes(form.getPlayTimes());
        user.setGameModes(form.getGameModes());
        user.setRiotNickname(form.getRiotNickname());
        user.setLastActiveAt(Instant.now());
        userRepository.save(user);

        return new AuthResponse(jwtTokenProvider.createToken(user.getId()), UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public boolean emailAvailable(String email) {
        return email != null && !email.isBlank() && !userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean nicknameAvailable(String nickname) {
        return nickname != null && !nickname.isBlank() && !userRepository.existsByNickname(nickname);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(request.password(), user.getPassword()))
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        user.setLastActiveAt(Instant.now());
        return new AuthResponse(jwtTokenProvider.createToken(user.getId()), UserDto.from(user));
    }

    // ================= 아이디 찾기 (이메일 마스킹) =================
    @Transactional(readOnly = true)
    public FindIdResponse findId(FindIdRequest request) {
        if (request.name() == null || request.name().isBlank())
            throw new IllegalArgumentException("이름을 입력해주세요.");
        if (request.phoneNumber() == null || request.phoneNumber().isBlank())
            throw new IllegalArgumentException("전화번호를 입력해주세요.");

        User user = userRepository.findByNameAndPhoneNumber(request.name(), request.phoneNumber())
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보를 찾을 수 없습니다."));

        // 마스킹 처리하여 반환 (예: ab***@naver.com)
        String maskedEmail = maskEmail(user.getEmail());
        return new FindIdResponse(maskedEmail);
    }

    // ================= 비밀번호 찾기 (임시 비밀번호 발급) =================
    @Transactional
    public FindPasswordResponse findPassword(FindPasswordRequest request) {
        if (request.name() == null || request.name().isBlank())
            throw new IllegalArgumentException("이름을 입력해주세요.");
        if (request.phoneNumber() == null || request.phoneNumber().isBlank())
            throw new IllegalArgumentException("전화번호를 입력해주세요.");
        if (request.email() == null || request.email().isBlank())
            throw new IllegalArgumentException("이메일을 입력해주세요.");

        User user = userRepository.findByNameAndPhoneNumberAndEmail(
                        request.name(), request.phoneNumber(), request.email())
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보를 찾을 수 없습니다."));

        // 임시 비밀번호 8자리 생성 (예: a1b2c3d4)
        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        user.setPassword(passwordEncoder.encode(tempPassword)); // DB 비밀번호 변경

        return new FindPasswordResponse(tempPassword);
    }

    // [이메일 마스킹 유틸 메서드] (예: abcdef@naver.com -> ab****@naver.com)
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;

        String[] parts = email.split("@");
        String id = parts[0];
        String domain = parts[1];

        if (id.length() <= 2) {
            return id.charAt(0) + "*@" + domain;
        } else {
            return id.substring(0, 2) + "*".repeat(id.length() - 2) + "@" + domain;
        }
    }
}