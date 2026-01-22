package com.portal.universe.authservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class LoginController {

    @GetMapping({"/login"})
    public String login(HttpServletRequest request, Model model) {

        // 🔑 핵심: action URL을 모델에 추가
        model.addAttribute("actionUrl", "/auth-service/login");

        // 에러 처리
        String error = request.getParameter("error");
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }

        // 로그아웃 메시지
        String logout = request.getParameter("logout");
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }

        // 디버깅 로그
        String requestUri = request.getRequestURI();
        log.debug("Login page requested: {}", requestUri);

        return "login";  // templates/login.html
    }
}