package com.portal.universe.authservice.controller;

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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public record UserSignupRequest(String email, String password, String name) {}

    @PostMapping("/signup")
    public ApiResponse<String> signup(@RequestBody UserSignupRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new CustomBusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User newUser = new User(request.email(), encodedPassword, request.name());
        User savedUser = userRepository.save(newUser);

        UserSignedUpEvent event = new UserSignedUpEvent(
                String.valueOf(savedUser.getId()),
                savedUser.getEmail(),
                savedUser.getName()
        );
        kafkaTemplate.send("user-signup", event);

        return ApiResponse.success("User registered successfully");
    }
}
