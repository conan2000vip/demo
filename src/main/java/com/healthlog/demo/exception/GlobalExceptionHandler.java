package com.healthlog.demo.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final String ERROR_ATTRIBUTE = "error";
    private static final String ERROR_VIEW = "error";
    private static final String MESSAGE_KEY = "message";
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // BusinessExceptionの例外を処理するハンドラ
    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException ex, Model model, HttpServletRequest request,
            HttpServletResponse response) {
        if (isJsonRequest(request)) {
            return ResponseEntity.status(ex.getStatus()).body(Map.of(MESSAGE_KEY, ex.getMessage()));
        }
        response.setStatus(ex.getStatus().value());
        addErrorPageAttributes(model, ex.getStatus());
        return ERROR_VIEW;
    }

    // 404 Not Foundの例外を処理するハンドラ
    @ExceptionHandler(ResponseStatusException.class)
    public Object handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request,
            HttpServletResponse response, Model model) {
        if (isJsonRequest(request)) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of(MESSAGE_KEY, ex.getReason() != null ? ex.getReason() : "リクエストに失敗しました。"));
        }
        response.setStatus(ex.getStatusCode().value());
        addErrorPageAttributes(model, ex.getStatusCode());
        return ERROR_ATTRIBUTE;
    }

    // 500 Internal Server Errorの例外を処理するハンドラ
    @ExceptionHandler(Exception.class)
    public Object handleException(Exception ex, Model model, HttpServletRequest request, HttpServletResponse response) {
        logger.error("Unexpected error while processing request {} {}", request.getMethod(), request.getRequestURI(), ex);
        if (isJsonRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(MESSAGE_KEY, "共有設定を保存できませんでした。時間をおいて、もう一度お試しください。"));
        }
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        addErrorPageAttributes(model, HttpStatus.INTERNAL_SERVER_ERROR);
        return ERROR_ATTRIBUTE;
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        return request.getRequestURI().matches(".*/profile/\\d+/share-settings/?")
                || request.getRequestURI().matches(".*/profile/\\d+/(weight|sleep|water|step)/chart-data/?")
                || request.getRequestURI().matches(".*/profile/\\d+/home/summary-chart/?")
                || request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json");
    }

    private Object respond(HttpStatus status, String message, HttpServletRequest request, HttpServletResponse response,
            Model model) {
        if (isJsonRequest(request)) {
            return ResponseEntity.status(status).body(Map.of(MESSAGE_KEY, message));
        }
        response.setStatus(status.value());
        addErrorPageAttributes(model, status);
        return ERROR_VIEW;
    }

    private void addErrorPageAttributes(Model model, HttpStatusCode status) {
        model.addAttribute("errorStatus", status.value());
        model.addAttribute("errorTitle", switch (status.value()) {
            case 400 -> "リクエストが正しくありません";
            case 401 -> "ログインが必要です";
            case 403 -> "閲覧権限がありません";
            case 404 -> "ページが見つかりません";
            case 409 -> "処理を完了できませんでした";
            case 500 -> "システムエラーが発生しました";
            default -> "リクエストを処理できませんでした";
        });
        model.addAttribute(ERROR_ATTRIBUTE, switch (status.value()) {
            case 400 -> "入力内容を確認して、もう一度お試しください。";
            case 401 -> "続けるにはログインしてください。";
            case 403 -> "このページを表示する権限がありません。";
            case 404 -> "";
            case 409 -> "画面を更新してから、もう一度お試しください。";
            case 500 -> "時間をおいてから、もう一度お試しください。";
            default -> "時間をおいてから、もう一度お試しください。";
        });
    }

    // 404 Not Foundの例外を処理するハンドラ
    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNotFound(NoResourceFoundException ex, Model model, HttpServletRequest request,
            HttpServletResponse response) {
        logger.warn("Not found: {}", request.getRequestURI());
        return respond(HttpStatus.NOT_FOUND, "ページが見つかりません。", request, response, model);
    }

    // 400 Bad Requestの例外を処理するハンドラ
    @ExceptionHandler({ MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class, MethodArgumentNotValidException.class })
    public Object handleBadRequest(Exception ex, Model model, HttpServletRequest request,
            HttpServletResponse response) {
        logger.warn("Bad request {}: {}", request.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.BAD_REQUEST, "入力内容が正しくありません。", request, response, model);
    }
}