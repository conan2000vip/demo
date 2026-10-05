/* =========================================================
   Home - 4 Data Integrated Chart
   ========================================================= */
(() => {
    "use strict";
    let chartInstance = null;
    let lastChartData = null;

    // Metric shown as bars (the other three are drawn as lines)
    let primaryKey = "weight";
    const BAR_AREA_RATIO = 0.45;
    const LINE_AREA_START = 0.55;
    const LINE_AREA_END = 0.95;
    const LINE_BAND_GAP = 0.04;
    const SERIES = [
        { key: "weight", label: "体重", legendLabel: "体重 (kg)", unit: "kg", color: "#e11d48" },
        { key: "sleep", label: "睡眠", legendLabel: "睡眠 (時間)", unit: "時間", color: "#4a3b8c" },
        { key: "water", label: "水分", legendLabel: "水分摂取 (ml)", unit: "ml", color: "#2196f3" },
        { key: "step", label: "歩数", legendLabel: "歩数 (歩)", unit: "歩", color: "#e67e22" },
    ];

    const getSeries = (key) => SERIES.find((s) => s.key === key);

    function formatValue(key, value) {
        const n = Number(value);
        if (!Number.isFinite(n)) return "-";
        if (key === "sleep") {
            const totalMinutes = Math.round(n * 60);
            const hours = Math.floor(totalMinutes / 60);
            const minutes = String(totalMinutes % 60).padStart(2, "0");
            return `${hours}h${minutes}`;
        }
        if (key === "weight") return n.toFixed(1);
        return n.toLocaleString("ja-JP");
    }

    function formatTooltipValue(key, value) {
        if (value === null || value === undefined) return "-";
        if (key === "sleep") return formatValue(key, value);
        return `${formatValue(key, value)} ${getSeries(key).unit}`;
    }

    function toNumbers(values) {
        return values
            .filter((value) => value !== null && value !== undefined)
            .map(Number)
            .filter(Number.isFinite);
    }

    function roundUpToNiceNumber(value) {
        const magnitude = 10 ** Math.floor(Math.log10(value));
        const scaled = value / magnitude;
        const niceScaled = scaled <= 1 ? 1 : scaled <= 2 ? 2 : scaled <= 5 ? 5 : 10;
        return niceScaled * magnitude;
    }

    function roundDownToNiceNumber(value) {
        const magnitude = 10 ** Math.floor(Math.log10(value));
        const scaled = value / magnitude;
        const niceScaled = scaled < 2 ? 1 : scaled < 5 ? 2 : 5;
        return niceScaled * magnitude;
    }

    function getPrimaryBounds(key, values) {
        const nums = toNumbers(values);
        if (!nums.length) return { min: 0, max: 1 };

        const dataMin = Math.min(...nums);
        const dataMax = Math.max(...nums);

        if (key === "water" || key === "step") {
            const max = Math.max(dataMax / BAR_AREA_RATIO, 1);
            const roundedMax = roundUpToNiceNumber(max);
            return {
                min: 0,
                max: roundedMax,
                stepSize: roundDownToNiceNumber(roundedMax / 10),
            };
        }

        const range = dataMax - dataMin;
        const referenceRange = range > 0 ? range : Math.max(Math.abs(dataMax) * 0.05, 0.5);
        const stepSize = roundUpToNiceNumber(referenceRange / (BAR_AREA_RATIO * 8));
        const min = Math.floor((dataMin - referenceRange * 0.1) / stepSize) * stepSize;
        const requiredMax = min + (dataMax - min) / BAR_AREA_RATIO;
        const max = Math.ceil(requiredMax / stepSize) * stepSize;
        return { min, max, stepSize };
    }

    function normalizeForDisplay(values, axisMin, axisMax, bandIndex) {
        const nums = toNumbers(values);
        if (!nums.length) return values.map(() => null);

        const dataMin = Math.min(...nums);
        const dataMax = Math.max(...nums);
        const axisRange = axisMax - axisMin;
        const lineCount = SERIES.length - 1;
        const totalBandGap = LINE_BAND_GAP * (lineCount - 1);
        const bandHeight = (LINE_AREA_END - LINE_AREA_START - totalBandGap) / lineCount;
        const bandStart = LINE_AREA_START + bandIndex * (bandHeight + LINE_BAND_GAP);
        const displayMin = axisMin + axisRange * bandStart;
        const displayMax = axisMin + axisRange * (bandStart + bandHeight);

        if (dataMin === dataMax) {
            const middle = (displayMin + displayMax) / 2;
            return values.map((value) => (value == null ? null : middle));
        }

        return values.map((value) => {
            if (value === null || value === undefined) return null;
            const n = Number(value);
            if (!Number.isFinite(n)) return null;
            const ratio = (n - dataMin) / (dataMax - dataMin);
            return displayMin + ratio * (displayMax - displayMin);
        });
    }

    const valueLabelPlugin = {
        id: "homeValueLabels",
        afterDatasetsDraw(chart) {
            if (!chart.options.plugins.homeValueLabels?.enabled) return;
            const { ctx } = chart;
            const lineLabelsByIndex = new Map();

            chart.data.datasets.forEach((dataset, datasetIndex) => {
                if (dataset.type !== "bar" && dataset.type !== "line") return;
                const meta = chart.getDatasetMeta(datasetIndex);
                if (meta.hidden) return;

                meta.data.forEach((element, index) => {
                    const rawValue = dataset.rawData?.[index];
                    if (rawValue === null || rawValue === undefined) return;

                    if (dataset.type === "line") {
                        const labels = lineLabelsByIndex.get(index) || [];
                        labels.push({ dataset, element, rawValue });
                        lineLabelsByIndex.set(index, labels);
                        return;
                    }

                    ctx.save();
                    ctx.font = "bold 12px sans-serif";
                    ctx.textAlign = "center";
                    ctx.fillStyle = "#1e293b";
                    ctx.textBaseline = "bottom";
                    ctx.fillText(formatValue(dataset.seriesKey, rawValue), element.x, element.y - 6);
                    ctx.restore();
                });
            });

            lineLabelsByIndex.forEach((labels) => {
                labels.sort((a, b) => b.element.y - a.element.y);
                let previousLabelY = Infinity;

                labels.forEach(({ dataset, element, rawValue }) => {
                    const labelY = Math.min(element.y - 8, previousLabelY - 14);
                    const label = formatValue(dataset.seriesKey, rawValue);

                    ctx.save();
                    ctx.font = "bold 10px sans-serif";
                    ctx.textAlign = "center";
                    ctx.textBaseline = "bottom";
                    ctx.lineWidth = 3;
                    ctx.lineJoin = "round";
                    ctx.strokeStyle = "#ffffff";
                    ctx.strokeText(label, element.x, labelY);
                    ctx.fillStyle = dataset.borderColor;
                    ctx.fillText(label, element.x, labelY);
                    ctx.restore();

                    previousLabelY = labelY;
                });
            });
        },
    };

    const axisTitlePlugin = {
        id: "homeAxisTitle",
        afterDraw(chart, _args, options) {
            const { ctx, chartArea } = chart;
            if (!chartArea || !options.text) return;
            ctx.save();
            ctx.fillStyle = options.color;
            ctx.font = "bold 12px sans-serif";
            ctx.textAlign = "left";
            ctx.textBaseline = "bottom";
            ctx.fillText(options.text, chartArea.left, chartArea.top - 8);
            ctx.restore();
        },
    };

    function createBarDataset(series, rawData) {
        return {
            type: "bar",
            seriesKey: series.key,
            label: series.legendLabel,
            data: rawData,
            rawData,
            backgroundColor: series.color,
            borderColor: series.color,
            borderWidth: 0,
            borderRadius: 6,
            maxBarThickness: 42,
            categoryPercentage: 0.65,
            barPercentage: 0.8,
            yAxisID: "y",
            order: 2,
        };
    }

    function createLineDataset(series, rawData, axisMin, axisMax, bandIndex) {
        return {
            type: "line",
            seriesKey: series.key,
            label: series.legendLabel,
            data: normalizeForDisplay(rawData, axisMin, axisMax, bandIndex),
            rawData,
            borderColor: series.color,
            backgroundColor: series.color,
            borderWidth: 2.5,
            tension: 0,
            spanGaps: true,
            pointRadius: 4,
            pointHoverRadius: 6,
            pointBackgroundColor: "#ffffff",
            pointBorderColor: series.color,
            pointBorderWidth: 2.5,
            yAxisID: "y",
            order: 1,
        };
    }

    function renderLegend() {
        const container = document.getElementById("homeIntegratedChartLegend");
        const template = document.getElementById("chartLegendItemTemplate");
        if (!container || !template) return;

        const items = SERIES.map((series) => {
            const item = template.content.firstElementChild.cloneNode(true);
            const symbol = item.querySelector(".home-chart-legend-symbol");
            symbol.classList.add(series.key === primaryKey ? "home-chart-legend-symbol--bar" : "home-chart-legend-symbol--line");
            symbol.style.setProperty("--series-color", series.color);
            item.querySelector(".home-chart-legend-label").textContent = series.legendLabel;
            return item;
        });
        container.replaceChildren(...items);
    }

    function updateMainButtons() {
        document.querySelectorAll("[data-main-metric]").forEach((button) => {
            const active = button.dataset.mainMetric === primaryKey;
            button.classList.toggle("is-active", active);
            button.setAttribute("aria-pressed", active ? "true" : "false");
        });
    }

    function renderChart(canvas, data) {
        chartInstance?.destroy();

        const mode = data.chartMode || "DAY";
        const labels = (data.labels || []).map((label) => HealthChart.formatLabel(label, mode));
        const primarySeries = getSeries(primaryKey);
        const bounds = getPrimaryBounds(primaryKey, data[primaryKey]?.values || []);
        let lineBandIndex = 0;
        const datasets = SERIES.map((series) => {
            const values = data[series.key]?.values || [];
            if (series.key === primaryKey) return createBarDataset(series, values);
            return createLineDataset(series, values, bounds.min, bounds.max, lineBandIndex++);
        });

        renderLegend();
        updateMainButtons();

        chartInstance = new Chart(canvas, {
            data: { labels, datasets },
            plugins: [valueLabelPlugin, axisTitlePlugin],
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: { mode: "index", intersect: false },
                layout: { padding: { top: 28, left: 8, right: 16, bottom: 0 } },
                plugins: {
                    legend: { display: false },
                    homeValueLabels: { enabled: true },
                    homeAxisTitle: {
                        text: primarySeries.legendLabel,
                        color: primarySeries.color,
                    },
                    tooltip: {
                        displayColors: true,
                        callbacks: {
                            label(context) {
                                const dataset = context.dataset;
                                const rawValue = dataset.rawData?.[context.dataIndex];
                                return `${dataset.label}: ${formatTooltipValue(dataset.seriesKey, rawValue)}`;
                            },
                        },
                    },
                },
                scales: {
                    x: {
                        position: "bottom",
                        grid: { display: false },
                        border: { display: true, color: "#94a3b8", width: 1.5 },
                        offset: true,
                        ticks: { color: "#1e293b", font: { weight: "bold" }, padding: 1 },
                    },
                    y: {
                        type: "linear",
                        position: "left",
                        min: bounds.min,
                        max: bounds.max,
                        grid: { display: true, color: "#eef2f7", drawTicks: false },
                        border: { display: true, color: primarySeries.color },
                        ticks: {
                            color: primarySeries.color,
                            padding: 8,
                            stepSize: bounds.stepSize,
                            font: { weight: "bold" },
                            callback(value) {
                                if (primaryKey === "step" || primaryKey === "water") {
                                    return Number(value).toLocaleString("ja-JP");
                                }
                                return formatValue(primaryKey, value);
                            },
                        },
                        title: { display: false },
                    },
                },
            },
        });
    }

    function toIsoDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        return `${year}-${month}-${day}`;
    }

    function getQuickRange(range) {
        const end = new Date();
        end.setHours(0, 0, 0, 0);
        const start = new Date(end);
        if (range === "1W") start.setDate(start.getDate() - 6);
        if (range === "30D") start.setDate(start.getDate() - 29);
        if (range === "6M" || range === "1Y") {
            const months = range === "6M" ? 6 : 12;
            const originalDay = start.getDate();
            start.setDate(1);
            start.setMonth(start.getMonth() - months);
            start.setDate(Math.min(originalDay, new Date(start.getFullYear(), start.getMonth() + 1, 0).getDate()));
        }
        return { from: toIsoDate(start), to: toIsoDate(end) };
    }

    function setActiveQuickRange(from, to) {
        document.querySelectorAll("#homeChartQuickRangeBar [data-range]").forEach((button) => {
            const range = getQuickRange(button.dataset.range);
            button.classList.toggle("is-active", range.from === from && range.to === to);
        });
    }

    function formatDateRange(from, to) {
        const format = (isoDate, includeYear = true) => {
            const [year, month, day] = isoDate.split("-");
            return `${includeYear ? `${year}/` : ""}${month}/${day}`;
        };
        if (window.matchMedia("(max-width: 420px)").matches) {
            const [fromYear] = from.split("-");
            const [toYear] = to.split("-");
            return fromYear === toYear ? `${format(from, false)} – ${format(to, false)}` : `${format(from)} – ${format(to)}`;
        }
        return `${format(from)} – ${format(to)}`;
    }

    function positionDatePopover(toggle, popover) {
        if (!popover.matches(":popover-open") && !popover.classList.contains("is-open")) return;
        const anchor = toggle.getBoundingClientRect();
        const popoverRect = popover.getBoundingClientRect();
        const margin = 16;
        const left = Math.max(margin, Math.min(anchor.right - popoverRect.width, window.innerWidth - popoverRect.width - margin));
        let top = anchor.bottom + 8;
        if (top + popoverRect.height > window.innerHeight - margin) {
            top = anchor.top - popoverRect.height - 8;
        }
        top = Math.max(margin, Math.min(top, window.innerHeight - popoverRect.height - margin));
        popover.style.left = `${left}px`;
        popover.style.top = `${top}px`;
    }

    function initChartFilter(canvas) {
        const form = document.getElementById("homeChartFilterForm");
        const startInput = document.getElementById("homeChartStartDate");
        const endInput = document.getElementById("homeChartEndDate");
        const error = document.getElementById("homeChartFilterError");
        const toggle = document.getElementById("homeChartDateToggle");
        const dateLabel = document.getElementById("homeChartDateLabel");
        const cancel = document.getElementById("homeChartDateCancel");
        if (!form || !startInput || !endInput || !toggle || !dateLabel) return;

        const popoverApiAvailable = typeof form.showPopover === "function";

        const updateDateLabel = () => {
            dateLabel.textContent = formatDateRange(startInput.value, endInput.value);
        };

        const search = async (from, to) => {
            error.hidden = true;
            error.textContent = "";
            if (!from || !to || from > to) {
                error.textContent = "開始日は終了日以前の日付を指定してください。";
                error.hidden = false;
                return;
            }
            startInput.value = from;
            endInput.value = to;
            canvas.dataset.from = from;
            canvas.dataset.to = to;
            setActiveQuickRange(from, to);
            updateDateLabel();
            await loadChart(canvas, from, to);
            if (popoverApiAvailable) form.hidePopover();
            else {
                form.classList.remove("is-open");
                toggle.setAttribute("aria-expanded", "false");
            }
        };

        toggle.addEventListener("click", () => {
            if (window.lucide) {
                lucide.createIcons();
            }
            if (!popoverApiAvailable) {
                form.classList.toggle("is-open");
                toggle.setAttribute("aria-expanded", String(form.classList.contains("is-open")));
                positionDatePopover(toggle, form);
                return;
            }
            if (form.matches(":popover-open")) form.hidePopover();
            else {
                form.showPopover({ source: toggle });
                requestAnimationFrame(() => positionDatePopover(toggle, form));
            }
        });

        form.addEventListener("toggle", () => {
            toggle.setAttribute("aria-expanded", String(form.matches(":popover-open")));
        });

        cancel?.addEventListener("click", () => {
            const defaultRange = getQuickRange("1W");
            search(defaultRange.from, defaultRange.to);

            if (popoverApiAvailable) form.hidePopover();
            else {
                form.classList.remove("is-open");
                toggle.setAttribute("aria-expanded", "false");
            }
        });

        window.addEventListener("resize", () => positionDatePopover(toggle, form));
        window.addEventListener("scroll", () => positionDatePopover(toggle, form), true);

        form.addEventListener("submit", (event) => {
            event.preventDefault();
            search(startInput.value, endInput.value);
        });

        document.querySelectorAll("#homeChartQuickRangeBar [data-range]").forEach((button) => {
            button.addEventListener("click", () => {
                const range = getQuickRange(button.dataset.range);
                search(range.from, range.to);
            });
        });

        [startInput, endInput].forEach((input) => {
            input.addEventListener("change", () => setActiveQuickRange(startInput.value, endInput.value));
        });

        updateDateLabel();
        window.addEventListener("resize", updateDateLabel);
        setActiveQuickRange(startInput.value, endInput.value);
    }

    async function loadChart(canvas, from, to) {
        const profileId = canvas.dataset.profileId;
        const url = `/profile/${encodeURIComponent(profileId)}/home/summary-chart` + `?startDate=${encodeURIComponent(from)}&endDate=${encodeURIComponent(to)}`;

        try {
            const response = await fetch(url, { headers: { Accept: "application/json" } });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            lastChartData = await response.json();
            renderChart(canvas, lastChartData);
        } catch (error) {
            console.error("統合グラフデータの取得に失敗しました。", error);
        }
    }

    function initMainMetricSelector(canvas) {
        document.querySelectorAll("[data-main-metric]").forEach((button) => {
            button.addEventListener("click", () => {
                const key = button.dataset.mainMetric;
                if (!getSeries(key) || key === primaryKey) return;
                primaryKey = key;
                if (lastChartData) renderChart(canvas, lastChartData);
            });
        });
    }

    function init() {
        const canvas = document.getElementById("homeIntegratedChartCanvas");
        if (!canvas || !canvas.dataset.profileId || !canvas.dataset.from || !canvas.dataset.to) return;
        initMainMetricSelector(canvas);
        initChartFilter(canvas);
        loadChart(canvas, canvas.dataset.from, canvas.dataset.to);
    }

    document.addEventListener("DOMContentLoaded", init);
})();
