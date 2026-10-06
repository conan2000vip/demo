// ログイン画面の処理（login.js）
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('loginForm');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const emailError = document.getElementById('emailError');
    const passwordError = document.getElementById('passwordError');
    const serverErrorBanner = document.querySelector('.banner--error, .auth-alert--error');

    // =========================================================
    // ヘルパー関数（エラー表示・クリア）
    // =========================================================
    function showError(input, errorBox, message) {
        if (input) input.classList.add('has-error');
        if (errorBox) {
            const span = errorBox.querySelector('span');
            if (span) span.textContent = message;
            errorBox.classList.add('is-visible');
        }
        if (serverErrorBanner) serverErrorBanner.style.display = 'none';
    }

    function clearError(input, errorBox) {
        if (input) input.classList.remove('has-error');
        if (errorBox) errorBox.classList.remove('is-visible');
        if (serverErrorBanner) serverErrorBanner.style.display = 'none';
    }

    // =========================================================
    // ★ 検証ロジック（Validate Email & Password）
    // =========================================================
    function validateEmail() {
        if (!emailInput) return true;
        const val = emailInput.value.trim();

        if (!val) {
            showError(emailInput, emailError, 'メールアドレスを入力してください');
            return false;
        }
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) {
            showError(emailInput, emailError, '正しいメールアドレスを入力してください');
            return false;
        }

        clearError(emailInput, emailError);
        return true;
    }

    function validatePassword() {
        if (!passwordInput) return true;
        const val = passwordInput.value;

        if (!val) {
            showError(passwordInput, passwordError, 'パスワードを入力してください');
            return false;
        }

        clearError(passwordInput, passwordError);
        return true;
    }

    // =========================================================
    // ★ リアルタイムイベントの設定
    // =========================================================
    if (emailInput) {
        // 入力中は以前のエラー表示を隠す。
        emailInput.addEventListener('input', () => clearError(emailInput, emailError));
        // メール欄から移動したときに形式を検証する。
        emailInput.addEventListener('blur', validateEmail);
    }

    if (passwordInput) {
        passwordInput.addEventListener('input', () => clearError(passwordInput, passwordError));
        passwordInput.addEventListener('blur', validatePassword);
    }

    // =========================================================
    // フォーム送信時の検証
    // =========================================================
    if (form) {
        form.addEventListener('submit', (e) => {
            if (serverErrorBanner) serverErrorBanner.style.display = 'none';
            const isEmailValid = validateEmail();
            const isPasswordValid = validatePassword();

            if (!isEmailValid || !isPasswordValid) {
                e.preventDefault(); // エラーがある場合は送信を停止する。
            }
        });
    }

    // 同期処理
    const resendHidden = document.getElementById('resendEmailHidden');
    if (emailInput && resendHidden) {
        emailInput.addEventListener('input', () => {
            resendHidden.value = emailInput.value;
        });
    }
});