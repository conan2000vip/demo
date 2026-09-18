// 共通処理（common.js）
// 認証画面（ログイン、登録、パスワード忘れ、コード確認、パスワード再設定）で共通利用する処理

document.addEventListener('DOMContentLoaded', () => {

    // =========================================================
    // パスワード表示・非表示の切り替え（すべてのパスワード入力欄で共通）
    // ボタンの配置方法に対応:
    //   1) <button class="toggle-password" data-target="入力欄のID">
    //   2) パスワード入力欄と同じ.input-box内に<button class="toggle-password">
    // =========================================================
    document.querySelectorAll('.toggle-password').forEach((btn) => {
        btn.addEventListener('click', () => {
            let target;

            if (btn.dataset.target) {
                target = document.getElementById(btn.dataset.target);
            } else {
                target = btn.closest('.input-box')?.querySelector('input');
            }
            if (!target) return;

            const icon = btn.querySelector('i');
            const willShow = target.type === 'password';
            target.type = willShow ? 'text' : 'password';

            if (icon) {
                icon.setAttribute('data-lucide', willShow ? 'eye-off' : 'eye');
            }
            if (window.lucide) {
                lucide.createIcons();
            }
        });
    });

    // =========================================================
    // ユーザーが入力欄を修正し始めたらエラー状態（赤い枠とエラーテキスト）を消去
    // 画面に古いエラーが残らないようにする
    // =========================================================
    document.querySelectorAll('.input-box input, .otp-box').forEach((input) => {
        input.addEventListener('input', () => {
            input.classList.remove('has-error');

            const group = input.closest('.input-group');
            const errorBox = group?.querySelector('.field-error');
            if (errorBox) {
                errorBox.classList.remove('is-visible');
            }
        });
    });

});