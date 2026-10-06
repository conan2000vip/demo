document.addEventListener("DOMContentLoaded", () => {
    initChart();
    initModal();
});

/* =========================================================
1. Step Chart / 歩数推移グラフ
   Uses shared HealthChart / 共通のHealthChartを使用
========================================================= */
function initChart() {
    const targetStep = window.targetStep !== null && window.targetStep !== undefined
        ? parseFloat(window.targetStep)
        : null;
    initMetricChart({
        canvasId: "stepChart",
        data: window.stepChartData,
        unit: "歩",
        type: "bar",
        color: "#e67e22",
        targetValue: targetStep,
    });
}
/* =========================================================
2. Add / Edit Modal / 新規登録・編集モーダル
========================================================= */
function initModal() {
    const overlay = document.getElementById("stepModalOverlay");
    const openBtns = [
        document.getElementById("openAddModalBtn"),
        document.getElementById("openAddModalBtnEmpty"),
        document.getElementById("openAddModalBtnFiltered")
    ].filter(Boolean);
    const closeBtn = document.getElementById("closeModalBtn");
    const cancelBtn = document.getElementById("cancelModalBtn");
    const modalTitle = document.getElementById("stepModalTitle");
    const form = document.getElementById("stepForm");

    const recordId = document.getElementById("recordId");
    const recordedDateInput = document.getElementById("recordedDate");
    const stepsInput = document.getElementById("steps");
    const memoInput = document.getElementById("memo");

    if (!overlay || !form || !recordId || !recordedDateInput || !stepsInput || !memoInput) {
        console.error("Step modal element is missing:", {
            overlay,
            form,
            recordId,
            recordedDateInput,
            stepsInput,
            memoInput
        });
        return;
    }

    // 警告を表示済みかどうかを保持する。
    let isStepsWarned = false;

    function openModal({ mode = "create", id = "", date = "", steps = "", memo = "" } = {}) {
        modalTitle.textContent = mode === "edit" ? "歩数を編集する" : "歩数を記録する";
        recordId.value = id;
        recordedDateInput.value = date || currentDate();
        recordedDateInput.max = currentDate();
        stepsInput.value = steps;
        memoInput.value = memo;
        isStepsWarned = false; // モーダルを開くときに警告状態をリセットする。
        clearAllErrors();
        overlay.classList.add("is-open");
        recordedDateInput.focus();
    }

    function closeModal() {
        overlay.classList.remove("is-open");
    }

    openBtns.forEach((btn) => {
        btn.addEventListener("click", () => {
            openModal({ mode: "create", date: btn.dataset.date || "" });
        });
    });

    if (closeBtn) closeBtn.addEventListener("click", closeModal);
    if (cancelBtn) cancelBtn.addEventListener("click", closeModal);

    overlay.addEventListener("click", (event) => {
        if (event.target === overlay) closeModal();
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && overlay.classList.contains("is-open")) closeModal();
    });

    // 記録編集（AJAXページ送り後も動くようイベントを委譲する）。
    document.addEventListener("click", (event) => {
        const btn = event.target.closest(".edit-btn");
        if (!btn) return;
        openModal({
            mode: "edit",
            id: btn.dataset.id || "",
            date: btn.dataset.date || "",
            steps: btn.dataset.steps || "",
            memo: btn.dataset.memo || ""
        });
    });

    // 入力中にエラーをクリアする。
    recordedDateInput.addEventListener("input", () => clearError("recordedDate"));

    // 歩数を修正したら警告状態をリセットする。
    stepsInput.addEventListener("input", () => {
        isStepsWarned = false;
        clearError("steps");
    });

    // 日付を検証する。
    function validateRecordedDate() {
        if (!recordedDateInput.value) {
            showError("recordedDate", "日付を入力してください");
            return false;
        }

        const selectedDate = new Date(recordedDateInput.value + "T00:00:00");
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        if (selectedDate > today) {
            showError("recordedDate", "未来の日付は指定できません");
            return false;
        }

        clearError("recordedDate");
        return true;
    }

    // 歩数を検証する。
    function validateSteps() {
        const value = stepsInput.value;

        if (value === "") {
            showError("steps", "歩数を入力してください");
            isStepsWarned = false;
            return false;
        }

        const steps = Number(value);

        if (!Number.isInteger(steps) || steps < 0 || steps > 200000) {
            showError("steps", "歩数は0〜200000歩の範囲で入力してください");
            isStepsWarned = false;
            return false;
        }

        if (steps > 50000 && !isStepsWarned) {
            showError("steps", "歩数が非常に大きいです。入力内容に間違いはありませんか？（※もう一度「保存」を押すと登録されます）");
            isStepsWarned = true; // 警告を表示済みにする。
            return false; // 初回は一時停止し、ユーザーの確認を求める。
        }

        // 2回目、または50,000以下の通常値なら保存を続行する。
        clearError("steps");
        return true;
    }

    form.addEventListener("submit", (event) => {
        const isRecordedDateValid = validateRecordedDate();
        const isStepsValid = validateSteps();

        if (!isRecordedDateValid || !isStepsValid) {
            event.preventDefault();
        }
    });

    function showError(inputId, message) {
        const input = document.getElementById(inputId);
        const errorBox = document.getElementById(inputId + "Error");
        if (!input || !errorBox) return;

        input.classList.add("invalid");
        const span = errorBox.querySelector("span");
        if (span) span.textContent = message;
        errorBox.classList.add("show");
    }

    function clearError(inputId) {
        const input = document.getElementById(inputId);
        const errorBox = document.getElementById(inputId + "Error");
        if (!input || !errorBox) return;

        input.classList.remove("invalid");
        const span = errorBox.querySelector("span");
        if (span) span.textContent = "";
        errorBox.classList.remove("show");
    }

    function clearAllErrors() {
        ["recordedDate", "steps"].forEach(clearError);
    }

    // 現在日付をYYYY-MM-DD形式で返す。
    function currentDate() {
        const date = new Date();
        const yyyy = date.getFullYear();
        const mm = String(date.getMonth() + 1).padStart(2, "0");
        const dd = String(date.getDate()).padStart(2, "0");
        return `${yyyy}-${mm}-${dd}`;
    }
}