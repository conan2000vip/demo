/* =========================================================
   Home page: general behavior
   (streak -> home-streak.js, integrated chart -> home-chart.js)
   ========================================================= */
document.addEventListener("DOMContentLoaded", () => {
    if (window.lucide) {
        lucide.createIcons();
    }

    initFeedbackToggle();
    initClickableCards();
});

/* ---------- "Show all feedback" toggle ---------- */
function initFeedbackToggle() {
    const button = document.getElementById("feedbackToggleBtn");
    if (!button) return;

    // e.g. "今日のフィードバックをすべて見る（N件）"
    const collapsedText = button.textContent;
    const VISIBLE_COUNT = 3; // must match the threshold in the Thymeleaf fragment
    let expanded = false;

    button.addEventListener("click", () => {
        expanded = !expanded;
        document.querySelectorAll("#feedbackSection .feedback-card").forEach((card, index) => {
            if (index >= VISIBLE_COUNT) {
                card.classList.toggle("is-hidden", !expanded);
            }
        });
        button.textContent = expanded ? "閉じる" : collapsedText;
    });
}

/* ---------- Cards that navigate on click / Enter / Space ---------- */
function initClickableCards() {
    document.querySelectorAll(".stat-card--clickable").forEach((card) => {
        const open = () => {
            const href = card.dataset.href;
            if (href) {
                window.location.href = href;
            }
        };

        card.addEventListener("click", open);
        card.addEventListener("keydown", (event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                open();
            }
        });
    });
}
