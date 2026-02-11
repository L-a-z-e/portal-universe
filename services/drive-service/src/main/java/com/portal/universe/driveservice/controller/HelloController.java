package com.portal.universe.driveservice.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public ApiResponse<Map<String, String>> hello() {
        return ApiResponse.success(Map.of(
                "service", "drive-service",
                "status", "running"
        ));
    }
}
