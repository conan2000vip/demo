(function () {
    'use strict';

    const PIN_SCROLL_POSITION_KEY = 'profile-pin-scroll-position';

    function rememberScrollPosition() {
        try {
            sessionStorage.setItem(PIN_SCROLL_POSITION_KEY, JSON.stringify({
                path: window.location.pathname,
                top: window.scrollY
            }));
        } catch (error) {
            // プライバシー設定によりストレージを利用できない場合がある。
        }
    }

    function restoreScrollPosition() {
        try {
            const stored = sessionStorage.getItem(PIN_SCROLL_POSITION_KEY);
            if (!stored) return;
            const position = JSON.parse(stored);
            sessionStorage.removeItem(PIN_SCROLL_POSITION_KEY);
            if (position.path !== window.location.pathname
                || typeof position.top !== 'number') return;
            window.requestAnimationFrame(function () {
                window.scrollTo(0, position.top);
            });
        } catch (error) {
            sessionStorage.removeItem(PIN_SCROLL_POSITION_KEY);
        }
    }

    function setPinError(input, errorId, message, invalid) {
        const error = document.getElementById(errorId);
        if (!input || !error) {
            return invalid;
        }
        error.textContent = invalid ? message : '';
        input.classList.toggle('is-invalid', invalid);
        input.setCustomValidity(invalid ? message : '');
        return invalid;
    }

    function sanitizePinInput(input) {
        const digitsOnly = input.value.replace(/\D/g, '').slice(0, 4);
        if (input.value !== digitsOnly) {
            input.value = digitsOnly;
        }
        validatePinFields();
    }

    function validatePinFields() {
        const pin = document.getElementById('pin');
        const confirmation = document.getElementById('pinConfirmation');
        const current = document.getElementById('currentPin');
        const currentInvalid = current && current.value.length > 0
            && !/^\d{4}$/.test(current.value);
        const pinInvalid = pin && pin.value.length > 0
            && !/^\d{4}$/.test(pin.value);
        const confirmationInvalid = confirmation && confirmation.value.length > 0
            && (!/^\d{4}$/.test(confirmation.value)
                || (pin && /^\d{4}$/.test(pin.value) && confirmation.value !== pin.value));

        const currentError = setPinError(current, 'currentPinError',
            '現在の暗証番号は4桁の数字で入力してください。', currentInvalid);
        const pinError = setPinError(pin, 'pinError',
            'PINは4桁の数字で入力してください。', pinInvalid);
        const confirmationMessage = pin && /^\d{4}$/.test(pin.value)
            && confirmation && confirmation.value !== pin.value
            ? 'PINが一致していません。'
            : 'PINは4桁の数字で入力してください。';
        const confirmationError = setPinError(confirmation, 'pinConfirmationError',
            confirmationMessage, confirmationInvalid);

        return !(currentError || pinError || confirmationError);
    }

    function togglePinFields(enabled) {
        const fields = document.getElementById('pinFields');
        const saved = document.getElementById('pinSavedState');
        const status = document.getElementById('pinLockStatus');
        const hint = document.getElementById('pinLockHint');
        if (saved) {
            if (fields) fields.hidden = true;
            if (status) status.textContent = '暗証番号でロック中';
        }
        if (fields) {
            fields.hidden = !enabled;
        }
        if (status) {
            status.textContent = enabled ? '暗証番号でロックする' : 'ロックなしで使う';
        }
        if (hint) {
            hint.textContent = enabled
                ? 'ONにすると、この画面を開くときに4桁の暗証番号入力が必要になります。'
                : 'OFFにすると、暗証番号なしで、ボタンを押すだけで画面が開きます。';
        }
    }

    function submitPinSetup() {
        const pin = document.getElementById('pin');
        const confirmation = document.getElementById('pinConfirmation');
        const action = document.getElementById('pinAction');
        if (action && action.value === 'REMOVE') {
            const current = document.getElementById('currentPin');
            const validCurrent = current && /^\d{4}$/.test(current.value);
            if (!validCurrent) {
                setPinError(current, 'currentPinError',
                    '現在の暗証番号は4桁の数字で入力してください。', true);
                if (current) current.focus();
                return false;
            }
            rememberScrollPosition();
            return true;
        }
        if (!validatePinFields() || !pin || !confirmation
            || !/^\d{4}$/.test(pin.value) || pin.value !== confirmation.value) {
            (pin && !/^\d{4}$/.test(pin.value) ? pin : confirmation).focus();
            return false;
        }
        const toggle = document.getElementById('pinEnabled');
        if (toggle) {
            toggle.checked = true;
        }
        if (action) action.value = 'SAVE';
        rememberScrollPosition();
        return true;
    }

    function beginPinChange() {
        const saved = document.getElementById('pinSavedState');
        const editor = document.getElementById('pinFields');
        const action = document.getElementById('pinAction');
        const saveButton = document.getElementById('pinSaveButton');
        if (saved) saved.hidden = true;
        if (editor) {
            editor.classList.remove('pin-editor--hidden');
            editor.hidden = false;
        }
        if (action) action.value = '';
        if (saveButton) saveButton.textContent = 'PINを保存';
        const pin = document.getElementById('pin');
        if (pin) pin.focus();
    }

    function cancelPinChange() {
        const saved = document.getElementById('pinSavedState');
        const editor = document.getElementById('pinFields');
        const action = document.getElementById('pinAction');
        const toggle = document.getElementById('pinEnabled');
        const saveButton = document.getElementById('pinSaveButton');
        const newRow = document.getElementById('newPinRow');
        const confirmationRow = document.getElementById('pinConfirmationRow');
        const inputs = document.querySelectorAll('[data-pin-input]');

        inputs.forEach(function (input) {
            input.value = '';
            input.setCustomValidity('');
            input.classList.remove('is-invalid');
        });
        document.querySelectorAll('.pin-field-error').forEach(function (error) {
            error.textContent = '';
        });
        if (action) action.value = '';
        if (toggle) {
            toggle.checked = true;
            togglePinFields(true);
        }
        if (newRow) newRow.hidden = false;
        if (confirmationRow) confirmationRow.hidden = false;
        if (saveButton) saveButton.textContent = 'PINを保存';
        if (editor) {
            editor.hidden = true;
            editor.classList.add('pin-editor--hidden');
        }
        if (saved) saved.hidden = false;
    }

    function openPinUnlockModal() {
        const modal = document.getElementById('pinUnlockModal');
        const input = document.getElementById('unlockCurrentPin');
        const error = document.getElementById('unlockPinError');
        if (!modal || !input) return;
        input.value = '';
        if (error) error.textContent = '';
        modal.hidden = false;
        input.focus();
    }

    function closePinUnlockModal() {
        const modal = document.getElementById('pinUnlockModal');
        if (modal) modal.hidden = true;
        const toggle = document.getElementById('pinEnabled');
        if (toggle) {
            toggle.checked = true;
            togglePinFields(true);
        }
    }

    function openAccountPasswordModal() {
        const pinModal = document.getElementById('pinUnlockModal');
        const modal = document.getElementById('accountPasswordModal');
        const input = document.getElementById('accountPasswordInput');
        if (pinModal) pinModal.hidden = true;
        if (modal) modal.hidden = false;
        if (input) {
            input.value = '';
            input.focus();
        }
    }

    function closeAccountPasswordModal() {
        const modal = document.getElementById('accountPasswordModal');
        if (modal) modal.hidden = true;
        closePinUnlockModal();
    }

    function confirmAccountPassword() {
        const input = document.getElementById('accountPasswordInput');
        const error = document.getElementById('accountPasswordError');
        const password = input ? input.value : '';
        if (!password) {
            if (error) error.textContent = 'アカウントのパスワードを入力してください。';
            if (input) input.focus();
            return;
        }
        const accountPassword = document.getElementById('accountPassword');
        const action = document.getElementById('pinAction');
        const toggle = document.getElementById('pinEnabled');
        if (accountPassword) accountPassword.value = password;
        if (action) action.value = 'REMOVE_ACCOUNT_PASSWORD';
        if (toggle) toggle.checked = false;
        const form = document.querySelector('form.profile-form');
        if (form) {
            rememberScrollPosition();
            form.submit();
        }
    }

    function confirmPinUnlock() {
        const input = document.getElementById('unlockCurrentPin');
        const error = document.getElementById('unlockPinError');
        if (!input) return;
        const pin = input.value.replace(/\D/g, '').slice(0, 4);
        input.value = pin;
        if (!/^\d{4}$/.test(pin)) {
            if (error) error.textContent = '現在の暗証番号は4桁の数字で入力してください。';
            input.focus();
            return;
        }

        const currentPin = document.getElementById('currentPin');
        const action = document.getElementById('pinAction');
        const toggle = document.getElementById('pinEnabled');
        if (currentPin) currentPin.value = pin;
        if (action) action.value = 'REMOVE';
        if (toggle) toggle.checked = false;
        const form = document.querySelector('form.profile-form');
        if (form) {
            rememberScrollPosition();
            form.submit();
        }
    }

    function initializePinPage() {
        restoreScrollPosition();
        document.querySelectorAll('[data-pin-input]').forEach(function (input) {
            input.addEventListener('input', function () {
                sanitizePinInput(input);
            });
            input.addEventListener('blur', validatePinFields);
        });
        document.querySelectorAll('[data-pin-toggle]').forEach(function (button) {
            button.addEventListener('click', function () {
                const input = document.getElementById(button.dataset.pinToggle);
                if (!input) return;
                input.type = input.type === 'password' ? 'text' : 'password';
                button.classList.toggle('is-visible', input.type === 'text');
                button.setAttribute('aria-label', input.type === 'text' ? 'PINを非表示' : 'PINを表示');
            });
        });
        const toggle = document.getElementById('pinEnabled');
        if (toggle) {
            toggle.addEventListener('change', function () {
                if (!toggle.checked && document.getElementById('currentPinRow')) {
                    toggle.checked = true;
                    togglePinFields(true);
                    openPinUnlockModal();
                    return;
                }
                togglePinFields(toggle.checked);
            });
            togglePinFields(toggle.checked);
        }
        document.querySelectorAll('[data-pin-modal-close]').forEach(function (button) {
            button.addEventListener('click', closePinUnlockModal);
        });
        const confirmUnlock = document.getElementById('confirmPinUnlock');
        if (confirmUnlock) confirmUnlock.addEventListener('click', confirmPinUnlock);
        const forgotPinButton = document.getElementById('forgotPinButton');
        if (forgotPinButton) {
            forgotPinButton.addEventListener('click', function (event) {
                event.preventDefault();
                openAccountPasswordModal();
            });
        }
        document.querySelectorAll('[data-account-password-close]').forEach(function (button) {
            button.addEventListener('click', closeAccountPasswordModal);
        });
        const confirmAccount = document.getElementById('confirmAccountPassword');
        if (confirmAccount) confirmAccount.addEventListener('click', confirmAccountPassword);
        const accountPasswordError = document.getElementById('accountPasswordError');
        if (accountPasswordError && accountPasswordError.textContent.trim()) {
            openAccountPasswordModal();
        }
        const unlockInput = document.getElementById('unlockCurrentPin');
        if (unlockInput) {
            unlockInput.addEventListener('input', function () {
                unlockInput.value = unlockInput.value.replace(/\D/g, '').slice(0, 4);
            });
            unlockInput.addEventListener('keydown', function (event) {
                if (event.key === 'Enter') confirmPinUnlock();
            });
        }
        const success = document.getElementById('pinSaveSuccess');
        if (success) {
            window.setTimeout(function () {
                success.hidden = true;
            }, 5000);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initializePinPage, {once: true});
    } else {
        initializePinPage();
    }

    window.beginPinChange = beginPinChange;
    window.cancelPinChange = cancelPinChange;
    window.submitPinSetup = submitPinSetup;
})();
