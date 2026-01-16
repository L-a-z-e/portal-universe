package com.portal.universe.authservice.controller;

import com.portal.universe.authservice.service.UserService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 관련 API를 처리하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    public record UserSignupRequest(
            String email,
            String password,
            String nickname,
            String realName,
            boolean marketingAgree
    ) {}

    @PostMapping("/signup")
    public ApiResponse<String> signup(@RequestBody UserSignupRequest request) {
        UserService.SignupCommand command = new UserService.SignupCommand(
                request.email(),
                request.password(),
                request.nickname(),
                request.realName(),
                request.marketingAgree()
        );

        userService.registerUser(command);

        return ApiResponse.success("User registered successfully");
    }
}