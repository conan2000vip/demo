// 確認コード画面の処理（verify-code.js）
// 6桁の確認コード入力画面の操作性を制御（登録画面とパスワード忘れ画面で共通）

document.addEventListener('DOMContentLoaded', () => {
    const boxes = Array.from(document.querySelectorAll('.otp-box'));
    const combinedInput = document.getElementById('codeCombined');
    const form = document.getElementById('verifyForm');
    const codeError = document.getElementById('codeError');

    const resendBtn = document.getElementById('resendBtn');
    const resendTimerText = document.getElementById('resendTimerText');

    // ---- 画面表示時に最初の入力欄へ自動フォーカス ----
    if (boxes[0]) boxes[0].focus();

    // ---- 数字入力: 1桁だけ保持し、次の入力欄へ自動移動 ----
    boxes.forEach((box, index) => {
        box.addEventListener('input', () => {
            box.value = box.value.replace(/[^0-9]/g, '').slice(0, 1);
            box.classList.remove('has-error');
            if (box.value && index < boxes.length - 1) {
                boxes[index + 1].focus();
            }
            syncCombined();
        });

        // ---- 空欄でBackspaceを押したら前の入力欄へ移動 ----
        box.addEventListener('keydown', (e) => {
            if (e.key === 'Backspace' && !box.value && index > 0) {
                boxes[index - 1].focus();
            }
            if (e.key === 'ArrowLeft' && index > 0) {
                boxes[index - 1].focus();
            }
            if (e.key === 'ArrowRight' && index < boxes.length - 1) {
                boxes[index + 1].focus();
            }
        });

        // ---- 任意の入力欄へ6桁の数字を貼り付け ----
        box.addEventListener('paste', (e) => {
            e.preventDefault();
            const pasted = (e.clipboardData || window.clipboardData)
                .getData('text')
                .replace(/[^0-9]/g, '')
                .slice(0, boxes.length);

            pasted.split('').forEach((digit, i) => {
                if (boxes[i]) boxes[i].value = digit;
            });

            const nextEmpty = boxes.findIndex(b => !b.value);
            (nextEmpty === -1 ? boxes[boxes.length - 1] : boxes[nextEmpty]).focus();
            syncCombined();
        });
    });

    function syncCombined() {
        combinedInput.value = boxes.map(b => b.value).join('');
    }

    // ---- 送信前にコードを結合し、6桁すべて入力されているか確認 ----
    form.addEventListener('submit', (e) => {
        syncCombined();
        if (combinedInput.value.length < boxes.length) {
            e.preventDefault();
            boxes.forEach(b => { if (!b.value) b.classList.add('has-error'); });
            codeError.classList.add('is-visible');
            codeError.querySelector('span').textContent = '6桁すべて入力してください';
        }
    });

    // ---- 「コードを再送」ボタンのカウントダウン ----
    let secondsLeft = 60;
    resendTimerText.textContent = `${secondsLeft}秒後に再送できます`;

    const timer = setInterval(() => {
        secondsLeft -= 1;
        if (secondsLeft <= 0) {
            clearInterval(timer);
            resendTimerText.textContent = '';
            resendBtn.disabled = false;
        } else {
            resendTimerText.textContent = `${secondsLeft}秒後に再送できます`;
        }
    }, 1000);

    // ---- コード再送ボタンの処理 ----
    resendBtn.addEventListener('click', () => {
        if (resendBtn.disabled) return;

        const email = document.querySelector('input[name="email"]').value;
        const purpose = document.querySelector('input[name="purpose"]').value;

        // ★ バックエンドとの接続点: resend-code APIをプロジェクトの構成に合わせて呼び出す
        fetch('/auth/resend-code', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({ email, purpose })
        }).then(() => {
            resendBtn.disabled = true;
            secondsLeft = 60;
            const retryTimer = setInterval(() => {
                secondsLeft -= 1;
                if (secondsLeft <= 0) {
                    clearInterval(retryTimer);
                    resendTimerText.textContent = '';
                    resendBtn.disabled = false;
                } else {
                    resendTimerText.textContent = `${secondsLeft}秒後に再送できます`;
                }
            }, 1000);
        });
    });
});