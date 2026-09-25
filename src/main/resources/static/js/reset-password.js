// reset-password.js - Realtime & Submit Validation
document.addEventListener('DOMContentLoaded', () => {

    const form = document.getElementById('resetPasswordForm');
    const newPasswordInput = document.getElementById('newPassword');
    const confirmPasswordInput = document.getElementById('confirmPassword');

    const newPasswordError = document.getElementById('newPasswordError');
    const confirmPasswordError = document.getElementById('confirmPasswordError');

    // --- 1. Validate Mật khẩu mới (Khớp 100% quy tắc @ValidPassword Java & register.js) ---
    function validateNewPassword() {
        if (!newPasswordInput || !newPasswordError) return true;
        const val = newPasswordInput.value;
        const span = newPasswordError.querySelector('span');

        if (val === '') {
            span.textContent = '新しいパスワードを入力してください';
            newPasswordError.classList.add('is-visible');
            return false;
        }
        if (val.length < 8) {
            span.textContent = 'パスワードは8文字以上で入力してください';
            newPasswordError.classList.add('is-visible');
            return false;
        }
        if (!/[a-z]/.test(val)) {
            span.textContent = '小文字を1文字以上入力してください';
            newPasswordError.classList.add('is-visible');
            return false;
        }
        if (!/[A-Z]/.test(val)) {
            span.textContent = '大文字を1文字以上入力してください';
            newPasswordError.classList.add('is-visible');
            return false;
        }
        if (!/\d/.test(val)) {
            span.textContent = '数字を1文字以上入力してください';
            newPasswordError.classList.add('is-visible');
            return false;
        }
        if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(val)) {
            span.textContent = '記号を1文字以上入力してください';
            newPasswordError.classList.add('is-visible');
            return false;
        }
        if (/(.)\1{2,}/.test(val)) {
            span.textContent = '同じ文字を3回以上連続して使用できません';
            newPasswordError.classList.add('is-visible');
            return false;
        }

        newPasswordError.classList.remove('is-visible');
        return true;
    }

    // --- 2. Validate Xác nhận mật khẩu ---
    function validateConfirmPassword() {
        if (!confirmPasswordInput || !confirmPasswordError) return true;
        const val = confirmPasswordInput.value;
        const passVal = newPasswordInput ? newPasswordInput.value : '';
        const span = confirmPasswordError.querySelector('span');

        if (val === '') {
            span.textContent = '新しいパスワード（確認）を入力してください';
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

    // --- Events Realtime (Kiểm tra khi gõ) ---
    if (newPasswordInput) {
        newPasswordInput.addEventListener('input', () => {
            validateNewPassword();
            if (confirmPasswordInput && confirmPasswordInput.value !== '') {
                validateConfirmPassword();
            }
        });
    }

    if (confirmPasswordInput) {
        confirmPasswordInput.addEventListener('input', validateConfirmPassword);
    }

    // --- Form Submit Event ---
    if (form) {
        form.addEventListener('submit', (e) => {
            const vPass = validateNewPassword();
            const vConfirm = validateConfirmPassword();

            if (!vPass || !vConfirm) {
                e.preventDefault(); // Chặn submit nếu có lỗi
            }
        });
    }
});