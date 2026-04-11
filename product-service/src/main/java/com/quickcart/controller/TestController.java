package com.quickcart.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/products/test")
    public String test() {
        return "Product Service is running!";
    }
}
