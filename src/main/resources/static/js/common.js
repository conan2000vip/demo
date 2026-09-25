// 共通画面処理（common.js）
document.addEventListener('DOMContentLoaded', () => {

    // Lucideアイコンを画面に描画する。
    if (window.lucide) {
        lucide.createIcons();
    }

    // パスワードの表示と非表示を切り替える。
    document.querySelectorAll('.toggle-password').forEach((btn) => {
        btn.addEventListener('click', () => {
            let target;
            if (btn.dataset.target) {
                target = document.getElementById(btn.dataset.target);
            } else {
                target = btn.closest('.input-box')?.querySelector('input');
            }
            if (!target) return;

            const icon = btn.querySelector('[data-lucide]') || btn.querySelector('svg');
            const willShow = target.type === 'password';

            target.type = willShow ? 'text' : 'password';

            if (icon) {
                icon.setAttribute('data-lucide', willShow ? 'eye-off' : 'eye');
                if (window.lucide) lucide.createIcons();
            }
        });
    });
});