# Shared HTTP contract

HttpResponses emits `{data: ...}` or `{error:{code,message,correlationId}}`.
Decimals are strings, Instants are UTC, and `data:null` is retained. GET page routes
default to HTML; explicit Accept application/json with nonzero quality selects JSON.
JSON-only routes and POST errors always return JSON.

RequestParsers rejects malformed canonical UUIDs, invalid page/pageSize/sort and money
before a query. Pagination is 1-based, default 20, max 100; impossible integer offsets
are rejected. Query owners must append the ID tie-breaker to their allowed ordering.

Correlation IDs are safe ASCII, max 64 characters; unsafe values are replaced.
Unexpected errors expose a generic 500. Server logging includes exception type and
correlation, not SQL messages, request bodies, cookies or secrets.

Filter order: correlation → errors → headers → CSRF → body validation → authentication.
All POST requests need session CSRF. The contracted VNPAY IPN is GET and must use
Liêm's signature verification/protocol response; there is no exempt POST payment path.

Identity POST bodies are JSON objects, max 64 KiB, depth 32, string size 16 KiB.
Duplicate keys, trailing JSON, forged identity/principal fields and unsupported media
types fail before the controller. Multipart routes belong to Đông's upload validation.

UI state helpers use textContent and aria-live. A network mutation error is returned
to the caller for status reconciliation; apiClient never retries POST automatically.

Unimplemented M1 routes deliberately return 501 rather than a fabricated success.
