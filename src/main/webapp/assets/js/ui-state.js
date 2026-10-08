(() => {
  "use strict";
  function show(element, state, message) {
    element.dataset.state = state;
    element.setAttribute("aria-live", state === "error" ? "assertive" : "polite");
    element.setAttribute("aria-busy", String(state === "loading"));
    element.textContent = message;
    if (state === "error") {
      element.setAttribute("tabindex", "-1");
      element.focus();
    }
  }
  window.uiState = Object.freeze({
    loading: (element, message = "Đang tải…") => show(element, "loading", message),
    empty: (element, message = "Chưa có dữ liệu.") => show(element, "empty", message),
    success: (element, message = "Đã hoàn tất.") => show(element, "success", message),
    error: (element, error) => show(element, "error", error.message || "Yêu cầu thất bại.")
  });
})();
