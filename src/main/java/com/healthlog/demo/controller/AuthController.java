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

import com.healthlog.demo.dto.auth.LoginRequest;
import com.healthlog.demo.dto.auth.PasswordResetConfirmRequest;
import com.healthlog.demo.dto.auth.PasswordResetRequest;
import com.healthlog.demo.dto.auth.RegisterRequest;
import com.healthlog.demo.dto.auth.VerifyCodeRequest;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.service.auth.LoginService;
import com.healthlog.demo.service.auth.PasswordResetService;
import com.healthlog.demo.service.auth.RegisterService;
import com.healthlog.demo.service.auth.VerifyCodeService;

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

    // 1. ログイン画面を表示し、ユーザーがメールアドレスとパスワードを入力できるフォームを準備する。
    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }

    // 2. ログイン入力を検証し、問題がなければ認証サービスでユーザーを認証してダッシュボードへ遷移する。
    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute("loginRequest") LoginRequest loginRequest, BindingResult bindingResult,
            HttpServletRequest request, HttpServletResponse response, Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/login";
        }

        try {
            // ログインサービスでパスワードとメール認証状態を確認し、認証成功後はダッシュボードへ移動する。
            loginService.login(loginRequest);
            return "redirect:/dashboard";
        } catch (BusinessException e) {
            // 認証に失敗した場合はエラーメッセージを表示し、未認証ユーザーにはメールアドレスを再確認させる。
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
            session.removeAttribute("IS_RESET_FLOW");
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
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("passwordResetRequest", new PasswordResetRequest());
        return "auth/forgot-password";
    }

    // 6. メールアドレスを検証し、パスワード再設定用の確認コードを送信して確認画面へ遷移する。
    @PostMapping("/forgot-password")
    public String handleForgotPassword(
            @Valid @ModelAttribute("passwordResetRequest") PasswordResetRequest request,
            BindingResult bindingResult, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }

        try {
            passwordResetService.sendPasswordResetEmail(request.getEmail());
            session.setAttribute("IS_RESET_FLOW", true);
            redirectAttributes.addFlashAttribute("email", request.getEmail());
            redirectAttributes.addFlashAttribute("message", "パスワード再設定用の確認コードを送信しました。");
            return "redirect:/auth/verify-code";
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/forgot-password";
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
        Boolean isResetFlow = (Boolean) session.getAttribute("IS_RESET_FLOW");
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
        Boolean isResetFlow = (Boolean) session.getAttribute("IS_RESET_FLOW");
        if (bindingResult.hasErrors()) {
            setupVerifyCodeModel(model, request.getEmail(), isResetFlow);
            return "auth/verify-code";
        }
        try {
            verifyCodeService.verifyCode(request);
            // パスワード再設定フローの場合 -> 新しいパスワード入力画面へ
            if (Boolean.TRUE.equals(isResetFlow)) {
                session.removeAttribute("IS_RESET_FLOW");
                session.setAttribute("RESET_EMAIL", request.getEmail());
                return "redirect:/auth/reset-password";
            }
            // 新規会員登録フローの場合 -> ログイン画面へ
            redirectAttributes.addFlashAttribute("message", "メールアドレスの認証が完了しました。ログインしてください。");
            return "redirect:/auth/login";
        } catch (BusinessException e) {
            // コード間違い等のエラー時にも header 情報（isResetFlow, maskedEmail）を再設定する
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
        String resetEmail = (String) session.getAttribute("RESET_EMAIL");

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

        String resetEmail = (String) session.getAttribute("RESET_EMAIL");
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
            passwordResetService.resetPassword(resetEmail, request);
            session.removeAttribute("RESET_EMAIL"); 
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
            // Spring Securityのログアウト処理で認証情報とセッションを終了する。
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/auth/login";
    }

    // 12. 指定されたメールアドレスへ確認コードを再送し、結果メッセージを確認画面へ表示する。
    @PostMapping("/resend-code")
    public String resendCode(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            verifyCodeService.resendCode(email);
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
