# k6 Test Scripts

load testing and security attack simulations for the task manager application, written for [k6](https://grafana.com/docs/k6/latest/).


## structure

**stress/** — load and performance tests:
- `polling-load.js` — simulates concurrent users hitting the polling endpoints
- `download-storm.js` — concurrent file downloads through Front Door
- `presence-storm.js` — presence/activity status flood

**attack-simulations/** — security-focused tests that verify the application rejects malicious input:
- `01-auth-bypass.js` — attempts to bypass authentication
- `02-rate-limit-hammer.js` — floods requests to trigger and verify rate limiting
- `03-input-abuse.js` — long strings, special characters, boundary values
- `04-unauthorized-access.js` — accessing resources without proper permissions
- `05-upload-header-pollution.js` — manipulated headers on file upload requests
- `06-idor-resource-tampering.js` — attempts to access other users' resources by ID
- `07-xss-html-injection.js` — XSS payloads in various input fields
- `08-session-manipulation.js` — tampered cookies and tokens
- `09-privilege-escalation.js` — attempts to perform admin actions as a regular user


## shared modules

- `config.js` — base URL, thresholds, and shared configuration
- `http-helpers.js` — common HTTP request wrappers
- `test-logger.js` — structured logging for test output


## running

```bash
# install k6: https://grafana.com/docs/k6/latest/set-up/install-k6/

# run a single script
k6 run stress/polling-load.js

# run with custom options
k6 run --vus 50 --duration 30s stress/polling-load.js
```

edit `config.js` to point at the correct base URL (local or Azure Front Door endpoint).
