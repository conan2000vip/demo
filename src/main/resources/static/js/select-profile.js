(function () {
    'use strict';

    const pinModal = document.getElementById('selectPinModal');
    const pinView = document.getElementById('selectPinView');
    const passwordView = document.getElementById('selectPasswordView');
    const passwordForm = document.getElementById('selectAccountPasswordForm');
    const passwordInput = document.getElementById('selectAccountPassword');
    const confirmPasswordButton = document.getElementById('selectConfirmAccountPassword');
    const pinInput = document.getElementById('selectPinInput');
    const pinDigits = Array.from(document.querySelectorAll('.pin-digit-input'));
    const pinError = document.getElementById('selectPinError');
    const passwordError = document.getElementById('selectAccountPasswordError');
    const profileId = pinModal?.dataset.profileId;
    let isSubmittingPin = false;

    function resetPinInputs() {
        resetPinModalDigits(pinDigits, pinInput);
    }

    function clearPasswordInput() {
        if (passwordInput) passwordInput.value = '';
    }

    function showPinModal() {
        if (!pinModal) return;
        pinModal.hidden = false;
        if (pinView) pinView.hidden = false;
        if (passwordView) passwordView.hidden = true;
        pinDigits[0]?.focus();
    }

    function closePinModal() {
        if (pinModal) pinModal.hidden = true;
        resetPinInputs();
        clearPasswordInput();
        if (pinError) pinError.textContent = '';
        if (passwordError) passwordError.textContent = '';
        isSubmittingPin = false;
    }

    pinModal?.querySelectorAll('[data-select-pin-close]').forEach((element) => {
        element.addEventListener('click', closePinModal);
    });

    function showPasswordModal() {
        resetPinInputs();
        if (pinError) pinError.textContent = '';
        if (pinView) pinView.hidden = true;
        if (passwordView) passwordView.hidden = false;
        passwordInput?.focus();
    }

    function backToPinModal() {
        resetPinInputs();
        clearPasswordInput();
        if (pinError) pinError.textContent = '';
        if (passwordError) passwordError.textContent = '';
        showPinModal();
    }

    document.querySelectorAll('.profile-card[data-profile-id]').forEach((card) => {
        card.addEventListener('click', (event) => {
            if (card.dataset.profileId !== profileId) return;
            event.preventDefault();
            showPinModal();
        });
    });

    document.getElementById('selectForgotPin')?.addEventListener('click', (event) => {
        event.preventDefault();
        showPasswordModal();
    });

    document.querySelectorAll('[data-account-password-close]').forEach((button) => {
        button.addEventListener('click', () => {
            backToPinModal();
        });
    });

    const submitAccountPassword = () => {
        const password = passwordInput?.value || '';
        if (!password) {
            if (passwordError) passwordError.textContent = 'アカウントのパスワードを入力してください。';
            passwordInput?.focus();
            return;
        }
        if (passwordInput) passwordInput.value = password;
        passwordForm?.submit();
    };

    confirmPasswordButton?.addEventListener('click', submitAccountPassword);
    passwordInput?.addEventListener('input', () => {
        if (passwordError) passwordError.textContent = '';
    });
    passwordInput?.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            submitAccountPassword();
        }
    });

    if (pinInput) {
        const pinForm = pinInput.form;
        const validatePin = () => {
            pinInput.value = pinDigits.map((digit) => digit.value).join('');
            if (pinInput.value.length !== 4) {
                if (pinError) pinError.textContent = '4桁のPINを入力してください。';
                pinDigits.find((digit) => !digit.value)?.focus();
                return false;
            }
            return true;
        };
        const submitPin = () => {
            if (!validatePin() || isSubmittingPin) return;
            isSubmittingPin = true;
            pinForm?.requestSubmit();
        };
        pinDigits.forEach((input, index) => {
            input.addEventListener('input', () => {
                input.value = input.value.replace(/\D/g, '').slice(0, 1);
                pinInput.value = pinDigits.map((digit) => digit.value).join('');
                if (pinError) pinError.textContent = '';
                if (input.value && pinDigits[index + 1]) {
                    pinDigits[index + 1].focus();
                } else if (index === pinDigits.length - 1 && input.value) {
                    submitPin();
                }
            });
            input.addEventListener('keydown', (event) => {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    submitPin();
                    return;
                }
                if (event.key === 'Backspace' && !input.value && pinDigits[index - 1]) {
                    pinDigits[index - 1].focus();
                }
            });
            input.addEventListener('paste', (event) => {
                event.preventDefault();
                const pasted = (event.clipboardData?.getData('text') || '')
                    .replace(/\D/g, '').slice(0, 4);
                pinDigits.forEach((digit, digitIndex) => {
                    digit.value = pasted[digitIndex] || '';
                });
                pinInput.value = pasted;
                if (pasted.length === 4) {
                    submitPin();
                } else {
                    pinDigits[Math.min(pasted.length, 3)].focus();
                }
            });
        });
        pinForm?.addEventListener('submit', (event) => {
            if (!validatePin()) {
                event.preventDefault();
            }
        });
    }

    if (passwordForm && profileId) {
        passwordForm.action = `/profile/${profileId}/verify-pin/account-password`;
    }

    if (passwordError?.textContent.trim()) {
        showPasswordModal();
    } else if (pinModal && !pinModal.hidden) {
        showPinModal();
    }
})();
