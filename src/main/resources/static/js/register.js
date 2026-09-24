// register.js - Realtime & Submit Validation
document.addEventListener('DOMContentLoaded', () => {

    const form = document.getElementById('registerForm');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const confirmPasswordInput = document.getElementById('confirmPassword');

    const emailError = document.getElementById('emailError');
    const passwordError = document.getElementById('passwordError');
    const confirmPasswordError = document.getElementById('confirmPasswordError');

    // --- 1. Email Check ---
    function validateEmail() {
        if (!emailInput || !emailError) return true;
        const val = emailInput.value.trim();
        const span = emailError.querySelector('span');

        if (val === '') {
            span.textContent = 'メールアドレスを入力してください';
            emailError.classList.add('is-visible');
            return false;
        }
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) {
            span.textContent = '正しいメールアドレスを入力してください';
            emailError.classList.add('is-visible');
            return false;
        }
        emailError.classList.remove('is-visible');
        return true;
    }

    // --- 2. Password Check (Khớp 100% quy tắc @ValidPassword Java) ---
    function validatePassword() {
        if (!passwordInput || !passwordError) return true;
        const val = passwordInput.value;
        const span = passwordError.querySelector('span');

        if (val === '') {
            span.textContent = 'パスワードを入力してください';
            passwordError.classList.add('is-visible');
            return false;
        }
        if (val.length < 8) {
            span.textContent = 'パスワードは8文字以上で入力してください';
            passwordError.classList.add('is-visible');
            return false;
        }
        if (!/[a-z]/.test(val)) {
            span.textContent = '小文字を1文字以上入力してください';
            passwordError.classList.add('is-visible');
            return false;
        }
        if (!/[A-Z]/.test(val)) {
            span.textContent = '大文字を1文字以上入力してください';
            passwordError.classList.add('is-visible');
            return false;
        }
        if (!/\d/.test(val)) {
            span.textContent = '数字を1文字以上入力してください';
            passwordError.classList.add('is-visible');
            return false;
        }
        if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(val)) {
            span.textContent = '記号を1文字以上入力してください';
            passwordError.classList.add('is-visible');
            return false;
        }
        if (/(.)\1{2,}/.test(val)) {
            span.textContent = '同じ文字を3回以上連続して使用できません';
            passwordError.classList.add('is-visible');
            return false;
        }

        passwordError.classList.remove('is-visible');
        return true;
    }

    // --- 3. Confirm Password Check ---
    function validateConfirmPassword() {
        if (!confirmPasswordInput || !confirmPasswordError) return true;
        const val = confirmPasswordInput.value;
        const passVal = passwordInput ? passwordInput.value : '';
        const span = confirmPasswordError.querySelector('span');

        if (val === '') {
            span.textContent = 'パスワード（確認）を入力してください';
            confirmPasswordError.classList.add('is-visible');
            return false;
        }
        if (val !== passVal) {
            span.textContent = 'パスワードが一致しません';
            confirmPasswordError.classList.add('is-visible');
            return false;
        }
        confirmPasswordError.classList.remove('is-visible');
        return true;
    }

    // --- Events Realtime ---
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

    // --- Form Submit Event ---
    if (form) {
        form.addEventListener('submit', (e) => {
            const vEmail = validateEmail();
            const vPass = validatePassword();
            const vConfirm = validateConfirmPassword();

            if (!vEmail || !vPass || !vConfirm) {
                e.preventDefault(); // Chặn submit lên server nếu UI báo lỗi
            }
        });
    }
});