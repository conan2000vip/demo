package com.healthlog.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.healthlog.demo.constant.SessionConstants;
import com.healthlog.demo.dto.auth.LoginRequest;
import com.healthlog.demo.dto.auth.PasswordResetConfirmRequest;
import com.healthlog.demo.dto.auth.PasswordResetRequest;
import com.healthlog.demo.dto.auth.RegisterRequest;
import com.healthlog.demo.dto.auth.VerifyCodeRequest;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.service.auth.LoginService;
import com.healthlog.demo.service.auth.PasswordResetService;
import com.healthlog.demo.service.auth.RegisterService;
import com.healthlog.demo.service.auth.VerifyCodeService;
import com.healthlog.demo.service.profile.ProfileService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginService loginService;
    private final RegisterService registerService;
    private final VerifyCodeService verifyCodeService;
    private final PasswordResetService passwordResetService;
    private final ProfileService profileService; // Inject ProfileService để kết nối luồng Profile

    // 1. ログイン画面を表示し、ユーザーがメールアドレスとパスワードを入力できるフォームを準備する。
    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }

    // 2. ログイン入力を検証し、成功時はプロファイル存在チェックを行って画面を遷移する。
    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute("loginRequest") LoginRequest loginRequest, BindingResult bindingResult,
            HttpServletRequest request, HttpServletResponse response, HttpSession session, Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/login";
        }

        try {
            // ログイン認証を実行してUserオブジェクトを取得
            User user = loginService.login(loginRequest);
            session.setAttribute(SessionConstants.LOGIN_USER, user);

            // プロファイルが存在しない場合は新規作成画面へ、存在する場合は選択画面へリダイレクト
            if (!profileService.hasAnyProfile(user.getId())) {
                return "redirect:/profile/new";
            }

            profileService.resolveCurrentProfile(session, user.getId());
            session.removeAttribute(SessionConstants.AUTHENTICATED_PROFILE_ID);
            return "redirect:/profile/select";

        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            if (e.getStatus() == HttpStatus.FORBIDDEN) {
                model.addAttribute("emailNotVerified", true);
                model.addAttribute("email", loginRequest.getEmail());
            }
            return "auth/login";
        }
    }

    // 3. 会員登録画面を表示し、新規登録情報を入力するためのフォームを準備する。
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    // 4. 登録入力を検証し、仮登録後にメール確認コードの入力画面へメールアドレスを引き継ぐ。
    @PostMapping("/register")
    public String handleRegister(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request, BindingResult bindingResult,
            HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            registerService.register(request);
            session.removeAttribute(SessionConstants.IS_RESET_FLOW);
            redirectAttributes.addFlashAttribute("email", request.getEmail());
            redirectAttributes.addFlashAttribute("message", "仮登録が完了しました。メールに送信された確認コードをご入力ください。");
            return "redirect:/auth/verify-code";

        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
    }

    // 5. パスワード再設定依頼画面を表示し、確認コード送信用のメールアドレス入力フォームを準備する。
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(
            @RequestParam(value = "returnTo", required = false) String returnTo,
            HttpSession session, Model model) {
        String safeReturnTo = normalizeReturnTo(returnTo);
        if (safeReturnTo != null) {
            session.setAttribute("PASSWORD_RESET_RETURN_TO", safeReturnTo);
            storePinProfileId(session, safeReturnTo);
        } else {
            session.removeAttribute("PASSWORD_RESET_RETURN_TO");
            session.removeAttribute("PASSWORD_RESET_PROFILE_ID");
        }
        model.addAttribute("passwordResetRequest", new PasswordResetRequest());
        model.addAttribute("returnTo", safeReturnTo != null
                ? safeReturnTo : session.getAttribute("PASSWORD_RESET_RETURN_TO"));
        return "auth/forgot-password";
    }

    // 6. メールアドレスを検証し、パスワード再設定用の確認コードを送信して確認画面へ遷移する。
    @PostMapping("/forgot-password")
    public String handleForgotPassword(
            @Valid @ModelAttribute("passwordResetRequest") PasswordResetRequest request,
            BindingResult bindingResult, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String returnTo = normalizeReturnTo(request.getReturnTo());
        if (returnTo != null) {
            session.setAttribute("PASSWORD_RESET_RETURN_TO", returnTo);
            storePinProfileId(session, returnTo);
        }
        model.addAttribute("returnTo", returnTo != null
                ? returnTo : session.getAttribute("PASSWORD_RESET_RETURN_TO"));
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }

        try {
            passwordResetService.sendPasswordResetEmail(request.getEmail());
            session.setAttribute(SessionConstants.IS_RESET_FLOW, true);
            redirectAttributes.addFlashAttribute("email", request.getEmail());
            redirectAttributes.addFlashAttribute("message", "パスワード再設定用の確認コードを送信しました。");
            return "redirect:/auth/verify-code";
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/forgot-password";
        }
    }

    private String normalizeReturnTo(String returnTo) {
        if (returnTo == null || returnTo.isBlank()
                || !returnTo.startsWith("/") || returnTo.startsWith("//")
                || returnTo.contains("\r") || returnTo.contains("\n")) {
            return null;
        }
        return returnTo;
    }

    private void storePinProfileId(HttpSession session, String returnTo) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^/profile/(\\d+)/edit$")
                .matcher(returnTo);
        if (matcher.matches()) {
            session.setAttribute("PASSWORD_RESET_PROFILE_ID", Long.valueOf(matcher.group(1)));
        } else {
            session.removeAttribute("PASSWORD_RESET_PROFILE_ID");
        }
    }

    // 7. 登録またはパスワード再設定で受け取ったメールアドレスをフォームに設定し、確認コード画面を表示する。
    @GetMapping("/verify-code")
    public String showVerifyCodeForm(
            @RequestParam(value = "email", required = false) String paramEmail, HttpSession session, Model model) {
        String email = (String) model.getAttribute("email");
        if (email == null || email.isBlank()) {
            email = paramEmail;
        }
        if (email == null || email.isBlank()) {
            return "redirect:/auth/login";
        }

        VerifyCodeRequest request = new VerifyCodeRequest();
        request.setEmail(email);
        Boolean isResetFlow = (Boolean) session.getAttribute(SessionConstants.IS_RESET_FLOW);
        model.addAttribute("verifyCodeRequest", request);
        model.addAttribute("email", email);
        model.addAttribute("maskedEmail", maskEmail(email));
        model.addAttribute("isResetFlow", Boolean.TRUE.equals(isResetFlow));
        return "auth/verify-code";
    }

    // 8. 入力された確認コードを検証し、成功した場合は再設定対象のメールアドレスをセッションに保存する。
    @PostMapping("/verify-code")
    public String handleVerifyCode(
            @Valid @ModelAttribute("verifyCodeRequest") VerifyCodeRequest request,
            BindingResult bindingResult, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Boolean isResetFlow = (Boolean) session.getAttribute(SessionConstants.IS_RESET_FLOW);
        if (bindingResult.hasErrors()) {
            setupVerifyCodeModel(model, request.getEmail(), isResetFlow);
            return "auth/verify-code";
        }
        try {
            verifyCodeService.verifyCode(request);
            // パスワード再設定の場合は、新しいパスワード入力画面へ遷移する。
            if (Boolean.TRUE.equals(isResetFlow)) {
                session.removeAttribute(SessionConstants.IS_RESET_FLOW);
                session.setAttribute(SessionConstants.RESET_EMAIL, request.getEmail());
                return "redirect:/auth/reset-password";
            }
            // 新規会員登録の場合は、ログイン画面へ遷移する。
            redirectAttributes.addFlashAttribute("message", "メールアドレスの認証が完了しました。ログインしてください。");
            return "redirect:/auth/login";
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            setupVerifyCodeModel(model, request.getEmail(), isResetFlow);
            return "auth/verify-code";
        }
    }

    private void setupVerifyCodeModel(Model model, String email, Boolean isResetFlow) {
        model.addAttribute("email", email);
        model.addAttribute("maskedEmail", maskEmail(email));
        model.addAttribute("isResetFlow", Boolean.TRUE.equals(isResetFlow));
    }

    // 9. セッションに再設定対象のメールアドレスがある場合だけ、パスワード変更画面を表示する。
    @GetMapping("/reset-password")
    public String showResetPasswordForm(HttpSession session, Model model) {
        String resetEmail = (String) session.getAttribute(SessionConstants.RESET_EMAIL);

        if (resetEmail == null || resetEmail.isBlank()) {
            return "redirect:/auth/forgot-password";
        }

        model.addAttribute("email", resetEmail);
        model.addAttribute("passwordResetConfirmRequest", new PasswordResetConfirmRequest());
        return "auth/reset-password";
    }

    // 10. セッションのメールアドレスと新しいパスワードを検証し、更新完了後にログイン画面へ遷移する。
    @PostMapping("/reset-password")
    public String handleResetPassword(
            @Valid @ModelAttribute("passwordResetConfirmRequest") PasswordResetConfirmRequest request,
            BindingResult bindingResult,
            @RequestParam(value = "email", required = false) String paramEmail,
            HttpSession session, Model model, RedirectAttributes redirectAttributes) {

        String resetEmail = (String) session.getAttribute(SessionConstants.RESET_EMAIL);
        if (resetEmail == null || resetEmail.isBlank()) {
            resetEmail = paramEmail;
        }
        if (resetEmail == null || resetEmail.isBlank()) {
            model.addAttribute("errorMessage", "有効なリクエストではありません。もう一度お試しください。");
            return "auth/reset-password";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("email", resetEmail);
            return "auth/reset-password";
        }

        try {
            Long pinProfileId = (Long) session.getAttribute("PASSWORD_RESET_PROFILE_ID");
            passwordResetService.resetPassword(resetEmail, request, pinProfileId);
            session.removeAttribute(SessionConstants.RESET_EMAIL);
            session.removeAttribute(SessionConstants.IS_RESET_FLOW);
            session.removeAttribute("PASSWORD_RESET_RETURN_TO");
            session.removeAttribute("PASSWORD_RESET_PROFILE_ID");
            SecurityContextHolder.clearContext();
            session.invalidate();
            redirectAttributes.addFlashAttribute("message", "パスワードの再設定が完了しました。新しいパスワードでログインしてください。");
            return "redirect:/auth/login";
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("email", resetEmail);
            return "auth/reset-password";
        }
    }

    // 11. 現在の認証情報を破棄してログアウトし、ログイン画面へリダイレクトする。
    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/auth/login";
    }

    // 12. 指定されたメールアドレスへ確認コードを再送し、結果メッセージを確認画面へ表示する。
    @PostMapping("/resend-code")
    public String resendCode(@RequestParam String email, @RequestParam(defaultValue = "false") boolean resetFlow,
            HttpSession session, RedirectAttributes redirectAttributes) {

        try {
            verifyCodeService.resendCode(email);
            session.setAttribute(SessionConstants.IS_RESET_FLOW, resetFlow);
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("message", "確認コードを再送信しました。");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("email", email);
        }
        return "redirect:/auth/verify-code";
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) {
            return local.charAt(0) + "***@" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }
}