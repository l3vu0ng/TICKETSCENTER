(() => {
  "use strict";
  const context = document.body?.dataset.contextPath || "";
  let csrfPromise;
  function url(path) {
    const pathname = typeof path === "string" ? path.split("?")[0] : "";
    if (typeof path !== "string" || !path.startsWith("/") || path.startsWith("//")
        || /[\\\\\u0000-\u0020]/.test(path) || pathname.includes("%")
        || pathname.split("/").some(segment => segment === "." || segment === "..")) {
      throw new TypeError("Expected an internal path");
    }
    return context + path;
  }
  async function request(path, options = {}) {
    let response;
    try {
      response = await fetch(url(path), { credentials: "same-origin", ...options,
        headers: { Accept: "application/json", ...options.headers } });
    } catch (cause) {
      throw Object.assign(new Error("Không thể kết nối. Hãy kiểm tra trạng thái trước khi thử lại."),
        { code: "NETWORK_ERROR", status: 0, cause });
    }
    let envelope;
    try { envelope = await response.json(); }
    catch { throw Object.assign(new Error("Phản hồi không hợp lệ"), { code: "INVALID_RESPONSE", status: response.status }); }
    if (!response.ok) {
      const details = envelope.error || {};
      if (response.status === 401) {
        csrfPromise = undefined;
        const destination = location.pathname.startsWith(context + "/") ? location.pathname.slice(context.length) : "/events";
        location.assign(context + "/auth?view=login&returnTo=" + encodeURIComponent(destination));
      }
      if (response.status === 403 && details.code === "FORBIDDEN") csrfPromise = undefined;
      if (response.status === 409) document.dispatchEvent(new CustomEvent("api:conflict", { detail: details }));
      throw Object.assign(new Error(details.message || "Yêu cầu thất bại"), details, { status: response.status });
    }
    if (!Object.hasOwn(envelope, "data")) throw Object.assign(new Error("Phản hồi không hợp lệ"), { code: "INVALID_RESPONSE", status: response.status });
    return envelope.data;
  }
  async function csrf() {
    if (!csrfPromise) csrfPromise = request("/auth/csrf").then(data => data.csrfToken)
      .catch(error => { csrfPromise = undefined; throw error; });
    return csrfPromise;
  }
  window.apiClient = Object.freeze({
    get: path => request(path),
    async post(path, body) {
      const token = await csrf();
      const data = await request(path, { method: "POST", headers: { "Content-Type": "application/json", "X-CSRF-Token": token }, body: JSON.stringify(body) });
      if (["/auth/login", "/auth/logout", "/auth/password/reset"].includes(path)) csrfPromise = undefined;
      return data;
    },
    resetCsrf() { csrfPromise = undefined; }
  });
})();
