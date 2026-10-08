"use strict";
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

// Uses the test container's route; no preview endpoint is packaged in the WAR.
async function main() {
  const { chromium } = require(process.env.TC_PLAYWRIGHT_MODULE || "playwright");
  assert.ok(process.env.TC_BROWSER_EXECUTABLE, "Set TC_BROWSER_EXECUTABLE");
  const browser = await chromium.launch({
    executablePath: process.env.TC_BROWSER_EXECUTABLE,
    headless: true
  });
  const output = process.env.TC_BROWSER_EVIDENCE_DIR;
  if (output) fs.mkdirSync(output, { recursive: true });
  try {
    const page = await browser.newPage();
    const errors = [];
    page.on("pageerror", error => errors.push(error.message));
    page.on("console", message => {
      if (message.type() === "error") errors.push(message.text());
    });
    for (const width of [320, 375, 768, 1440]) {
      await page.setViewportSize({ width, height: 900 });
      const response = await page.goto(process.argv[2] + "/test-layout?signedIn=true");
      assert.equal(response.status(), 200);
      await page.waitForFunction(() => window.apiClient && window.uiState);
      assert.equal(await page.locator("main p").first().textContent(), "<script>alert(1)</script>");
      assert.equal(await page.locator("nav a").count(), 3);
      assert.equal(await page.locator("nav a").first().textContent(), "Sự kiện");
      assert.equal(await page.locator("footer").textContent(), "TicketsCenter · Sự kiện và vé của bạn");
      const dimensions = await page.evaluate(() => ({
        scroll: document.documentElement.scrollWidth,
        width: document.documentElement.clientWidth
      }));
      assert.ok(dimensions.scroll <= dimensions.width + 1, `Horizontal overflow at ${width}`);
      await page.keyboard.press("Tab");
      assert.equal(await page.evaluate(() => document.activeElement.className), "skip-link");
      assert.ok(await page.locator(".skip-link").evaluate(element => element.getBoundingClientRect().top >= 0));
      assert.equal(await page.locator(".skip-link").evaluate(element => getComputedStyle(element).outlineStyle), "solid");
      await page.keyboard.press("Tab");
      assert.equal(await page.evaluate(() => document.activeElement.className), "brand");
      const states = await page.evaluate(() => {
        const element = document.getElementById("notifications");
        const results = [];
        for (const state of ["loading", "empty", "success", "error"]) {
          window.uiState[state](element, state === "error" ? { message: "<script>state</script>" } : "Trạng thái kiểm thử");
          results.push({
            state: element.dataset.state,
            busy: element.getAttribute("aria-busy"),
            live: element.getAttribute("aria-live"),
            script: element.querySelector("script") !== null,
            focused: document.activeElement === element
          });
        }
        return results;
      });
      assert.deepEqual(states.map(item => item.state), ["loading", "empty", "success", "error"]);
      assert.equal(states[0].busy, "true");
      assert.equal(states[3].busy, "false");
      assert.equal(states[3].live, "assertive");
      assert.equal(states[3].focused, true);
      assert.ok(states.every(item => !item.script));
      if (output) await page.screenshot({ path: path.join(output, `layout-${width}.png`), fullPage: true });
      console.log(`Layout PASS: ${width}px, escaped text, no overflow, keyboard focus, four UI states`);
    }
    assert.deepEqual(errors, [], "Browser/CSP errors");
  } finally {
    await browser.close();
  }
}
main().catch(error => { console.error(error); process.exitCode = 1; });
