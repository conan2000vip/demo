package com.healthlog.demo.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.healthlog.demo.service.helper.TabProfileContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TabProfileContextInterceptor implements HandlerInterceptor {

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            ModelAndView modelAndView) {
        if (modelAndView == null || modelAndView.getViewName() == null
                || !modelAndView.getViewName().startsWith("redirect:")) {
            return;
        }
        String viewName = modelAndView.getViewName();
        String location = viewName.substring("redirect:".length());
        if (location.startsWith("/") && !location.startsWith("//")) {
            modelAndView.setViewName("redirect:" + TabProfileContext.appendTabId(request, location));
        }
    }
}