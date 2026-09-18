// ログイン画面の処理（login.js）
// ログイン画面固有の処理: 基本入力チェックと確認メール再送フォームのメール同期

document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('loginForm');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const emailError = document.getElementById('emailError');
    const passwordError = document.getElementById('passwordError');

    // =========================================================
    // 送信前に基本入力を確認（未入力や形式不正なら送信を止め、
    // 明らかなエラーでサーバーを呼び出さない）
    // =========================================================
    if (form) {
        form.addEventListener('submit', (e) => {
            let hasError = false;

            [emailError, passwordError].forEach(el => el?.classList.remove('is-visible'));
            [emailInput, passwordInput].forEach(el => el?.classList.remove('has-error'));

            if (!emailInput.value.trim()) {
                showError(emailInput, emailError, 'メールアドレスを入力してください');
                hasError = true;
            } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailInput.value)) {
                showError(emailInput, emailError, '正しいメールアドレスを入力してください');
                hasError = true;
            }

            if (!passwordInput.value) {
                showError(passwordInput, passwordError, 'パスワードを入力してください');
                hasError = true;
            }

            if (hasError) e.preventDefault();
        });
    }

    function showError(input, errorBox, message) {
        input.classList.add('has-error');
        if (errorBox) {
            errorBox.querySelector('span').textContent = message;
            errorBox.classList.add('is-visible');
        }
    }

    // =========================================================
    // 現在のメールアドレスを確認メール再送フォームのhidden inputへ同期
    // ログイン失敗後に別のメールアドレスを入力しても、古いアドレスではなく現在の値を送信する
    // =========================================================
    const resendHidden = document.getElementById('resendEmailHidden');
    if (emailInput && resendHidden) {
        emailInput.addEventListener('input', () => {
            resendHidden.value = emailInput.value;
        });
    }
});