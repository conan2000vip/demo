document.addEventListener("DOMContentLoaded", () => {
  const form = document.querySelector(".profile-form");
  if (!form) return;

  const goals = [
    {
      id: "name",
      validate(value) {
        const name = value.trim();
        if (!name) return "名前を入力してください。";
        if (value.length > 100) return "名前は100文字以内で入力してください。";
        return "";
      }
    },
    {
      id: "birthDate",
      validate(value) {
        if (!value) return "生年月日を入力してください。";
        if (!/^\d{4}-\d{2}-\d{2}$/.test(value) || Number.isNaN(new Date(`${value}T00:00:00`).getTime())) {
          return "正しい生年月日を入力してください。";
        }
        const today = new Date();
        const todayValue = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
        if (value > todayValue) return "生年月日には未来の日付を指定できません。";
        return "";
      }
    },
    {
      id: "relationship",
      validate(value) {
        if (!value) return "続柄を選択してください。";
        return "";
      }
    },
    {
      id: "height",
      validate(value) {
        if (!value) return "身長を入力してください。";
        if (!/^-?(?:\d+(?:\.\d*)?|\.\d+)$/.test(value)) return "身長は数値で入力してください。";
        const height = Number(value);
        const errors = [];
        if (height < 10 || height > 350) errors.push("身長は10〜350cmの範囲で入力してください。");
        if ((value.split(".")[1] || "").length > 1) errors.push("小数点以下1桁まで入力してください。");
        return errors.join(" ");
      }
    },
    {
      id: "targetWeight",
      validate(value) {
        if (!value) return "";
        if (!/^-?(?:\d+(?:\.\d*)?|\.\d+)$/.test(value)) return "体重目標は数値で入力してください。";
        const weight = Number(value);
        const errors = [];
        if (weight < 1 || weight > 700) errors.push("体重目標は1〜700kgの範囲で入力してください。");
        if ((value.split(".")[1] || "").length > 1) errors.push("小数点以下1桁まで入力してください。");
        return errors.join(" ");
      }
    },
    {
      id: "waterGoalMl",
      validate(value) {
        if (!value) return "";
        if (!/^-?\d+$/.test(value)) return "水分目標は数値で入力してください（整数のみ）。";
        const amount = Number(value);
        if (!Number.isSafeInteger(amount) || amount < 0 || amount > 10000) {
          return "水分目標は0〜10000mlの範囲で入力してください。";
        }
        return "";
      }
    },
    {
      id: "stepGoal",
      validate(value) {
        if (!value) return "";
        if (!/^-?\d+$/.test(value)) return "歩数目標は数値で入力してください（整数のみ）。";
        const steps = Number(value);
        if (!Number.isSafeInteger(steps) || steps < 0 || steps > 200000) {
          return "歩数目標は0〜200000歩の範囲で入力してください。";
        }
        return "";
      }
    },
    {
      id: "dailySleepGoal",
      validate(value) {
        if (!value) return "";
        if (!/^-?(?:\d+(?:\.\d*)?|\.\d+)$/.test(value)) return "睡眠目標は数値で入力してください。";
        const hours = Number(value);
        const errors = [];
        if (hours < 0 || hours > 24) errors.push("睡眠目標は0〜24時間の範囲で入力してください。");
        if ((value.split(".")[1] || "").length > 1) errors.push("小数点以下1桁まで入力してください。");
        return errors.join(" ");
      }
    }
  ];

  const controls = goals.map((goal) => {
    const input = document.getElementById(goal.id);
    if (!input || input.type === "hidden") return null;

    const group = input.closest(".input-group");
    if (!group) return null;
    let error = document.getElementById(`${goal.id}Error`);
    if (!error) {
      error = group.querySelector(".field-error");
      if (!error) {
        error = document.createElement("span");
        error.className = "field-error";
        group.appendChild(error);
      }
      error.id = `${goal.id}Error`;
      error.setAttribute("role", "alert");
    }

    input.addEventListener("input", () => validateControl(goal, input, error));
    input.addEventListener("change", () => validateControl(goal, input, error));
    input.addEventListener("blur", () => validateControl(goal, input, error));
    return { goal, input, error };
  }).filter(Boolean);

  form.addEventListener("submit", (event) => {
    const isValid = controls
      .map(({ goal, input, error }) => validateControl(goal, input, error))
      .every(Boolean);
    if (!isValid) event.preventDefault();
  });

  function validateControl(goal, input, error) {
    const message = goal.validate(input.value.trim());
    error.textContent = message;
    error.classList.toggle("show", Boolean(message));
    input.classList.toggle("invalid", Boolean(message));
    input.setAttribute("aria-invalid", String(Boolean(message)));
    if (message) {
      input.setAttribute("aria-describedby", error.id);
    } else {
      input.removeAttribute("aria-describedby");
    }
    return !message;
  }
});
