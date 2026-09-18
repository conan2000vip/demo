// パスワード再設定画面の処理（reset-password.js）
document.addEventListener('DOMContentLoaded', () => {
    // ---- 各入力欄のパスワード表示・非表示を切り替え ----
    document.querySelectorAll('.toggle-password').forEach((btn) => {
        btn.addEventListener('click', () => {
            const target = document.getElementById(btn.dataset.target);
            const icon = btn.querySelector('i');
            const isHidden = target.type === 'password';
            target.type = isHidden ? 'text' : 'password';
            if (icon) icon.setAttribute('data-lucide', isHidden ? 'eye-off' : 'eye');
            if (window.lucide) lucide.createIcons();
        });
    });

    // ---- 送信前に基本入力を確認 ----
    const form = document.getElementById('resetPasswordForm');
    const newPassword = document.getElementById('newPassword');
    const confirmPassword = document.getElementById('confirmPassword');
    const newPasswordError = document.getElementById('newPasswordError');
    const confirmPasswordError = document.getElementById('confirmPasswordError');

    form.addEventListener('submit', (e) => {
        let hasError = false;

        [newPasswordError, confirmPasswordError].forEach(el => {
            el.classList.remove('is-visible');
        });

        if (newPassword.value.length < 8) {
            newPasswordError.querySelector('span').textContent = 'パスワードは8文字以上で入力してください';
            newPasswordError.classList.add('is-visible');
            hasError = true;
        }

        if (confirmPassword.value !== newPassword.value) {
            confirmPasswordError.querySelector('span').textContent = 'パスワードが一致しません';
            confirmPasswordError.classList.add('is-visible');
            hasError = true;
        }

        if (hasError) e.preventDefault();
    });
});