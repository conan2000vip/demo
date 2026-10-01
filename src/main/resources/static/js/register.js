// register.js：入力中と送信時の登録フォーム検証
document.addEventListener('DOMContentLoaded', () => {

    const form = document.getElementById('registerForm');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const confirmPasswordInput = document.getElementById('confirmPassword');

    const emailError = document.getElementById('emailError');
    const passwordError = document.getElementById('passwordError');
    const confirmPasswordError = document.getElementById('confirmPasswordError');

    // ★ 1. Lấy element banner lỗi từ Server (dải màu hồng)
    const serverErrorBanner = document.querySelector('.banner--error, .auth-alert--error');

    // ★ Hàm hỗ trợ ẩn banner Server
    function hideServerError() {
        if (serverErrorBanner) serverErrorBanner.style.display = 'none';
    }

    // --- 1. メールアドレスの検証 ---
    function validateEmail() {
        if (!emailInput || !emailError) return true;
        hideServerError(); // ★ Ẩn banner server khi validate

        const val = emailInput.value.trim();
        const span = emailError.querySelector('span');

        if (val === '') {
            span.textContent = 'メールアドレスを入力してください';
            emailError.classList.add('is-visible');
            emailInput.classList.add('has-error'); // ★ Thêm viền đỏ
            return false;
        }
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) {
            span.textContent = '正しいメールアドレスを入力してください';
            emailError.classList.add('is-visible');
            emailInput.classList.add('has-error'); // ★ Thêm viền đỏ
            return false;
        }

        emailError.classList.remove('is-visible');
        emailInput.classList.remove('has-error'); // ★ Xóa viền đỏ
        return true;
    }

    // --- 2. Javaの@ValidPasswordと同じルールでパスワードを検証 ---
    function validatePassword() {
        if (!passwordInput || !passwordError) return true;
        hideServerError(); // ★ Ẩn banner server khi validate

        const val = passwordInput.value;
        const span = passwordError.querySelector('span');

        const showError = (msg) => {
            span.textContent = msg;
            passwordError.classList.add('is-visible');
            passwordInput.classList.add('has-error'); // ★ Thêm viền đỏ
        };

        if (val === '') {
            showError('パスワードを入力してください');
            return false;
        }
        if (val.length < 8) {
            showError('パスワードは8文字以上で入力してください');
            return false;
        }
        if (!/[a-z]/.test(val)) {
            showError('小文字を1文字以上入力してください');
            return false;
        }
        if (!/[A-Z]/.test(val)) {
            showError('大文字を1文字以上入力してください');
            return false;
        }
        if (!/\d/.test(val)) {
            showError('数字を1文字以上入力してください');
            return false;
        }
        if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(val)) {
            showError('記号を1文字以上入力してください');
            return false;
        }
        if (/(.)\1{2,}/.test(val)) {
            showError('同じ文字を3回以上連続して使用できません');
            return false;
        }

        passwordError.classList.remove('is-visible');
        passwordInput.classList.remove('has-error'); // ★ Xóa viền đỏ
        return true;
    }

    // --- 3. 確認用パスワードの検証 ---
    function validateConfirmPassword() {
        if (!confirmPasswordInput || !confirmPasswordError) return true;
        hideServerError(); // ★ Ẩn banner server khi validate

        const val = confirmPasswordInput.value;
        const passVal = passwordInput ? passwordInput.value : '';
        const span = confirmPasswordError.querySelector('span');

        if (val === '') {
            span.textContent = 'パスワード（確認）を入力してください';
            confirmPasswordError.classList.add('is-visible');
            confirmPasswordInput.classList.add('has-error'); // ★ Thêm viền đỏ
            return false;
        }
        if (val !== passVal) {
            span.textContent = 'パスワードが一致しません';
            confirmPasswordError.classList.add('is-visible');
            confirmPasswordInput.classList.add('has-error'); // ★ Thêm viền đỏ
            return false;
        }

        confirmPasswordError.classList.remove('is-visible');
        confirmPasswordInput.classList.remove('has-error'); // ★ Xóa viền đỏ
        return true;
    }

    // --- 入力中のリアルタイム検証イベント ---
    if (emailInput) emailInput.addEventListener('input', validateEmail);
    if (passwordInput) {
        passwordInput.addEventListener('input', () => {
            validatePassword();
            if (confirmPasswordInput && confirmPasswordInput.value !== '') {
                validateConfirmPassword();
            }
        });
    }
    if (confirmPasswordInput) confirmPasswordInput.addEventListener('input', validateConfirmPassword);

    // --- フォーム送信時の検証イベント ---
    if (form) {
        form.addEventListener('submit', (e) => {
            hideServerError(); // ★ Ẩn banner server khi bấm submit
            const vEmail = validateEmail();
            const vPass = validatePassword();
            const vConfirm = validateConfirmPassword();

            if (!vEmail || !vPass || !vConfirm) {
                e.preventDefault(); // 画面上にエラーがある場合はサーバーへの送信を中止する。
            }
        });
    }
});