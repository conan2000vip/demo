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

  /* ---------- 4 ô nhập PIN trong modal mở khóa ---------- */

  function getUnlockDigits() {
    return Array.from(document.querySelectorAll('#unlockPinDigits .pin-digit-input'));
  }

  function syncUnlockPin() {
    const hidden = document.getElementById('unlockCurrentPin');
    if (hidden) {
      hidden.value = getUnlockDigits().map(function (d) { return d.value; }).join('');
    }
  }

  function resetUnlockDigits() {
    getUnlockDigits().forEach(function (d) { d.value = ''; });
    syncUnlockPin();
  }

  function initUnlockDigits() {
    const digits = getUnlockDigits();
    digits.forEach(function (input, i) {
      input.addEventListener('input', function () {
        input.value = input.value.replace(/\D/g, '').slice(0, 1);
        syncUnlockPin();
        const error = document.getElementById('unlockPinError');
        if (error) error.textContent = '';
        if (input.value && i < digits.length - 1) digits[i + 1].focus();
      });
      input.addEventListener('keydown', function (event) {
        if (event.key === 'Backspace' && !input.value && i > 0) {
          digits[i - 1].value = '';
          digits[i - 1].focus();
          syncUnlockPin();
        }
        if (event.key === 'Enter') {
          event.preventDefault();
          confirmPinUnlock();
        }
      });
      input.addEventListener('paste', function (event) {
        const text = (event.clipboardData || window.clipboardData)
          .getData('text').replace(/\D/g, '').slice(0, 4);
        if (!text) return;
        event.preventDefault();
        digits.forEach(function (d, j) { d.value = text[j] || ''; });
        syncUnlockPin();
        digits[Math.min(text.length, digits.length - 1)].focus();
      });
    });
  }

  function openPinUnlockModal() {
    const modal = document.getElementById('pinUnlockModal');
    const error = document.getElementById('unlockPinError');
    if (!modal) return;
    resetUnlockDigits();
    if (error) error.textContent = '';
    modal.hidden = false;
    const first = getUnlockDigits()[0];
    if (first) first.focus();
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

  function backToPinUnlockModal() {
    const modal = document.getElementById('accountPasswordModal');
    const input = document.getElementById('accountPasswordInput');
    const error = document.getElementById('accountPasswordError');
    if (input) input.value = '';
    if (error) error.textContent = '';
    if (modal) modal.hidden = true;
    openPinUnlockModal();
  }

  async function submitPinRemoval(form, errorId, onFail) {
    const data = new FormData(form);
    const toggle = document.getElementById('pinEnabled');
    if (toggle) data.set(toggle.name, 'false');   // server vẫn nhận pinEnabled=false, nút gạt trên màn hình giữ nguyên

    try {
      const response = await fetch(form.action, {
        method: 'POST',
        body: new URLSearchParams(data),
        credentials: 'same-origin'
      });
      const html = await response.text();
      const doc = new DOMParser().parseFromString(html, 'text/html');
      const serverError = doc.getElementById(errorId);
      const message = serverError ? serverError.textContent.trim() : '';

      if (message) {
        onFail(message);   // sai: hiện lỗi trong modal, không tải lại trang
        return;
      }
      rememberScrollPosition();
      window.location.href = response.url;   // đúng: chuyển trang như cũ
    } catch (error) {
      if (toggle) toggle.checked = false;    // lỗi mạng: quay về cách gửi cũ
      rememberScrollPosition();
      form.submit();
    }
  }

  function clearPinRemovalFields() {
    ['pinAction', 'currentPin', 'accountPassword'].forEach(function (id) {
      const el = document.getElementById(id);
      if (el) el.value = '';
    });
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
    document.getElementById('accountPassword').value = password;
    document.getElementById('pinAction').value = 'REMOVE_ACCOUNT_PASSWORD';
    const form = document.querySelector('form.profile-form');
    if (!form) return;
    submitPinRemoval(form, 'accountPasswordError', function (message) {
      clearPinRemovalFields();
      if (error) error.textContent = message;
      if (input) { input.value = ''; input.focus(); }
    });
  }

  function confirmPinUnlock() {
    const input = document.getElementById('unlockCurrentPin');
    const error = document.getElementById('unlockPinError');
    if (!input) return;
    syncUnlockPin();
    const pin = input.value.replace(/\D/g, '').slice(0, 4);
    if (!/^\d{4}$/.test(pin)) {
      if (error) error.textContent = '現在の暗証番号は4桁の数字で入力してください。';
      const firstEmpty = getUnlockDigits().find(function (d) { return !d.value; });
      if (firstEmpty) firstEmpty.focus();
      return;
    }
    document.getElementById('currentPin').value = pin;
    document.getElementById('pinAction').value = 'REMOVE';
    const form = document.querySelector('form.profile-form');
    if (!form) return;
    submitPinRemoval(form, 'unlockPinError', function (message) {
      clearPinRemovalFields();
      if (error) error.textContent = message;
      resetUnlockDigits();
      const first = getUnlockDigits()[0];
      if (first) first.focus();
    });
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
    document.querySelectorAll('[data-account-password-back]').forEach(function (button) {
      button.addEventListener('click', backToPinUnlockModal);
    });
    const confirmAccount = document.getElementById('confirmAccountPassword');
    if (confirmAccount) confirmAccount.addEventListener('click', confirmAccountPassword);

    const accountPasswordInput = document.getElementById('accountPasswordInput');
    if (accountPasswordInput) {
      accountPasswordInput.addEventListener('input', function () {
        const error = document.getElementById('accountPasswordError');
        if (error) error.textContent = '';
      });
      accountPasswordInput.addEventListener('keydown', function (event) {
        if (event.key === 'Enter') {
          event.preventDefault();
          confirmAccountPassword();
        }
      });
    }
    const accountPasswordError = document.getElementById('accountPasswordError');
    if (accountPasswordError && accountPasswordError.textContent.trim()) {
      openAccountPasswordModal();
    } else {
      const unlockModal = document.getElementById('pinUnlockModal');
      if (unlockModal && !unlockModal.hidden) {
        const first = getUnlockDigits()[0];
        if (first) first.focus();
      }
    }
    initUnlockDigits();
    const success = document.getElementById('pinSaveSuccess');
    if (success) {
      window.setTimeout(function () {
        success.hidden = true;
      }, 3000);
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializePinPage, { once: true });
  } else {
    initializePinPage();
  }
  window.beginPinChange = beginPinChange;
  window.cancelPinChange = cancelPinChange;
  window.submitPinSetup = submitPinSetup;
})();