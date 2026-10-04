// 共通画面処理（common.js）
document.addEventListener('DOMContentLoaded', () => {
    // Lucideアイコンを画面に描画する。
    if (window.lucide) {
        lucide.createIcons();
    }

    initUserMenu();
    initMobileNavigation();
    initChartRangePicker();
    initAlertAutoDismiss();
    initFilterForm();
    initMemoExpand();

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
    initDeleteConfirm();
    initAjaxPagination();
});

function initAlertAutoDismiss() {
    document.querySelectorAll('.alert').forEach((alert) => {
        window.setTimeout(() => alert.remove(), 10000);
    });
}

function initUserMenu() {
    const toggle = document.getElementById('userMenuToggle');
    const dropdown = document.getElementById('userMenuDropdown');
    if (!toggle || !dropdown) return;

    function closeMenu() {
        dropdown.classList.remove('is-open');
        toggle.setAttribute('aria-expanded', 'false');
    }

    toggle.addEventListener('click', () => {
        const isOpen = dropdown.classList.toggle('is-open');
        toggle.setAttribute('aria-expanded', String(isOpen));
    });

    document.addEventListener('click', (event) => {
        if (!toggle.contains(event.target) && !dropdown.contains(event.target)) {
            closeMenu();
        }
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') closeMenu();
    });
}

function initMobileNavigation() {
    const toggle = document.getElementById('menuToggle');
    const navigation = document.getElementById('appNavigation');
    if (!toggle || !navigation) return;

    function closeNavigation() {
        navigation.classList.remove('open');
        toggle.setAttribute('aria-expanded', 'false');
    }

    toggle.addEventListener('click', () => {
        const isOpen = navigation.classList.toggle('open');
        toggle.setAttribute('aria-expanded', String(isOpen));
    });

    navigation.querySelectorAll('a').forEach((link) => {
        link.addEventListener('click', closeNavigation);
    });

    document.addEventListener('click', (event) => {
        if (!toggle.contains(event.target) && !navigation.contains(event.target)) {
            closeNavigation();
        }
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') closeNavigation();
    });
}

function initChartRangePicker() {
    const form = document.getElementById('chartRangePopover');
    const toggle = document.getElementById('chartRangeToggle');
    const label = document.getElementById('chartRangeLabel');
    const startInput = document.getElementById('chartRangeStartDate');
    const endInput = document.getElementById('chartRangeEndDate');
    const quickRangeBar = document.getElementById('chartRangeQuickBar');
    const error = document.getElementById('chartRangeError');
    if (!form || !toggle || !label || !startInput || !endInput) return;

    function toIsoDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    function getRange(range) {
        const end = new Date();
        end.setHours(0, 0, 0, 0);
        const start = new Date(end);
        if (range === '1W') start.setDate(start.getDate() - 6);
        if (range === '30D') start.setDate(start.getDate() - 29);
        if (range === '6M' || range === '1Y') {
            const months = range === '6M' ? 6 : 12;
            const originalDay = start.getDate();
            start.setDate(1);
            start.setMonth(start.getMonth() - months);
            start.setDate(Math.min(originalDay, new Date(start.getFullYear(), start.getMonth() + 1, 0).getDate()));
        }
        return { start: toIsoDate(start), end: toIsoDate(end) };
    }

    function formatDate(value, includeYear = true) {
        if (!value) return '';
        const [year, month, day] = value.split('-');
        return `${includeYear ? `${year}/` : ''}${month}/${day}`;
    }

    function updateLabel() {
        if (!startInput.value || !endInput.value) {
            const defaultRange = getRange('1W');
            label.textContent = `${formatDate(defaultRange.start)} – ${formatDate(defaultRange.end)}`;
        } else {
            const shortDates = window.matchMedia('(max-width: 420px)').matches;
            const sameYear = startInput.value.slice(0, 4) === endInput.value.slice(0, 4);
            const includeYear = !shortDates || !sameYear;
            label.textContent = `${formatDate(startInput.value, includeYear)} – ${formatDate(endInput.value, includeYear)}`;
        }
    }

    function syncQuickRange() {
        const selectedStart = startInput.value || getRange('1W').start;
        const selectedEnd = endInput.value || getRange('1W').end;
        quickRangeBar?.querySelectorAll('[data-range]').forEach((button) => {
            const range = getRange(button.dataset.range);
            button.classList.toggle('is-active', selectedStart === range.start && selectedEnd === range.end);
        });
    }

    function closePopover() {
        form.hidden = true;
        toggle.setAttribute('aria-expanded', 'false');
    }

    function positionPopover() {
        if (form.hidden) return;
        const anchor = toggle.getBoundingClientRect();
        const popover = form.getBoundingClientRect();
        const margin = 16;
        const left = Math.max(margin, Math.min(anchor.right - popover.width, window.innerWidth - popover.width - margin));
        let top = anchor.bottom + 8;
        if (top + popover.height > window.innerHeight - margin) {
            top = anchor.top - popover.height - 8;
        }
        top = Math.max(margin, Math.min(top, window.innerHeight - popover.height - margin));
        form.style.left = `${left}px`;
        form.style.top = `${top}px`;
    }

    function updateRangeDisplay() {
        updateLabel();
        syncQuickRange();
        if (error) error.hidden = true;
    }

    if (!startInput.value && !endInput.value) {
        const defaultRange = getRange('1W');
        startInput.value = defaultRange.start;
        endInput.value = defaultRange.end;
    }

    toggle.addEventListener('click', () => {
        const isOpen = form.hidden;
        form.hidden = !isOpen;
        toggle.setAttribute('aria-expanded', String(isOpen));
        if (isOpen) requestAnimationFrame(positionPopover);
    });

    quickRangeBar?.querySelectorAll('[data-range]').forEach((button) => {
        button.addEventListener('click', () => {
            const range = getRange(button.dataset.range);
            startInput.value = range.start;
            endInput.value = range.end;
            form.requestSubmit();
        });
    });

    [startInput, endInput].forEach((input) => {
        input.addEventListener('input', updateRangeDisplay);
        input.addEventListener('change', updateRangeDisplay);
    });

    form.addEventListener('submit', (event) => {
        if (startInput.value && endInput.value && startInput.value > endInput.value) {
            event.preventDefault();
            error.textContent = '終了日は開始日以降の日付を指定してください。';
            error.hidden = false;
        }
    });

    document.addEventListener('click', (event) => {
        if (!form.hidden && !form.contains(event.target) && !toggle.contains(event.target)) closePopover();
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && !form.hidden) {
            closePopover();
            toggle.focus();
        }
    });

    updateRangeDisplay();
    window.addEventListener('resize', updateLabel);
    window.addEventListener('resize', positionPopover);
    window.addEventListener('scroll', positionPopover, true);
}

function initFilterForm() {
    const form = document.getElementById('filterForm');
    const quickRangeBar = document.getElementById('quickRangeBar');
    const startInput = document.getElementById('startDateInput');
    const endInput = document.getElementById('endDateInput');
    if (!form || !startInput || !endInput || form.dataset.filterInitialized === 'true') return;
    form.dataset.filterInitialized = 'true';

    function toIsoDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    function subtractMonths(date, count) {
        const target = new Date(date);
        const originalDay = target.getDate();
        target.setDate(1);
        target.setMonth(target.getMonth() - count);
        const daysInTargetMonth = new Date(target.getFullYear(), target.getMonth() + 1, 0).getDate();
        target.setDate(Math.min(originalDay, daysInTargetMonth));
        return target;
    }

    function getRange(range) {
        const end = new Date();
        end.setHours(0, 0, 0, 0);
        const start = new Date(end);
        if (range === '1W') start.setDate(start.getDate() - 6);
        if (range === '1M') start.setTime(subtractMonths(end, 1).getTime());
        if (range === '6M') start.setTime(subtractMonths(end, 6).getTime());
        if (range === '1Y') start.setTime(subtractMonths(end, 12).getTime());
        if (range === '1M' || range === '6M' || range === '1Y') start.setDate(start.getDate() + 1);
        return { start: toIsoDate(start), end: toIsoDate(end) };
    }

    function syncActiveRange() {
        if (!quickRangeBar) return;
        quickRangeBar.querySelectorAll('[data-range]').forEach(button => {
            const range = getRange(button.dataset.range);
            button.classList.toggle(
                'is-active',
                startInput.value === range.start && endInput.value === range.end
            );
        });
    }

    quickRangeBar?.querySelectorAll('[data-range]').forEach(button => {
        button.addEventListener('click', () => {
            const range = getRange(button.dataset.range);
            startInput.value = range.start;
            endInput.value = range.end;
            form.requestSubmit();
        });
    });

    function removeFilterError() {
        document.getElementById('filterErrorAlert')?.remove();
    }

    function showFilterError(message) {
        removeFilterError();
        const alert = document.createElement('div');
        alert.className = 'alert alert--error';
        alert.id = 'filterErrorAlert';
        alert.innerHTML =
            '<i data-lucide="alert-circle"></i><span></span>' +
            '<button type="button" class="alert__close"><i data-lucide="x"></i></button>';
        alert.querySelector('span').textContent = message;
        alert.querySelector('.alert__close').addEventListener('click', () => alert.remove());
        form.parentNode.insertBefore(alert, form);
        if (window.lucide) lucide.createIcons();
        window.setTimeout(() => alert.remove(), 10000);
    }

    form.addEventListener('submit', event => {
        if (startInput.value && endInput.value && startInput.value > endInput.value) {
            event.preventDefault();
            showFilterError('終了日は開始日以降の日付を指定してください。');
            return;
        }
        removeFilterError();
    });

    const onDateChange = () => {
        removeFilterError();
        syncActiveRange();
    };
    startInput.addEventListener('input', onDateChange);
    startInput.addEventListener('change', onDateChange);
    endInput.addEventListener('input', onDateChange);
    endInput.addEventListener('change', onDateChange);
    syncActiveRange();
}

// Chống load lại trang 
function initAjaxPagination() {
    const card = document.querySelector('.table-card');
    if (!card) return;

    async function loadPage(url, push) {
        let response;
        try {
            response = await fetch(url, { headers: { Accept: 'text/html' } });
        } catch {
            window.location.href = url;
            return;
        }
        if (!response.ok) { window.location.href = url; return; }

        const doc = new DOMParser().parseFromString(await response.text(), 'text/html');
        const newCard = doc.querySelector('.table-card');
        if (!newCard) { window.location.href = url; return; }

        card.innerHTML = newCard.innerHTML;
        if (push) history.pushState(null, '', url);
        if (window.lucide) lucide.createIcons();
        document.dispatchEvent(new CustomEvent('table:updated'));
    }

    card.addEventListener('click', (event) => {
        const link = event.target.closest('.pagination a');
        if (!link) return;
        event.preventDefault();
        loadPage(link.href, true);
    });

    window.addEventListener('popstate', () => loadPage(window.location.href, false));
}

function initDeleteConfirm() {
    const overlay = document.getElementById('deleteModalOverlay');
    if (!overlay) return;

    const closeBtn = document.getElementById('closeDeleteModalBtn');
    const cancelBtn = document.getElementById('cancelDeleteBtn');
    const confirmBtn = document.getElementById('confirmDeleteBtn');
    const messageEl = overlay.querySelector('.modal__message');

    let pendingForm = null;

    function openDeleteModal(form, customMessage) {
        pendingForm = form;
        if (messageEl && customMessage) {
            messageEl.textContent = customMessage;
        }
        overlay.classList.add('is-open');
    }

    function closeDeleteModal() {
        overlay.classList.remove('is-open');
        pendingForm = null;
    }

    document.addEventListener('submit', (event) => {
        const form = event.target;
        if (!form.classList || !form.classList.contains('delete-form')) return;
        event.preventDefault();
        openDeleteModal(form, form.dataset.confirmMessage || null);
    });

    confirmBtn?.addEventListener('click', () => {
        if (pendingForm) pendingForm.submit();
        closeDeleteModal();
    });

    closeBtn?.addEventListener('click', closeDeleteModal);
    cancelBtn?.addEventListener('click', closeDeleteModal);

    overlay.addEventListener('click', (event) => {
        if (event.target === overlay) closeDeleteModal();
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && overlay.classList.contains('is-open')) {
            closeDeleteModal();
        }
    });
}

window.HealthChart = (() => {
    const defaultDays = 7;

    function create({
        canvasId,
        data,
        unit = '',
        type = 'bar',
        days = defaultDays,
        isSearching = false,
        targetValue = null,
        color = null,
        showDataLabels = false,
        labelColor = '#1e293b',
        valueFormatter = (v) => String(v)
    }) {
        const canvas = document.getElementById(canvasId);
        if (!canvas || typeof Chart === 'undefined' || !data?.labels?.length) return null;

        let labels;
        let values;
        if (!isSearching) {
            const dateRange = buildLastNDaysRange(days);
            const valueMap = Object.fromEntries(data.labels.map((label, index) => [label, data.values[index]]));
            labels = dateRange.map(date => formatLabel(date, 'DAY'));
            values = dateRange.map(date => {
                const value = valueMap[date];
                return value == null ? null : Number(value);
            });
        } else {
            labels = data.labels.map(date => formatLabel(date, data.chartMode));
            values = data.values.map(value => value == null ? null : Number(value));
        }

        const styles = getComputedStyle(document.documentElement);
        const primary = color || styles.getPropertyValue('--chart-primary').trim() || '#e11d48';
        const isLine = type === 'line';
        const validValues = values.filter(value => value !== null && Number.isFinite(value));
        let yMin;
        let yMax;

        if (validValues.length > 0) {
            const allValues = targetValue != null && targetValue !== ''
                ? [...validValues, Number(targetValue)]
                : validValues;
            const minValue = Math.min(...allValues);
            const maxValue = Math.max(...allValues);
            const difference = maxValue - minValue;
            const margin = difference < 1 ? 0.8 : difference <= 3 ? 1.5 : 2.5;
            yMin = Math.floor(minValue - margin);
            yMax = Math.ceil(maxValue + margin);
        }

        let isHovering = false;
        const datasets = [{
            data: values,
            borderColor: primary,
            borderWidth: isLine ? 2.5 : 1,
            spanGaps: true,
            ...(isLine ? {
                tension: 0,
                fill: false,
                pointRadius: 5,
                pointHoverRadius: 7,
                pointBackgroundColor: primary,
                pointBorderColor: '#ffffff',
                pointBorderWidth: 2
            } : {
                backgroundColor: hexToRgba(primary, 0.75),
                borderRadius: 8,
                maxBarThickness: 40
            })
        }];

        return new Chart(canvas, {
            type,
            data: { labels, datasets },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                layout: { padding: { left: 10, right: 15, top: 5, bottom: 5 } },
                plugins: {
                    legend: { display: false },
                    tooltip: { enabled: false }
                },
                scales: {
                    x: {
                        grid: { display: true, color: '#f1f5f9', drawTicks: false },
                        border: { display: true, color: '#94a3b8', width: 1.5 },
                        offset: true,
                        ticks: {
                            padding: 8,
                            color: '#475569',
                            font: { weight: 'bold' },
                            autoSkip: true,
                            maxTicksLimit: data.chartMode === 'HOUR' ? 8 : undefined,
                            minRotation: 0,
                            maxRotation: 0
                        }
                    },
                    y: {
                        grid: { display: true, color: '#f1f5f9', drawTicks: false },
                        border: { display: true, color: '#94a3b8', width: 1.5 },
                        beginAtZero: false,
                        suggestedMin: yMin,
                        suggestedMax: yMax,
                        ticks: { padding: 10, color: '#475569', font: { weight: 'bold' } }
                    }
                }
            },
            plugins: (isLine || showDataLabels) ? [{
                id: 'customLabelsAndTargetPlugin',
                afterDatasetsDraw(chart) {
                    const { ctx, chartArea, scales: { y } } = chart;
                    if (targetValue != null && targetValue !== '') {
                        const targetNumber = Number(targetValue);
                        const yPosition = y.getPixelForValue(targetNumber);
                        if (yPosition >= chartArea.top && yPosition <= chartArea.bottom) {
                            ctx.save();
                            ctx.globalAlpha = isHovering ? 1 : 0.35;
                            ctx.beginPath();
                            ctx.setLineDash([6, 4]);
                            ctx.strokeStyle = '#0284c7';
                            ctx.lineWidth = 1.5;
                            ctx.moveTo(chartArea.left, yPosition);
                            ctx.lineTo(chartArea.right, yPosition);
                            ctx.stroke();
                            ctx.restore();
                        }
                    }

                    if (!showDataLabels) return;
                    const metadata = chart.getDatasetMeta(0);
                    metadata.data.forEach((element, index) => {
                        const value = chart.data.datasets[0].data[index];
                        if (value == null) return;
                        ctx.save();
                        ctx.fillStyle = '#1e293b';
                        ctx.font = 'bold 12px sans-serif';
                        ctx.textAlign = 'center';
                        ctx.textBaseline = 'bottom';
                        ctx.fillText(valueFormatter(value), element.x, element.y + (isLine ? -7 : -6));
                        ctx.restore();
                    });
                }
            }] : []
        });
    }

    function buildLastNDaysRange(count) {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const result = [];
        for (let offset = count - 1; offset >= 0; offset--) {
            const date = new Date(today);
            date.setDate(date.getDate() - offset);
            result.push(toIsoDate(date));
        }
        return result;
    }

    function toIsoDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    function formatLabel(value, mode) {
        if (!value) return '';
        if (mode === 'DAY') {
            const date = new Date(`${value}T00:00:00`);
            const weekdays = ['日', '月', '火', '水', '木', '金', '土'];
            return `${date.getDate()}(${weekdays[date.getDay()]})`;
        }
        if (mode === 'WEEK') {
            const start = new Date(`${value}T00:00:00`);
            const end = new Date(start);
            end.setDate(start.getDate() + 6);
            const today = new Date();
            const finalEnd = end > today ? today : end;
            return `${start.getMonth() + 1}/${start.getDate()}~${finalEnd.getMonth() + 1}/${finalEnd.getDate()}`;
        }
        return value;
    }

    function hexToRgba(hex, alpha) {
        const clean = hex.replace('#', '');
        const value = Number.parseInt(clean, 16);
        const red = (value >> 16) & 255;
        const green = (value >> 8) & 255;
        const blue = value & 255;
        return `rgba(${red}, ${green}, ${blue}, ${alpha})`;
    }

    return { create, formatLabel };
})();

function initChartSwipe({ chart, wrapperEl, chartUrl, initialFrom, initialTo, onRangeUpdate }) {
    if (!chart || !wrapperEl || !chartUrl || !initialFrom || !initialTo) return;

    let currentFrom = new Date(`${initialFrom}T00:00:00`);
    let currentTo = new Date(`${initialTo}T00:00:00`);

    function toIso(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    async function loadRange(from, to) {
        const separator = chartUrl.includes('?') ? '&' : '?';
        const url = `${chartUrl}${separator}startDate=${toIso(from)}&endDate=${toIso(to)}`;
        let response;
        try {
            response = await fetch(url, { headers: { Accept: 'application/json' } });
        } catch {
            return;
        }
        if (!response.ok) return;
        const data = await response.json();
        currentFrom = from;
        currentTo = to;
        chart.data.labels = data.labels.map(label => HealthChart.formatLabel(label, data.chartMode));
        chart.data.datasets[0].data = data.values.map(value => value == null ? null : Number(value));
        chart.update();
        onRangeUpdate?.(data);
    }

    let touchStartX = null;
    wrapperEl.addEventListener('touchstart', event => {
        touchStartX = event.touches[0].clientX;
    }, { passive: true });

    wrapperEl.addEventListener('touchend', event => {
        if (touchStartX === null) return;
        const deltaX = event.changedTouches[0].clientX - touchStartX;
        touchStartX = null;
        if (Math.abs(deltaX) < 40) return;

        const spanDays = Math.round((currentTo - currentFrom) / 86400000) + 1;
        const from = new Date(currentFrom);
        const to = new Date(currentTo);
        if (deltaX < 0) {
            from.setDate(from.getDate() + spanDays);
            to.setDate(to.getDate() + spanDays);
            if (from > new Date()) return;
        } else {
            from.setDate(from.getDate() - spanDays);
            to.setDate(to.getDate() - spanDays);
        }
        loadRange(from, to);
    }, { passive: true });
}

// mở rộng ô memo
function initMemoExpand() {
    function setExpanded(cell, expanded) {
        cell.classList.toggle('is-expanded', expanded);
        cell.setAttribute('aria-expanded', String(expanded));
    }

    function toggleCell(cell) {
        const willExpand = !cell.classList.contains('is-expanded');
        document.querySelectorAll('.memo-cell.is-expanded').forEach((expandedCell) => {
            if (expandedCell !== cell) setExpanded(expandedCell, false);
        });
        setExpanded(cell, willExpand);
    }

    document.querySelectorAll('.memo-cell').forEach((cell) => {
        if (!cell.textContent.trim()) return;
        cell.tabIndex = 0;
        cell.setAttribute('aria-expanded', 'false');
    });

    document.addEventListener('click', (event) => {
        const target = event.target;
        const cell = target instanceof Element ? target.closest('.memo-cell') : null;
        if (!cell) {
            document.querySelectorAll('.memo-cell.is-expanded').forEach((c) => {
                setExpanded(c, false);
            });
            return;
        }

        if (!cell.textContent.trim()) return;
        toggleCell(cell);
    });

    document.addEventListener('keydown', (event) => {
        if (event.key !== 'Enter' && event.key !== ' ') return;
        const target = event.target;
        const cell = target instanceof Element ? target.closest('.memo-cell') : null;
        if (!cell || !cell.textContent.trim()) return;
        event.preventDefault();
        toggleCell(cell);
    });
}
