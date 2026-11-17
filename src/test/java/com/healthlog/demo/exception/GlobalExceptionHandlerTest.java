package com.healthlog.demo.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

    @Test
    void businessExceptionRendersExistingErrorTemplateWithOriginalStatus() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/profile/1/save");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Object view = handler.handleBusinessException(
                new BusinessException(HttpStatus.FORBIDDEN, "アクセス権限がありません"),
                model,
                request,
                response);

        assertEquals("error", view);
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        assertEquals(HttpStatus.FORBIDDEN.value(), model.getAttribute("errorStatus"));
        assertEquals("閲覧権限がありません", model.getAttribute("errorTitle"));
        assertEquals("このページを表示する権限がありません。", model.getAttribute("error"));
    }

    @Test
    void forbiddenGetRendersForbiddenPageInsteadOfRedirectingToPinProtectedProfile() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/profile/30/sleep");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Object view = handler.handleBusinessException(
                new BusinessException(HttpStatus.FORBIDDEN, "このカテゴリは共有されていません"),
                model,
                request,
                response);

        assertEquals("error", view);
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        assertNull(response.getRedirectedUrl());
        assertEquals("閲覧権限がありません", model.getAttribute("errorTitle"));
        assertEquals("このページを表示する権限がありません。", model.getAttribute("error"));
    }

    @Test
    void badRequestRendersExistingErrorTemplateWithBadRequestStatus() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/profile/1/home");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Object view = handler.handleBadRequest(
                new IllegalArgumentException("bad input"), model, request, response);

        assertEquals("error", view);
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertEquals(HttpStatus.BAD_REQUEST.value(), model.getAttribute("errorStatus"));
        assertEquals("リクエストが正しくありません", model.getAttribute("errorTitle"));
        assertEquals("入力内容を確認して、もう一度お試しください。", model.getAttribute("error"));
    }

    @Test
    void notFoundPageShowsStatusCodeWithoutExposingExceptionReason() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/missing");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Object view = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "private resource identifier"),
                request,
                response,
                model);

        assertEquals("error", view);
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
        assertEquals(HttpStatus.NOT_FOUND.value(), model.getAttribute("errorStatus"));
        assertEquals("ページが見つかりません", model.getAttribute("errorTitle"));
        assertEquals("", model.getAttribute("error"));
    }

    @Test
    void unexpectedErrorShowsGenericMessageAndInternalServerErrorCode() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/profile/1/home");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Object view = handler.handleException(
                new IllegalStateException("private database detail"),
                model,
                request,
                response);

        assertEquals("error", view);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), model.getAttribute("errorStatus"));
        assertEquals("システムエラーが発生しました", model.getAttribute("errorTitle"));
        assertEquals("時間をおいてから、もう一度お試しください。", model.getAttribute("error"));
    }
}
