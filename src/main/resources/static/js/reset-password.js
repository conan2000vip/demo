// reset-password.js：入力中と送信時のパスワード検証
document.addEventListener('DOMContentLoaded', () => {

    const form = document.getElementById('resetPasswordForm');
    const newPasswordInput = document.getElementById('newPassword');
    const confirmPasswordInput = document.getElementById('confirmPassword');

    const newPasswordError = document.getElementById('newPasswordError');
    const confirmPasswordError = document.getElementById('confirmPasswordError');

    // --- 1. Javaの@ValidPasswordおよびregister.jsと同じルールで新しいパスワードを検証 ---
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

    // --- 2. 確認用パスワードを検証 ---
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

    // --- 入力中のリアルタイム検証イベント ---
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

    // --- フォーム送信時の検証イベント ---
    if (form) {
        form.addEventListener('submit', (e) => {
            const vPass = validateNewPassword();
            const vConfirm = validateConfirmPassword();

            if (!vPass || !vConfirm) {
                e.preventDefault(); // エラーがある場合は送信を中止する。
            }
        });
    }
});