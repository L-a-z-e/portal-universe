package com.portal.universe.authservice.controller;

import com.portal.universe.authservice.domain.Role;
import com.portal.universe.authservice.domain.User;
import com.portal.universe.authservice.exception.AuthErrorCode;
import com.portal.universe.authservice.repository.UserRepository;
import com.portal.universe.common.event.UserSignedUpEvent;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.commonlibrary.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 관련 API를 처리하는 컨트롤러입니다.
 * 현재 회원가입 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 회원가입 요청 시 사용되는 DTO(Data Transfer Object)입니다.
     * @param email 사용자의 이메일
     * @param password 사용자의 비밀번호
     * @param name 사용자의 이름
     */
    public record UserSignupRequest(String email, String password, String name) {}

    /**
     * 신규 사용자 등록(회원가입)을 처리하는 API 엔드포인트입니다.
     * @param request 회원가입 정보를 담은 요청 DTO
     * @return 처리 성공 메시지를 담은 ApiResponse
     * @throws CustomBusinessException 이메일이 이미 존재할 경우 발생
     */
    @PostMapping("/signup")
    public ApiResponse<String> signup(@RequestBody UserSignupRequest request) {
        // 1. 이메일 중복 확인
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new CustomBusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // 3. 사용자 정보 저장
        User newUser = new User(request.email(), encodedPassword, request.name(), Role.USER);
        User savedUser = userRepository.save(newUser);

        // 4. 회원가입 이벤트 발행 (Kafka)
        // 다른 마이크로서비스(예: 쇼핑 서비스)에서 사용자 정보를 동기화할 수 있도록 이벤트를 발행합니다.
        UserSignedUpEvent event = new UserSignedUpEvent(
                String.valueOf(savedUser.getId()),
                savedUser.getEmail(),
                savedUser.getName()
        );
        kafkaTemplate.send("user-signup", event);

        return ApiResponse.success("User registered successfully");
    }
}