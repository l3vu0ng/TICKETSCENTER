# Shared frontend foundation

Include header.jspf → notifications.jspf → content → footer.jspf under WEB-INF/views.
ViewSupport sets viewModel/contextPath and defaults pageTitle, activeNav, memberships,
notice. Set currentUser to the safe UserDto and memberships to a current Page from Đông;
never pass an entity graph or a stale role map. Views must be explicitly allowlisted.

The header shows personal, admin and organization/check-in links from current DTOs.
Navigation is presentation only; each service still rechecks permissions.

Bootstrap 5.3.0 is served locally from its WebJar. app.css follows the reference palette
and supports narrow screens, wrapped navigation, visible keyboard focus and table overflow.
JSTL c:out escapes user text; no scriptlets or prototype mock data are used.

apiClient.get/post return data. POST obtains a runtime session CSRF token, invalidates
its cache after login/logout/reset, uses the WAR context path and never retries mutation.
401 redirects to local auth, 403 returns a usable error, 409 emits api:conflict for refresh.
uiState exposes loading/empty/success/error via textContent, aria-live and focus.

Checks supplied: real JSP render/XSS/context-path/static asset assertions on Tomcat and
five isolated JavaScript behavior tests. Optional browser smoke uses the test container
route at 320/375/768/1440 and checks UTF-8, overflow, keyboard focus, UI states and CSP.
Navigation revocation with real memberships remains deferred. Evidence is in
docs/evidence/khanh/KHANH-11.md.
