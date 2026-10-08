"use strict";
const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");
const path = require("node:path");
const source = fs.readFileSync(path.resolve(__dirname, "../../main/webapp/assets/js/api-client.js"), "utf8");
function setup(handler) {
  const calls = [], redirects = [];
  const sandbox = {
    document: { body: { dataset: { contextPath: "/ticketscenter" } }, dispatchEvent() {} },
    window: {}, location: { pathname: "/ticketscenter/me/orders", assign: value => redirects.push(value) },
    CustomEvent: class { constructor(type, options) { this.type = type; this.detail = options.detail; } },
    fetch: async (url, options) => { calls.push({ url, options }); return handler(url, options, calls); }
  };
  vm.runInNewContext(source, sandbox);
  return { client: sandbox.window.apiClient, calls, redirects };
}
function reply(status, body) { return { status, ok: status >= 200 && status < 300, json: async () => body }; }
test("POST gets runtime CSRF and invalidates cache after login", async () => {
  const { client, calls } = setup(url => reply(200, url.endsWith("/csrf") ? { data: { csrfToken: "runtime" } } : { data: { saved: true } }));
  await client.post("/auth/login", { email: "buyer.a@example.test" });
  await client.post("/me/profile", { fullName: "Buyer" });
  assert.equal(calls.filter(call => call.url.endsWith("/csrf")).length, 2);
  assert.equal(calls[1].options.headers["X-CSRF-Token"], "runtime");
  assert.equal(calls[1].options.credentials, "same-origin");
});
test("failed mutation is attempted once and exposes reconciliation error", async () => {
  const { client, calls } = setup(url => {
    if (url.endsWith("/csrf")) return reply(200, { data: { csrfToken: "runtime" } });
    throw new Error("offline");
  });
  await assert.rejects(client.post("/orders", {}), error => error.code === "NETWORK_ERROR" && error.status === 0);
  assert.equal(calls.filter(call => call.options.method === "POST").length, 1);
});
test("401 redirects to internal auth and preserves the current page", async () => {
  const { client, redirects } = setup(() => reply(401, { error: { code: "UNAUTHORIZED", message: "expired" } }));
  await assert.rejects(client.get("/me/orders"), error => error.status === 401);
  assert.equal(redirects[0], "/ticketscenter/auth?view=login&returnTo=%2Fme%2Forders");
});
test("null data is supported and unsafe paths never fetch", async () => {
  const { client, calls } = setup(() => reply(200, { data: null }));
  assert.equal(await client.get("/me/hold"), null);
  for (const value of ["//evil.test", "/\\evil", "/%2f%2fevil", "/../admin", "/auth\r\nHeader:x"])
    await assert.rejects(client.get(value));
  assert.equal(calls.length, 1);
});
test("encoded query values remain usable for event searches", async () => {
  const { client, calls } = setup(() => reply(200, { data: [] }));
  await client.get("/events?search=Nh%E1%BA%A1c");
  assert.equal(calls[0].url, "/ticketscenter/events?search=Nh%E1%BA%A1c");
});
