let currentActiveProfileId = null;
let currentShareData = [];

// Mở Modal và tải dữ liệu từ RestController
function openShareModal(profileId, currentProfileId, isCurrentPrimary) {
    if (!isCurrentPrimary && profileId !== currentProfileId) {
        showShareAlert('このプロファイルの共有設定を変更する権限がありません。自分のプロファイルを選択してください。');
        return;
    }

    currentActiveProfileId = profileId;
    const modal = document.getElementById('shareSettingsModal');

    fetch(`/profile/${profileId}/share-settings`, {
        method: 'GET',
        headers: { 'Accept': 'application/json' }
    })
        .then(response => {
            if (!response.ok) return response.json().then(error => { throw new Error(error.message); });
            return response.json();
        })
        .then(data => {
            currentShareData = data;
            renderShareMatrixTable(data);
            modal.scrollTop = 0;
            modal.classList.add('is-open');
            const dialog = modal.querySelector('.modal');
            dialog.scrollTop = 0;
            modal.querySelector('.modal__close')?.focus({ preventScroll: true });
            dialog.scrollTop = 0;
            document.documentElement.classList.add('share-settings-open');
            document.body.classList.add('share-settings-open');
        })
        .catch(err => showShareAlert(err.message));
}

    function showShareAlert(message, type = 'error') {
        const main = document.querySelector('.profile-manage');
        if (!main) return;

        main.querySelector('.share-inline-alert')?.remove();

        const alert = document.createElement('div');
        alert.className = `alert alert--${type} share-inline-alert`;
        alert.setAttribute('role', type === 'error' ? 'alert' : 'status');
        alert.setAttribute('aria-live', type === 'error' ? 'assertive' : 'polite');

        const messageElement = document.createElement('span');
        messageElement.textContent = message;

        const closeButton = document.createElement('button');
        closeButton.type = 'button';
        closeButton.className = 'alert__close';
        closeButton.setAttribute('aria-label', '閉じる');
        closeButton.textContent = '×';
        closeButton.addEventListener('click', () => alert.remove());

        alert.append(messageElement, closeButton);
        main.prepend(alert);
        window.setTimeout(() => alert.remove(), 10000);
    }

// Render ma trận phân quyền 5 hạng mục
function renderShareMatrixTable(data) {
    const list = document.getElementById('shareMatrixBody');
    list.innerHTML = '';

    const permissionFields = [
        ['weightRole', '体重'],
        ['sleepRole', '睡眠'],
        ['waterRole', '水分'],
        ['stepRole', '歩数'],
        ['memoRole', 'メモ']
    ];

    data.forEach((item, index) => {
        const isDisabled = item.self;

        const member = document.createElement('section');
        member.className = 'share-settings__member';
        member.innerHTML = `
            <div class="share-settings__member-header">
                <div class="share-settings__member-info">
                    <strong class="share-settings__member-name">${escapeHtml(item.targetProfileName)}</strong>
                    <span class="share-settings__relationship">${escapeHtml(item.relationship)}</span>
                </div>
                ${item.self ? '<span class="badge badge--selected share-settings__active-badge">操作中</span>' : ''}
            </div>
            <div class="share-settings__permissions">
                ${permissionFields.map(([fieldName, label]) => createRoleSelectCell(
                    index, fieldName, item.roles[fieldName], isDisabled, label
                )).join('')}
            </div>
        `;
        list.appendChild(member);
    });
}

function createRoleSelectCell(index, fieldName, currentRole, isDisabled, label) {
    const selectId = `share-role-${index}-${fieldName}`;
    return `
        <div class="share-settings__permission">
            <label for="${selectId}">${label}</label>
            <select id="${selectId}" class="share-settings__select" ${isDisabled ? 'disabled' : ''}
                    onchange="onRoleChange(${index}, '${fieldName}', this.value)">
                <option value="EDITOR" ${currentRole === 'EDITOR' ? 'selected' : ''}>編集者</option>
                <option value="VIEWER" ${currentRole === 'VIEWER' ? 'selected' : ''}>閲覧者</option>
                <option value="NONE" ${currentRole === 'NONE' ? 'selected' : ''}>閲覧不可</option>
            </select>
        </div>
    `;
}

function onRoleChange(index, fieldName, value) {
    if (currentShareData[index] && currentShareData[index].roles) {
        currentShareData[index].roles[fieldName] = value;
    }
}

// Gọi API POST lưu cấu hình
function saveShareSettings() {
    if (!currentActiveProfileId) return;

    const btnSave = document.getElementById('btnSaveShareSettings');
    btnSave.disabled = true;

    fetch(`/profile/${currentActiveProfileId}/share-settings`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(currentShareData)
    })
        .then(response => {
            if (!response.ok) return response.json().then(error => { throw new Error(error.message); });
            const activeProfileName = currentShareData.find(item => item.self)?.targetProfileName;
            showShareAlert(`${activeProfileName} の共有設定を保存しました。`, 'success');
            closeShareModal();
        })
        .catch(err => showShareAlert(err.message))
        .finally(() => btnSave.disabled = false);
}

function closeShareModal() {
    const modal = document.getElementById('shareSettingsModal');
    if (modal) {
        modal.classList.remove('is-open');
    }
    document.documentElement.classList.remove('share-settings-open');
    document.body.classList.remove('share-settings-open');
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"']/g, function (m) {
        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[m];
    });
}