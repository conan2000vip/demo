package com.healthlog.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final ProfilePinAuthenticationInterceptor profilePinAuthenticationInterceptor;
    private final TabProfileContextInterceptor tabProfileContextInterceptor;

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/auth/login");
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(tabProfileContextInterceptor);
        registry.addInterceptor(profilePinAuthenticationInterceptor).addPathPatterns("/profile/**");
    }
}
