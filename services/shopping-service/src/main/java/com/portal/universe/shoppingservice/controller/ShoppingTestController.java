package com.portal.universe.shoppingservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shopping")
public class ShoppingTestController {
    @GetMapping
    public String test() {
        return "Shopping Service Test!";
    }
}