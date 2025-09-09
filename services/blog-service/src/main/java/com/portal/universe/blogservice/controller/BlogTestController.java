package com.portal.universe.blogservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blogs")
public class BlogTestController {

    @GetMapping("/test")
    public String test() {
        return "Hello from Blog Service! Routing is successful!";
    }
}
