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

    // 1. GET: ログインフォームを表示
    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }

    // 2. POST: ログインを処理（HttpServletRequest/Responseを使用）
    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute("loginRequest") LoginRequest loginRequest, BindingResult bindingResult,
            HttpServletRequest request, HttpServletResponse response, Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/login";
        }

        try {
            // サービスでパスワードとメール認証を確認し、リクエストとレスポンスを通じてセッションを保存
            loginService.login(loginRequest);
            return "redirect:/dashboard";
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            if (e.getStatus() == HttpStatus.FORBIDDEN) {
                model.addAttribute("emailNotVerified", true);
                model.addAttribute("email", loginRequest.getEmail());
            }
            return "auth/login";
        }
    }

    // 3. GET: 登録フォームを表示
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    // 4. POST: 登録を処理
    @PostMapping("/register")
    public String handleRegister(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request, BindingResult bindingResult,
            Model model, RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            registerService.register(request);
            // 登録後、メール認証コード入力画面にリダイレクトし、メールアドレスとメッセージをフラッシュ属性として渡す
            redirectAttributes.addFlashAttribute("email", request.getEmail());
            redirectAttributes.addFlashAttribute("message", "仮登録が完了しました。メールに送信された確認コードをご入力ください。");
            return "redirect:/auth/verify-code";

        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
    }

    // 5. GET: パスワード再設定フォームを表示
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("passwordResetRequest", new PasswordResetRequest());
        return "auth/forgot-password";
    }

    // 5. POST: パスワード再設定リクエストを処理
    @PostMapping("/forgot-password")
    public String handleForgotPassword(
            @Valid @ModelAttribute("passwordResetRequest") PasswordResetRequest request,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }

        try {
            passwordResetService.sendPasswordResetEmail(request.getEmail());
            redirectAttributes.addFlashAttribute("email", request.getEmail());
            redirectAttributes.addFlashAttribute("message", "パスワード再設定用の確認コードを送信しました。");
            return "redirect:/auth/verify-code";

        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/forgot-password";
        }
    }

    // 6. GET: 確認コード入力フォームを表示
    @GetMapping("/verify-code")
    public String showVerifyCodeForm(Model model) {
        String email = (String) model.getAttribute("email");
        VerifyCodeRequest request = new VerifyCodeRequest();
        request.setEmail(email);

        model.addAttribute("verifyCodeRequest", request);
        return "auth/verify-code";
    }

    // 6. POST: 確認コードを検証
    @PostMapping("/verify-code")
    public String handleVerifyCode(
            @Valid @ModelAttribute("verifyCodeRequest") VerifyCodeRequest request,
            BindingResult bindingResult, HttpSession session, Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/verify-code";
        }

        try {
            verifyCodeService.verifyCode(request);
            session.setAttribute("RESET_EMAIL", request.getEmail());
            return "redirect:/auth/reset-password";

        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/verify-code";
        }
    }

    // 7. GET: パスワード再設定フォームを表示
    @GetMapping("/reset-password")
    public String showResetPasswordForm(HttpSession session, Model model) {
        String resetEmail = (String) session.getAttribute("RESET_EMAIL");

        if (resetEmail == null) {
            return "redirect:/auth/forgot-password";
        }

        model.addAttribute("passwordResetConfirmRequest", new PasswordResetConfirmRequest());
        return "auth/reset-password";
    }

    // 7. POST: パスワード再設定を処理
    @PostMapping("/reset-password")
    public String handleResetPassword(
            @Valid @ModelAttribute("passwordResetConfirmRequest") PasswordResetConfirmRequest request,
            BindingResult bindingResult, HttpSession session, Model model, RedirectAttributes redirectAttributes) {

        String resetEmail = (String) session.getAttribute("RESET_EMAIL");
        if (resetEmail == null) {
            return "redirect:/auth/forgot-password";
        }

        if (bindingResult.hasErrors()) {
            return "auth/reset-password";
        }

        try {
            passwordResetService.resetPassword(resetEmail, request);
            session.removeAttribute("RESET_EMAIL");
            redirectAttributes.addFlashAttribute("message", "パスワードの再設定が完了しました。新しいパスワードでログインしてください。");
            return "redirect:/auth/login";
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/reset-password";
        }
    }

    // 8. POST:ログアウト処理
    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/auth/login";
    }

    @PostMapping("/resend-code")
    public String resendCode(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            verifyCodeService.resendCode(email); 
            redirectAttributes.addFlashAttribute("message", "確認コードを再送信しました。");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/auth/verify-code";
    }
}
