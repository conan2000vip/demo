let currentActiveProfileId = null;
let currentShareData = [];

// Mở Modal và tải dữ liệu từ RestController
function openShareModal(profileId) {
    currentActiveProfileId = profileId;
    const modal = document.getElementById('shareSettingsModal');

    fetch(`/profile/${profileId}/share-settings`, {
        method: 'GET',
        headers: { 'Accept': 'application/json' }
    })
        .then(response => {
            if (!response.ok) throw new Error('共有設定の取得に失敗しました。');
            return response.json();
        })
        .then(data => {
            currentShareData = data;
            renderShareMatrixTable(data);
            modal.classList.add('is-open');
        })
        .catch(err => alert(err.message));
}

// Render ma trận phân quyền 5 hạng mục
function renderShareMatrixTable(data) {
    const tbody = document.getElementById('shareMatrixBody');
    tbody.innerHTML = '';

    const isCurrentPrimary = data.some(item => item.isSelf && item.isPrimary);

    data.forEach((item, index) => {
        const isDisabled = !item.isSelf && !isCurrentPrimary;

        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td style="text-align: left;">
                <strong style="color: #1a1a1a;">${escapeHtml(item.targetProfileName)}</strong>
                <span style="font-size: 12px; color: #888;">(${escapeHtml(item.relationship)})</span>
                ${item.isSelf ? '<span class="badge badge--selected" style="margin-left: 4px;">操作中</span>' : ''}
            </td>
            ${createRoleSelectCell(index, 'weightRole', item.roles.weightRole, isDisabled)}
            ${createRoleSelectCell(index, 'sleepRole', item.roles.sleepRole, isDisabled)}
            ${createRoleSelectCell(index, 'waterRole', item.roles.waterRole, isDisabled)}
            ${createRoleSelectCell(index, 'stepRole', item.roles.stepRole, isDisabled)}
            ${createRoleSelectCell(index, 'memoRole', item.roles.memoRole, isDisabled)}
        `;
        tbody.appendChild(tr);
    });
}

function createRoleSelectCell(index, fieldName, currentRole, isDisabled) {
    return `
        <td>
            <select style="padding: 4px 6px; border-radius: 6px; border: 1px solid #dde2e8; font-size: 12.5px;" 
                    ${isDisabled ? 'disabled' : ''} 
                    onchange="onRoleChange(${index}, '${fieldName}', this.value)">
                <option value="EDITOR" ${currentRole === 'EDITOR' ? 'selected' : ''}>編集者</option>
                <option value="VIEWER" ${currentRole === 'VIEWER' ? 'selected' : ''}>閲覧者</option>
                <option value="NONE" ${currentRole === 'NONE' ? 'selected' : ''}>閲覧不可</option>
            </select>
        </td>
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
            if (!response.ok) throw new Error('共有設定の保存に失敗しました。');
            alert('共有設定を保存しました。');
            closeShareModal();
        })
        .catch(err => alert(err.message))
        .finally(() => btnSave.disabled = false);
}

function closeShareModal() {
    const modal = document.getElementById('shareSettingsModal');
    if (modal) {
        modal.classList.remove('is-open');
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"']/g, function (m) {
        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[m];
    });
}