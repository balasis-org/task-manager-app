/**
 * polling-load.js
 * ─────────────────────────────────────────────────────────────────────
 * Simulates N concurrent users polling has-changed + presence as fast
 * as the rate limiter allows (40 req/min per user).  Unlike
 * presence-storm.js which mimics real 30-second poll intervals, this
 * script intentionally pushes the boundary to stress-test:
 *
 *   • The Bucket4j rate limiter (should return 429 beyond 40/min)
 *   • The database (has-changed queries hit the DB)
 *   • Redis (heartbeat ZADD + presence ZRANGEBYSCORE)
 *
 * Usage
 * ─────
 *   k6 run scripts/polling-load.js
 *   k6 run -e VUS=30 -e DURATION=2m scripts/polling-load.js
 *   k6 run -e BASE_URL=https://... -e VUS=10 scripts/polling-load.js
 *
 * ─────────────────────────────────────────────────────────────────────
 */

import { sleep } from "k6";
import { Counter, Rate } from "k6/metrics";
import { ALL_USERS } from "../config.js";
import { fakeLogin, getGroups, findGroupByName, hasChanged, getPresence } from "./helpers.js";

// ── Custom metrics ──────────────────────────────────────────────────
const rateLimited  = new Counter("rate_limited_429");
const pollSuccess  = new Counter("poll_success");
const rateLimitPct = new Rate("rate_limit_pct");

// ── Options ─────────────────────────────────────────────────────────
const VUS      = parseInt(__ENV.VUS      || "30", 10);
const DURATION = __ENV.DURATION          || "2m";

export const options = {
    scenarios: {
        aggressive_poll: {
            executor:    "per-vu-iterations",
            vus:         Math.min(VUS, ALL_USERS.length),
            iterations:  1,
            maxDuration: DURATION,
        },
    },
    thresholds: {
        rate_limit_pct:    ["rate<0.50"],            // expect some 429s but <50%
        http_req_duration: ["p(95)<3000"],           // p95 under 3s even under load
    },
};

// ── Main VU function ────────────────────────────────────────────────
export default function () {
    const user = ALL_USERS[__VU - 1];

    // 1. Authenticate
    fakeLogin(user.email, user.name);

    // 2. Find stress group
    const groups = getGroups();
    const stressGroup = findGroupByName(groups, "Stress Test Group C");
    if (!stressGroup) {
        console.error(`VU ${__VU}: Stress Test Group C not found!`);
        return;
    }
    const groupId = stressGroup.id;
    let lastSeen = new Date().toISOString();

    // 3. Aggressive poll loop — ~2 seconds between requests
    //    At 2s intervals each VU does ~30 req/min → just under the 40/min limit
    //    With 30 VUs this means 900 req/min total hitting the backend
    const durationMs = parseDuration(DURATION);
    const deadline = Date.now() + durationMs;

    while (Date.now() < deadline) {
        const hcRes = hasChanged(groupId, lastSeen);

        if (hcRes.status === 429) {
            rateLimited.add(1);
            rateLimitPct.add(1);
            // Respect Retry-After header if present
            const retryAfter = hcRes.headers["Retry-After"];
            sleep(retryAfter ? parseInt(retryAfter, 10) : 5);
            continue;
        }

        rateLimitPct.add(0);
        pollSuccess.add(1);

        if (hcRes.status === 409) {
            lastSeen = new Date().toISOString();
        }

        // Also fetch presence
        const presRes = getPresence(groupId);
        if (presRes && presRes.status === 429) {
            rateLimited.add(1);
        }

        sleep(2); // 2 seconds between poll rounds
    }

    if (__VU === 1) {
        console.log("VU 1 finished aggressive polling.");
    }
}

// ── Helpers ─────────────────────────────────────────────────────────

function parseDuration(str) {
    const match = str.match(/^(\d+)(s|m|h)$/);
    if (!match) return 120_000;
    const val = parseInt(match[1], 10);
    switch (match[2]) {
        case "s": return val * 1_000;
        case "m": return val * 60_000;
        case "h": return val * 3_600_000;
        default:  return 120_000;
    }
}
