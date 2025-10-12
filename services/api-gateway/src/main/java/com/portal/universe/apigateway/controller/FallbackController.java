package com.portal.universe.apigateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

    @GetMapping("/fallback/blog")
    public Mono<String> blogServiceFallback() {
        return Mono.just("Blog Service is currently unavailable. Please try again later.");
    }

}
