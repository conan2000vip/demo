package com.healthlog.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RootController {

    @GetMapping("/")
    public String root() {
        return "redirect:/auth/login";
    }
}
