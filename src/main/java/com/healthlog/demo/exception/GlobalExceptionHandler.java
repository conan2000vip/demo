package com.healthlog.demo.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final String ERROR_ATTRIBUTE = "error";
    private static final String MESSAGE_KEY = "message";
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException ex, Model model,
            HttpServletRequest request, HttpServletResponse response) {
        if (isJsonRequest(request)) {
            return ResponseEntity.status(ex.getStatus()).body(Map.of(MESSAGE_KEY, ex.getMessage()));
        }
        response.setStatus(ex.getStatus().value());
        model.addAttribute(ERROR_ATTRIBUTE, ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Object handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request,
            HttpServletResponse response, Model model) {
        if (isJsonRequest(request)) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of(MESSAGE_KEY, ex.getReason() != null ? ex.getReason() : "リクエストに失敗しました。"));
        }
        response.setStatus(ex.getStatusCode().value());
        model.addAttribute(ERROR_ATTRIBUTE, ex.getReason());
        return ERROR_ATTRIBUTE;
    }

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception ex, Model model, HttpServletRequest request,
            HttpServletResponse response) {
        if (isJsonRequest(request)) {
            logger.error("Unexpected error while processing JSON API request", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(MESSAGE_KEY, "共有設定を保存できませんでした。時間をおいて、もう一度お試しください。"));
        }
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        model.addAttribute(ERROR_ATTRIBUTE, "システムエラーが発生しました。");
        return ERROR_ATTRIBUTE;
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        return request.getRequestURI().matches(".*/profile/\\d+/share-settings/?")
                || request.getRequestURI().matches(".*/profile/\\d+/(weight|sleep|water|step)/chart-data/?")
                || request.getRequestURI().matches(".*/profile/\\d+/home/summary-chart/?")
                || request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json");
    }
}