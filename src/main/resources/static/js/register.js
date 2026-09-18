// 新規登録画面の処理（register.js）
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
    const form = document.getElementById('registerForm');
    const email = document.getElementById('email');
    const password = document.getElementById('password');
    const confirmPassword = document.getElementById('confirmPassword');

    const emailError = document.getElementById('emailError');
    const passwordError = document.getElementById('passwordError');
    const confirmPasswordError = document.getElementById('confirmPasswordError');

    form.addEventListener('submit', (e) => {
        let hasError = false;
        [emailError, passwordError, confirmPasswordError].forEach(el => el.classList.remove('is-visible'));

        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) {
            emailError.querySelector('span').textContent = '正しいメールアドレスを入力してください';
            emailError.classList.add('is-visible');
            hasError = true;
        }

        if (password.value.length < 8) {
            passwordError.querySelector('span').textContent = 'パスワードは8文字以上で入力してください';
            passwordError.classList.add('is-visible');
            hasError = true;
        }

        if (confirmPassword.value !== password.value) {
            confirmPasswordError.querySelector('span').textContent = 'パスワードが一致しません';
            confirmPasswordError.classList.add('is-visible');
            hasError = true;
        }

        if (hasError) e.preventDefault();
    });
});