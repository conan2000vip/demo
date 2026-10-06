/* =========================================================
   Health Streak Achievement - ストリーク
   ========================================================= */
(() => {
    "use strict";

    const DAY_MILESTONES = [3, 7, 14, 30, 60, 90, 100, 180, 500, 1000];
    const ROADMAP_NODE_COUNT = 4;

    let tickerId = null;

    const $ = (id) => document.getElementById(id);

    /* ---------- Milestone helpers ---------- */
    function parseDate(value) {
        if (typeof value !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return null;
        const [year, month, day] = value.split("-").map(Number);
        return new Date(Date.UTC(year, month - 1, day));
    }

    function addDays(date, days) {
        const result = new Date(date);
        result.setUTCDate(result.getUTCDate() + days);
        return result;
    }

    function addYears(date, years) {
        const year = date.getUTCFullYear() + years;
        const month = date.getUTCMonth();
        const day = Math.min(date.getUTCDate(), new Date(Date.UTC(year, month + 1, 0)).getUTCDate());
        return new Date(Date.UTC(year, month, day));
    }

    function daysBetween(from, to) {
        return Math.round((to - from) / 86400000);
    }

    function getStreakAge(days) {
        const start = parseDate(window.currentStreakStartDate);
        if (!start || days <= 0) return { label: `${days}日`, years: 0, remainingDays: days };

        const end = addDays(start, days - 1);
        let years = end.getUTCFullYear() - start.getUTCFullYear();
        while (years > 0 && addYears(start, years) > end) years--;
        const anniversary = addYears(start, years);
        const remainingDays = daysBetween(anniversary, end);
        const label = years === 0 ? `${days}日` : remainingDays === 0 ? `${years}年` : `${years}年${remainingDays}日`;
        return { label, years, remainingDays };
    }

    function getMilestones(days) {
        const milestones = DAY_MILESTONES.map((value) => ({
            days: value,
            label: value >= 30 && value % 30 === 0 ? `${value / 30}ヶ月` : `${value}日`,
        }));
        const start = parseDate(window.currentStreakStartDate);
        if (start) {
            const currentYears = getStreakAge(days).years;
            for (let year = 1; year <= Math.max(4, currentYears + 3); year++) {
                const anniversary = addYears(start, year);
                milestones.push({
                    days: daysBetween(start, anniversary) + 1,
                    label: `${year}年`,
                });
            }
        }
        return milestones.sort((a, b) => a.days - b.days);
    }

    function isMilestone(days) {
        if (DAY_MILESTONES.includes(days)) return true;
        const age = getStreakAge(days);
        return age.years > 0 && age.remainingDays === 0;
    }

    function showBrokenStreakNotice() {
        const days = Number(window.brokenStreakDays) || 0;
        const date = window.brokenStreakDate;
        if (days <= 0 || typeof date !== "string" || !date) return;

        const profileId = window.currentProfileId ?? "unknown";
        const storageKey = `healthStreakBrokenShown_${profileId}_${date}`;
        try {
            if (localStorage.getItem(storageKey)) return;
        } catch (e) {
            // ストレージを利用できない場合も通知を表示し続ける。
        }

        showAchievement(0, true, days);
        try {
            localStorage.setItem(storageKey, "true");
        } catch (e) {
            // ストレージがない場合は次回訪問時にも通知されることがある。
        }
    }

    function nextMilestone(days) {
        return getMilestones(days).find((milestone) => milestone.days > days) ?? { days: days + 1, label: `${days + 1}日` };
    }

    /* ---------- "Already shown" memory (localStorage) ---------- */
    function getStorageKey(days, occurredAt) {
        // window.currentProfileId is injected by Thymeleaf
        const profileId = window.currentProfileId ?? "unknown";
        // occurredAt is expected as "yyyy-MM-ddTHH:mm:ss" -> keep the date part only
        const dateOnly = typeof occurredAt === "string" && occurredAt.length >= 10 ? occurredAt.slice(0, 10) : "unknown-date";
        return `healthStreakModalShown_${profileId}_${days}_${dateOnly}`;
    }

    function findAndShowStreakFeedback() {
        if (!Array.isArray(window.homeFeedback)) return;

        const feedback = window.homeFeedback.find((f) => f.type === "HEALTH_STREAK");
        if (!feedback) return;

        const match = (feedback.title || "").match(/\d+/);
        if (!match) return;

        const days = Number(match[0]);
        const storageKey = getStorageKey(days, feedback.occurredAt);

        try {
            if (localStorage.getItem(storageKey)) return;
        } catch (e) {
            /* storage unavailable: show anyway */
        }

        showAchievement(days);

        try {
            localStorage.setItem(storageKey, "true");
        } catch (e) {
            /* ignore */
        }
    }

    /* 連続記録のロードマップ */
    function calcRoadmapFill(streak, nodes) {
        const last = nodes.length - 1;
        if (streak >= nodes[last].days) return 100;
        if (streak <= nodes[0].days) return 0;

        const i = nodes.findIndex((value, idx) => idx < last && streak < nodes[idx + 1].days);
        const ratio = (streak - nodes[i].days) / (nodes[i + 1].days - nodes[i].days);
        return ((i + ratio) / last) * 100;
    }

    function renderRoadmap(streak) {
        const container = $("streakRoadmap");
        const template = $("streakStepTemplate");
        if (!container || !template) return;

        if (streak <= 0) {
            container.style.display = "none";
            return;
        }
        container.style.display = "flex";

        const milestones = getMilestones(streak);
        const targetIndex = Math.max(
            0,
            milestones.findIndex((milestone) => milestone.days > streak),
        );

        // 対象の前後4ノードを表示する。
        const startIdx = Math.min(Math.max(0, targetIndex - 1), Math.max(0, milestones.length - ROADMAP_NODE_COUNT));
        const nodes = milestones.slice(startIdx, startIdx + ROADMAP_NODE_COUNT);

        const steps = nodes.map((milestone, index) => {
            const step = template.content.firstElementChild.cloneNode(true);
            let status = "";
            let icon;

            if (streak >= milestone.days) {
                status = "is-completed";
                icon = "✓";
            } else if (milestone === milestones[targetIndex]) {
                status = "is-active"; // CSS defines .is-active (not .is-target)
                icon = "🔥";
            } else {
                icon = index === nodes.length - 1 ? "🏆" : "🎁"; // locked: default style
            }

            if (status) step.classList.add(status);
            step.querySelector(".streak-roadmap__node").textContent = icon;
            step.querySelector(".streak-roadmap__label").textContent = milestone.label;
            return step;
        });

        container.querySelectorAll(".streak-roadmap__step").forEach((el) => el.remove());
        container.append(...steps);

        const fill = container.querySelector(".streak-roadmap__fill");
        if (fill) {
            fill.style.width = `${calcRoadmapFill(streak, nodes).toFixed(1)}%`;
        }
    }

    /* ---------- Icon + title effects ---------- */
    function applyIconEffect(iconEl, days) {
        if (!iconEl) return;

        const effects = ["effect-target", "effect-pop", "effect-shine", "effect-trophy", "effect-crown"];
        iconEl.classList.remove(...effects);
        void iconEl.offsetWidth; // restart the animation

        if (days <= 0) return;
        if (days < 7) iconEl.classList.add("effect-target");
        else if (days < 30) iconEl.classList.add("effect-pop");
        else if (days < 90) iconEl.classList.add("effect-shine");
        else if (days < 365) iconEl.classList.add("effect-trophy");
        else iconEl.classList.add("effect-crown");
    }

    function animateNumber(days) {
        const title = $("streakTitle");
        if (!title) return;

        cancelAnimationFrame(tickerId);

        const render = (n) => {
            const age = getStreakAge(n);
            if (n === days && isMilestone(days) && age.years > 0 && age.remainingDays === 0) {
                title.textContent = `${age.years}年達成！`;
            } else if (n === days && isMilestone(days)) {
                title.textContent = `${n}日連続達成！`;
            } else {
                title.textContent = `🔥 ${age.label}連続中！`;
            }
        };

        const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
        if (reduceMotion || days <= 1) {
            render(days);
            return;
        }

        const duration = Math.min(400 + days * 15, 1600);
        const start = performance.now();

        const tick = (now) => {
            const t = Math.min((now - start) / duration, 1);
            const eased = 1 - Math.pow(1 - t, 3); // easeOutCubic
            render(Math.round(days * eased));
            if (t < 1) {
                tickerId = requestAnimationFrame(tick);
            }
        };
        tickerId = requestAnimationFrame(tick);
    }

    function createConfetti() {
        const container = $("streakConfetti");
        if (!container) return;

        container.innerHTML = "";
        for (let i = 0; i < 70; i++) {
            const piece = document.createElement("span");
            piece.className = "streak-confetti-piece";
            piece.style.left = `${50 + (Math.random() - 0.5) * 20}%`;
            piece.style.setProperty("--x", `${(Math.random() - 0.5) * 650}px`);
            piece.style.setProperty("--y", `${300 + Math.random() * 350}px`);
            piece.style.setProperty("--rotate", `${Math.random() * 720 - 360}deg`);
            piece.style.animationDelay = `${Math.random() * 0.25}s`;
            piece.style.background = `hsl(${Math.random() * 360}, 80%, 60%)`;
            container.appendChild(piece);
        }
    }

    /* 表示と閉じる処理 */
    function pickIcon(days) {
        if (days >= 1095) return "💎";
        if (days >= 365) return "👑";
        if (days >= 90) return "🏆";
        if (days >= 30) return "⭐";
        return "🎉";
    }

    function showAchievement(days, hasPreviousStreak = false, brokenStreakDays = 0) {
        const modal = $("streakModal");
        const title = $("streakTitle");
        const message = $("streakMessage");
        const icon = $("streakIcon");
        if (!modal) return;

        renderRoadmap(days);

        if (brokenStreakDays > 0 || (days <= 0 && hasPreviousStreak)) {
            // Streak was reset
            modal.classList.add("is-reset");
            title.textContent = brokenStreakDays > 0 ? `${brokenStreakDays}日連続の記録が途切れました` : "連続記録が途切れました";
            message.innerHTML =
                brokenStreakDays > 0
                    ? "これまで積み重ねた習慣は消えません。<br>無理のないペースで、また記録を続けていきましょう！"
                    : "これまで積み重ねた習慣は消えません。<br>無理のないペースで、また記録を続けていきましょう！";
            icon.textContent = "🔄";
        } else if (days <= 0) {
            modal.classList.remove("is-reset");
            title.textContent = "健康記録を始めましょう";
            message.textContent = "体重・睡眠・水分・歩数を記録して、今日から健康習慣を始めましょう！";
            icon.textContent = "🌱";
        } else {
            modal.classList.remove("is-reset");
            const age = getStreakAge(days);
            title.textContent = age.years > 0 && age.remainingDays === 0 ? `${age.years}年達成！` : `${age.label}連続達成！`;

            if (days < 7) {
                message.textContent = "毎日の記録を続けて、7日連続達成を目指しましょう！";
                icon.textContent = "🎯";
            } else {
                message.textContent = "素晴らしい継続力です！習慣が確実に身についています。";
                icon.textContent = pickIcon(days);
            }
        }

        const confetti = $("streakConfetti");
        if (confetti) confetti.innerHTML = "";

        if (days > 0) {
            animateNumber(days);
            if (isMilestone(days)) createConfetti();
        }

        const next = $("streakNext");
        if (next) {
            if (days > 0) {
                const target = nextMilestone(days);
                next.textContent = `次の目標（${target.label}）まで ${target.days - days}日`;
                next.style.display = "";
            } else {
                next.style.display = "none";
            }
        }

        const note = $("streakNote");
        if (note) {
            note.textContent =
                days <= 0
                    ? hasPreviousStreak
                        ? "途切れても大丈夫です。無理のないペースで、また始めましょう。"
                        : "毎日の記録を少しずつ続けていきましょう！"
                    : "この調子で無理なく続けていきましょう！";
        }
        applyIconEffect(icon, days);
        modal.classList.add("is-visible");
        modal.setAttribute("aria-hidden", "false");
        document.body.classList.add("modal-open");
    }

    function closeAchievement() {
        cancelAnimationFrame(tickerId);
        const modal = $("streakModal");
        if (!modal) return;
        if (modal.contains(document.activeElement)) {
            document.activeElement.blur();
        }
        modal.classList.remove("is-visible");
        modal.setAttribute("aria-hidden", "true");
        document.body.classList.remove("modal-open");
        $("streakViewButton")?.focus();
    }

    /* 初期化 */
    function readCurrentStreak() {
        const button = $("streakViewButton");
        const strong = button?.querySelector("strong");
        const raw = strong?.dataset.streak ?? strong?.textContent ?? 0;
        return Number(raw) || 0;
    }

    function updateCurrentStreakDisplay() {
        const value = $("streakViewButton")?.querySelector("strong");
        if (!value) return;
        const age = getStreakAge(readCurrentStreak());
        value.textContent = age.years > 0 && age.remainingDays === 0 ? `${age.years}年達成` : age.label;
    }

    function init() {
        const modal = $("streakModal");
        if (!modal) return;
        updateCurrentStreakDisplay();
        $("streakModalButton")?.addEventListener("click", closeAchievement);
        $("streakViewButton")?.addEventListener("click", (event) => {
            const hasPreviousStreak = event.currentTarget.dataset.hasPreviousStreak === "true";
            showAchievement(readCurrentStreak(), hasPreviousStreak, Number(window.brokenStreakDays) || 0);
        });
        modal.querySelector(".streak-modal__overlay")?.addEventListener("click", closeAchievement);

        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && modal.classList.contains("is-visible")) {
                closeAchievement();
            }
        });
        showBrokenStreakNotice();
        if (!modal.classList.contains("is-visible")) {
            findAndShowStreakFeedback();
        }
    }
    document.addEventListener("DOMContentLoaded", init);
})();
