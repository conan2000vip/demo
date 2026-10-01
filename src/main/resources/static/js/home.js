document.addEventListener("DOMContentLoaded", () => {
    // Initialize Lucide icons
    if (window.lucide) {
        lucide.createIcons();
    }

    // Show all feedback items
    const feedbackToggleBtn = document.getElementById("feedbackToggleBtn");
    if (feedbackToggleBtn) {
        const collapsedText = feedbackToggleBtn.textContent; // "今日のフィードバックをすべて見る（N件）"
        let expanded = false;

        feedbackToggleBtn.addEventListener("click", () => {
            expanded = !expanded;
            document
                .querySelectorAll("#feedbackSection .feedback-card")
                .forEach((card, index) => {
                    if (index >= 3) {
                        card.classList.toggle("is-hidden", !expanded);
                    }
                });

            feedbackToggleBtn.textContent = expanded ? "閉じる" : collapsedText;
        });
    }

    // Initialize clickable stat cards
    initStatCards();

    // Initialize Health Streak Achievement
    initStreakAchievement();
});

function initStatCards() {
    document.querySelectorAll(".stat-card--clickable").forEach((card) => {
        const openCard = () => {
            const href = card.dataset.href;

            if (href) {
                window.location.href = href;
            }
        };

        card.addEventListener("click", openCard);

        card.addEventListener("keydown", (event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                openCard();
            }
        });
    });
}

/* =========================================================
   Health Streak Achievement
   ========================================================= */
const STREAK_MILESTONES = [7, 14, 30, 60, 90, 100, 180, 365, 500, 730, 1000];
function isStreakMilestone(days) {
    if (STREAK_MILESTONES.includes(days)) return true;
    return days > 1000 && days % 365 === 0;
}

function getNextStreakMilestone(days) {
    const next = STREAK_MILESTONES.find((m) => m > days);
    if (next) return next;
    return (Math.floor(days / 365) + 1) * 365;
}

function initStreakAchievement() {
    const streakModal = document.getElementById("streakModal");
    if (!streakModal) {
        return;
    }

    const streakModalButton = document.getElementById("streakModalButton");
    const streakViewButton = document.getElementById("streakViewButton");
    streakModalButton?.addEventListener("click", closeStreakAchievement);
    streakViewButton?.addEventListener("click", () => {
        const streak = Number(
            document.querySelector("#streakViewButton strong")?.textContent || 0
        );
        showCurrentStreakAchievement(streak);
    });
    streakModal
        .querySelector(".streak-modal__overlay")
        ?.addEventListener("click", closeStreakAchievement);

    document.addEventListener("keydown", (event) => {
        if (
            event.key === "Escape" &&
            streakModal.classList.contains("is-visible")
        ) {
            closeStreakAchievement();
        }
    });
    findAndShowStreakFeedback();
}

function getStreakStorageKey(days, occurredAt) {
    // window.currentProfileId は Thymeleaf 側で埋め込む
    const profileId = window.currentProfileId ?? "unknown";
    // occurredAt は "yyyy-MM-ddTHH:mm:ss" 形式想定 → 日付部分のみ抽出
    const dateOnly =
        typeof occurredAt === "string" && occurredAt.length >= 10
            ? occurredAt.slice(0, 10)
            : "unknown-date";
    return `healthStreakModalShown_${profileId}_${days}_${dateOnly}`;
}

/* =========================================================
   Find HEALTH_STREAK feedback
   ========================================================= */
function findAndShowStreakFeedback() {
    if (!Array.isArray(window.homeFeedback)) {
        return;
    }

    const streakFeedback = window.homeFeedback.find(
        (feedback) => feedback.type === "HEALTH_STREAK",
    );
    if (!streakFeedback) {
        return;
    }

    const match = (streakFeedback.title || "").match(/\d+/);
    if (!match) {
        return;
    }

    const days = Number(match[0]);
    const storageKey = getStreakStorageKey(days, streakFeedback.occurredAt);

    try {
        if (localStorage.getItem(storageKey)) {
            return;
        }
    } catch (e) { }

    showCurrentStreakAchievement(days);

    try {
        localStorage.setItem(storageKey, "true");
    } catch (e) { }
}

/* =========================================================
   Dynamic Streak Roadmap Render
   ========================================================= */
const ALL_MILESTONES = [3, 7, 14, 30, 60, 90, 180, 365, 500, 730, 1000];

function renderStreakRoadmap(streak) {
    const container = document.getElementById("streakRoadmap");
    if (!container) return;

    if (streak <= 0) {
        container.style.display = "none";
        return;
    }
    container.style.display = "flex";

    // 1. Tìm mốc mục tiêu tiếp theo (mốc đầu tiên > streak)
    let targetIndex = ALL_MILESTONES.findIndex((m) => m > streak);
    if (targetIndex === -1) targetIndex = ALL_MILESTONES.length - 1;

    // 2. Chọn 4 mốc hiển thị quanh vị trí mục tiêu
    let startIdx = Math.max(0, targetIndex - 1);
    if (startIdx + 4 > ALL_MILESTONES.length) {
        startIdx = Math.max(0, ALL_MILESTONES.length - 4);
    }
    const visibleNodes = ALL_MILESTONES.slice(startIdx, startIdx + 4);

    // 3. Tính % tiến trình thanh ngang
    let fillPercent = 0;
    if (streak >= visibleNodes[3]) {
        fillPercent = 100;
    } else if (streak <= visibleNodes[0]) {
        fillPercent = Math.max(0, (streak / visibleNodes[0]) * 33.33);
    } else {
        if (streak <= visibleNodes[1]) {
            const ratio = (streak - visibleNodes[0]) / (visibleNodes[1] - visibleNodes[0]);
            fillPercent = 0 + ratio * 33.33;
        } else if (streak <= visibleNodes[2]) {
            const ratio = (streak - visibleNodes[1]) / (visibleNodes[2] - visibleNodes[1]);
            fillPercent = 33.33 + ratio * 33.33;
        } else {
            const ratio = (streak - visibleNodes[2]) / (visibleNodes[3] - visibleNodes[2]);
            fillPercent = 66.66 + ratio * 33.33;
        }
    }

    // 4. Sinh HTML cho từng chấm mốc
    let nodesHTML = visibleNodes.map((nodeVal, index) => {
        let statusClass = "";
        let icon = "";

        if (streak >= nodeVal) {
            statusClass = "is-completed"; // Đã đạt được
            icon = "✓";
        } else if (nodeVal === ALL_MILESTONES[targetIndex]) {
            statusClass = "is-target";    // Mục tiêu tiếp theo
            icon = "🔥";
        } else {
            statusClass = "is-locked";    // Mốc xa hơn
            icon = index === 3 ? "🏆" : "🎁";
        }

        let labelText = nodeVal >= 30 && nodeVal % 30 === 0
            ? `${nodeVal / 30}ヶ月`
            : `${nodeVal}日`;

        return `
            <div class="streak-roadmap__step ${statusClass}">
                <div class="streak-roadmap__node">${icon}</div>
                <span class="streak-roadmap__label">${labelText}</span>
            </div>
        `;
    }).join("");

    container.innerHTML = `
        <div class="streak-roadmap__track">
            <div class="streak-roadmap__fill" style="width: ${fillPercent.toFixed(1)}%;"></div>
        </div>
        ${nodesHTML}
    `;
}

/* =========================================================
   Show Achievement
   ========================================================= */
function showCurrentStreakAchievement(days) {
    const streakModal = document.getElementById("streakModal");
    const streakTitle = document.getElementById("streakTitle");
    const streakMessage = document.getElementById("streakMessage");
    const streakIcon = document.getElementById("streakIcon");

    if (!streakModal) {
        return;
    }
    renderStreakRoadmap(days);
    // ★ Xử lý riêng khi chuỗi bị reset (0 ngày) và khi có chuỗi (> 0 ngày)
    if (days <= 0) {
        streakTitle.textContent = "連続記録がリセットされました";
        streakMessage.innerHTML =
            "記録が途切れてしまいましたが、<br>今日からまた新しい記録をスタートさせましょう！";
        streakIcon.textContent = "🔄";
        streakModal.classList.add("is-reset");
    } else {
        streakModal.classList.remove("is-reset");
        streakTitle.textContent = `${days}日連続達成！`;

        if (days < 7) {
            streakMessage.textContent =
                "毎日の記録を続けて、7日連続達成を目指しましょう！";
            streakIcon.textContent = "🎯";
        } else {
            streakMessage.textContent =
                "素晴らしい継続力です！習慣が確実に身についています。";

            if (days >= 1095) {
                streakIcon.textContent = "💎";
            } else if (days >= 365) {
                streakIcon.textContent = "👑";
            } else if (days >= 90) {
                streakIcon.textContent = "🏆";
            } else if (days >= 30) {
                streakIcon.textContent = "⭐";
            } else {
                streakIcon.textContent = "🎉";
            }
        }
    }
    const streakConfetti = document.getElementById("streakConfetti");
    if (streakConfetti) {
        streakConfetti.innerHTML = "";
    }

    if (days > 0) {
        animateStreakNumber(days);
        if (isStreakMilestone(days)) {
            createConfetti();
        }
    }
    const streakNext = document.getElementById("streakNext");
    if (streakNext) {
        if (days > 0) {
            const remain = getNextStreakMilestone(days) - days;
            streakNext.textContent = `次の目標まで ${remain}日`;
            streakNext.style.display = "";
        } else {
            streakNext.style.display = "none";
        }
    }
    const streakNote = document.getElementById("streakNote");
    if (streakNote) {
        streakNote.textContent = days <= 0
            ? "途切れても大丈夫です。無理のないペースで、また始めましょう。"
            : "この調子で無理なく続けていきましょう！";
    }
    applyStreakIconEffect(streakIcon, days);
    streakModal.classList.add("is-visible");
    streakModal.setAttribute("aria-hidden", "false");
    document.body.classList.add("modal-open");
}

/* =========================================================
   Close Achievement
   ========================================================= */
function closeStreakAchievement() {
    cancelAnimationFrame(streakTickerId);
    const streakModal = document.getElementById("streakModal");
    if (!streakModal) {
        return;
    }
    if (streakModal.contains(document.activeElement)) {
        document.activeElement.blur();
    }
    streakModal.classList.remove("is-visible");
    streakModal.setAttribute("aria-hidden", "true");
    document.body.classList.remove("modal-open");
    document.getElementById("streakViewButton")?.focus();
}

/* =========================================================
   Confetti
   ========================================================= */
function createConfetti() {
    const streakConfetti = document.getElementById("streakConfetti");
    if (!streakConfetti) {
        return;
    }
    streakConfetti.innerHTML = "";
    const pieces = 70;
    for (let i = 0; i < pieces; i++) {
        const piece = document.createElement("span");
        piece.className = "streak-confetti-piece";
        const x = (Math.random() - 0.5) * 650;
        const y = 300 + Math.random() * 350;
        const rotate = Math.random() * 720 - 360;
        piece.style.left = `${50 + (Math.random() - 0.5) * 20}%`;
        piece.style.setProperty("--x", `${x}px`);
        piece.style.setProperty("--y", `${y}px`);
        piece.style.setProperty("--rotate", `${rotate}deg`);
        piece.style.animationDelay = `${Math.random() * 0.25}s`;
        piece.style.background = `hsl(${Math.random() * 360}, 80%, 60%)`;
        streakConfetti.appendChild(piece);
    }
}

/* =========================================================
   Health Streak View Button
   ========================================================= */
function applyStreakIconEffect(iconEl, days) {
    const effects = ["effect-target", "effect-pop", "effect-shine", "effect-trophy", "effect-crown"];
    iconEl.classList.remove(...effects);
    void iconEl.offsetWidth;

    if (days <= 0) return;
    if (days < 7) iconEl.classList.add("effect-target");
    else if (days < 30) iconEl.classList.add("effect-pop");
    else if (days < 90) iconEl.classList.add("effect-shine");
    else if (days < 365) iconEl.classList.add("effect-trophy");
    else iconEl.classList.add("effect-crown"); // 👑 và 💎
}

let streakTickerId = null;

function animateStreakNumber(days) {
    const streakTitle = document.getElementById("streakTitle");
    if (!streakTitle) return;

    cancelAnimationFrame(streakTickerId);

    const render = (n) => {
        streakTitle.textContent = isStreakMilestone(days)
            ? `${n}日連続達成！`
            : `🔥 ${n}日連続達成中！`;
    };

    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduceMotion || days <= 1) {
        render(days);
        return;
    }

    const duration = Math.min(400 + days * 15, 1600); // ngày càng nhiều thì chạy lâu hơn một chút
    const start = performance.now();

    const tick = (now) => {
        const t = Math.min((now - start) / duration, 1);
        const eased = 1 - Math.pow(1 - t, 3); // easeOutCubic: nhanh đầu, chậm cuối
        render(Math.round(days * eased));
        if (t < 1) {
            streakTickerId = requestAnimationFrame(tick);
        }
    };
    streakTickerId = requestAnimationFrame(tick);
}

/* =========================================================
 * Home - 4 Data Integrated Chart
 * ========================================================= */
(() => {
    let chartInstance = null;
    const WEEKDAYS = ["日", "月", "火", "水", "木", "金", "土"];
    const MAX_POINTS_FOR_VALUE_LABELS = 8;

    const SERIES = [
        { key: "weight", label: "体重 (kg)", color: "#f54b73", axis: "yWeight" },
        { key: "sleep", label: "睡眠 (時間)", color: "#7567e8", axis: "ySleep" },
        { key: "water", label: "水分摂取 (ml)", color: "#4b9bea", axis: "yWater" },
        { key: "step", label: "歩数 (歩)", color: "#18a874", axis: "yStep" }
    ];

    function formatXLabel(label, mode) {
        if (mode === "DAY") {
            const d = new Date(`${label}T00:00:00`);
            return `${d.getMonth() + 1}/${d.getDate()}(${WEEKDAYS[d.getDay()]})`;
        }
        if (mode === "WEEK") return HealthChart.formatLabel(label, "WEEK");
        return label;
    }

    function formatValue(key, v) {
        const n = Number(v);
        if (!Number.isFinite(n)) return "";
        return key === "weight" || key === "sleep" ? n.toFixed(1) : n.toLocaleString("ja-JP");
    }

    // 各点の値を表示するプラグイン（点が少ないときだけ）
    const valueLabelPlugin = {
        id: "homeValueLabels",
        afterDatasetsDraw(chart) {
            if (!chart.options.plugins.homeValueLabels?.enabled) return;
            const { ctx } = chart;
            chart.data.datasets.forEach((ds, i) => {
                const meta = chart.getDatasetMeta(i);
                if (meta.hidden) return;
                meta.data.forEach((el, idx) => {
                    const v = ds.data[idx];
                    if (v === null || v === undefined) return;
                    ctx.save();
                    ctx.font = "bold 11px sans-serif";
                    ctx.textAlign = "center";
                    if (ds.type === "bar") {
                        ctx.fillStyle = "#ffffff";
                        ctx.textBaseline = "top";
                        ctx.fillText(formatValue(ds.seriesKey, v), el.x, el.y + 4);
                    } else {
                        ctx.fillStyle = ds.borderColor;
                        ctx.textBaseline = "bottom";
                        ctx.fillText(formatValue(ds.seriesKey, v), el.x, el.y - 8);
                    }
                    ctx.restore();
                });
            });
        }
    };

    function lineDataset(s, data) {
        return {
            type: "line", seriesKey: s.key, label: s.label, data,
            borderColor: s.color, backgroundColor: s.color, borderWidth: 2.5,
            tension: 0, spanGaps: true, yAxisID: s.axis, order: 1,
            pointRadius: 5, pointHoverRadius: 7,
            pointBackgroundColor: s.color, pointBorderColor: "#ffffff", pointBorderWidth: 2
        };
    }

    function barDataset(s, data) {
        return {
            type: "bar", seriesKey: s.key, label: s.label, data,
            backgroundColor: s.color, borderColor: s.color, borderWidth: 0,
            borderRadius: 6, maxBarThickness: 40, yAxisID: s.axis, order: 2
        };
    }

    function axis(position, color, title, opts = {}) {
        return {
            type: "linear", position,
            grid: opts.grid
                ? { display: true, color: "#f1f5f9", drawTicks: false }
                : { drawOnChartArea: false },
            border: { display: true, color },
            ticks: { color, font: { weight: "bold" }, padding: 8 },
            title: { display: true, text: title, color, font: { weight: "bold" } },
            ...opts.scale
        };
    }

    function renderChart(canvas, data) {
        if (chartInstance) chartInstance.destroy();

        const mode = data.chartMode || "DAY";
        const labels = (data.labels || []).map((l) => formatXLabel(l, mode));
        const datasets = SERIES.map((s) => {
            const values = data[s.key]?.values || [];
            return s.key === "water" ? barDataset(s, values) : lineDataset(s, values);
        });

        chartInstance = new Chart(canvas, {
            data: { labels, datasets },
            plugins: [valueLabelPlugin],
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: { mode: "index", intersect: false },
                layout: { padding: { top: 24, left: 8, right: 8, bottom: 4 } },
                plugins: {
                    legend: { position: "top", labels: { usePointStyle: true, boxWidth: 8, padding: 20 } },
                    homeValueLabels: { enabled: labels.length <= MAX_POINTS_FOR_VALUE_LABELS }
                },
                scales: {
                    x: {
                        grid: { display: false },
                        border: { display: true, color: "#94a3b8", width: 1.5 },
                        offset: true,
                        ticks: { color: "#1e293b", font: { weight: "bold" }, padding: 8 }
                    },
                    yWeight: axis("left", "#f54b73", "体重 (kg)", { grid: true, scale: { grace: "10%" } }),
                    ySleep: axis("left", "#7567e8", "睡眠 (時間)", { scale: { grace: "10%" } }),
                    yWater: axis("right", "#2584dc", "水分 (ml)", { scale: { beginAtZero: true } }),
                    yStep: axis("right", "#18a874", "歩数 (歩)", { scale: { beginAtZero: true } })
                }
            }
        });
    }

    async function loadChart(canvas, from, to) {
        const profileId = canvas.dataset.profileId;
        const url = `/profile/${encodeURIComponent(profileId)}/home/summary-chart`
            + `?startDate=${encodeURIComponent(from)}&endDate=${encodeURIComponent(to)}`;
        try {
            const response = await fetch(url, { headers: { Accept: "application/json" } });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            renderChart(canvas, await response.json());
        } catch (error) {
            console.error("統合グラフデータの取得に失敗しました。", error);
        }
    }

        function initHomeIntegratedChart() {
        const canvas = document.getElementById("homeIntegratedChartCanvas");
        if (!canvas || !canvas.dataset.profileId || !canvas.dataset.from || !canvas.dataset.to) return;
        loadChart(canvas, canvas.dataset.from, canvas.dataset.to);
    }
    document.addEventListener("DOMContentLoaded", initHomeIntegratedChart);
})();