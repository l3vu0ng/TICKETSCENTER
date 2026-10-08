"use strict";
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

async function main() {
  const base = process.argv[2];
  const output = process.argv[3];
  assert.match(base || "", /^http:\/\/127\.0\.0\.1:[0-9]+\/ticketscenter$/);
  const moduleName = process.env.TC_PLAYWRIGHT_MODULE || "playwright";
  assert.equal(require(moduleName + "/package.json").version, "1.62.1", "Pinned Playwright version required");
  const { chromium } = require(moduleName);
  const options = { headless: true, timeout: 15000 };
  if (process.env.TC_BROWSER_EXECUTABLE) options.executablePath = process.env.TC_BROWSER_EXECUTABLE;
  const browser = await chromium.launch(options);
  try {
    const context = await browser.newContext({ baseURL: base + "/" });
    const page = await context.newPage();
    page.setDefaultTimeout(10000);
    const failures = [];
    page.on("pageerror", error => failures.push(error.message));
    page.on("console", message => {
      if (message.type() === "error") failures.push(message.text());
    });
    const live = await context.request.get(base + "/health/live", { timeout: 10000 });
    assert.equal(live.status(), 200);
    assert.deepEqual(await live.json(), { data: { status: "UP" } });
    for (const width of [320, 375, 768, 1440]) {
      await page.setViewportSize({ width, height: 900 });
      const response = await page.goto(base + "/test-m0-layout");
      assert.equal(response.status(), 200);
      await page.waitForFunction(() => window.apiClient && window.uiState);
      assert.equal(await page.locator("main p").first().textContent(), "<script>window.unescaped=true</script>");
      assert.equal(await page.evaluate(() => Boolean(window.unescaped)), false);
      assert.equal(await page.locator("main script").count(), 0);
      assert.match(await page.locator("footer").textContent(), /TicketsCenter/);
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
      assert.ok(overflow <= 1, "Horizontal overflow at " + width);
      await page.keyboard.press("Tab");
      assert.equal(await page.evaluate(() => document.activeElement.className), "skip-link");
      assert.equal(await page.locator(".skip-link").getAttribute("href"), "#main-content");
      assert.equal(await page.locator("#main-content").count(), 1);
      assert.ok(await page.locator(".skip-link").evaluate(element => element.getBoundingClientRect().top >= 0));
      assert.equal(await page.locator(".skip-link").evaluate(element => getComputedStyle(element).outlineStyle), "solid");
      await page.keyboard.press("Tab");
      assert.equal(await page.evaluate(() => document.activeElement.className), "brand");
      const states = await page.evaluate(() => {
        const element = document.getElementById("notifications");
        const values = [];
        for (const state of ["loading", "empty", "success", "error"]) {
          window.uiState[state](element, state === "error" ? { message: "<script>state</script>" } : "Kiểm thử");
          values.push({ state: element.dataset.state, busy: element.getAttribute("aria-busy"),
            live: element.getAttribute("aria-live"), script: element.querySelector("script") !== null });
        }
        return values;
      });
      assert.deepEqual(states.map(item => item.state), ["loading", "empty", "success", "error"]);
      assert.equal(states[0].busy, "true");
      assert.equal(states[3].busy, "false");
      assert.equal(states[3].live, "assertive");
      assert.ok(states.every(item => !item.script));
      await page.screenshot({ path: path.join(output, "m0-layout-" + width + ".png"), fullPage: true });
      console.log("M0 layout PASS: " + width + "px / keyboard / escaped content / UI states");
    }
    // Requests use a real browser-context session against the packaged WAR.
    const csrf = await context.request.get(base + "/auth/csrf");
    assert.equal(csrf.status(), 200);
    const token = (await csrf.json()).data.csrfToken;
    assert.equal(typeof token, "string");
    assert.equal(token.length, 43);
    const rejected = await context.request.post(base + "/auth/login", { data: {} });
    assert.equal(rejected.status(), 403);
    const correlation = rejected.headers()["x-correlation-id"];
    assert.equal((await rejected.json()).error.correlationId, correlation);
    const pending = await context.request.post(base + "/auth/login", {
      data: {}, headers: { "X-CSRF-Token": token }
    });
    assert.equal(pending.status(), 501, "M0 login contract must not report fake success");
    assert.deepEqual(failures, [], "Browser JavaScript/CSP/asset errors");
    await context.close();
    console.log("M0 BROWSER PASS: WAR live / isolated session / CSRF denial / unimplemented login501");
  } finally { await browser.close(); }
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
