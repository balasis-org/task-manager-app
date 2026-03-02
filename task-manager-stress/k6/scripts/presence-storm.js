
import { sleep } from "k6";
import { Trend, Counter, Gauge } from "k6/metrics";
import { ALL_USERS } from "../config.js";
import { fakeLogin, getGroups, findGroupByName, hasChanged, getPresence, getGroupDetail } from "./helpers.js";

// ── Custom metrics ──────────────────────────────────────────────────
const presenceCount   = new Gauge("presence_online_count");
const heartbeatOk     = new Counter("heartbeat_ok");
const heartbeatFail   = new Counter("heartbeat_fail");
const presenceLatency = new Trend("presence_latency_ms");

// ── Options ─────────────────────────────────────────────────────────
const VUS      = parseInt(__ENV.VUS      || "50", 10);
const DURATION = __ENV.DURATION          || "3m";
const POLL_SEC = parseInt(__ENV.POLL_SEC || "30", 10);

export const options = {
    scenarios: {
        presence: {
            executor:    "per-vu-iterations",
            vus:         Math.min(VUS, ALL_USERS.length),
            iterations:  1,           // each VU runs the default function once
            maxDuration: DURATION,
        },
    },
    thresholds: {
        http_req_failed:       ["rate<0.05"],            // <5% HTTP errors
        http_req_duration:     ["p(95)<2000"],           // p95 under 2s
        presence_latency_ms:   ["p(95)<1500"],           // presence p95 under 1.5s
    },
};

// ── Main VU function ────────────────────────────────────────────────
export default function () {
    const user = ALL_USERS[__VU - 1];

    // 1. Authenticate
    const loginData = fakeLogin(user.email, user.name);
    const userId = loginData.userId;

    // 2. Find "Stress Test Group C"
    const groups = getGroups();
    const stressGroup = findGroupByName(groups, "Stress Test Group C");
    if (!stressGroup) {
        console.error(`VU ${__VU}: Stress Test Group C not found!`);
        return;
    }
    const groupId = stressGroup.id;

    // 3. load group detail once to get a serverNow timestamp for lastSeen
    const detail = getGroupDetail(groupId);
    //    The backend returns an ISO timestamp we can use for has-changed polling
    let lastSeen = new Date().toISOString();

    // 4. poll loop — mimics the frontend GroupProvider cycle
    const durationMs = parseDuration(DURATION);
    const deadline = Date.now() + durationMs;

    while (Date.now() < deadline) {
        // 4a. has-changed (piggybacks heartbeat on the backend)
        const hcRes = hasChanged(groupId, lastSeen);
        if (hcRes.status === 204 || hcRes.status === 409) {
            heartbeatOk.add(1);
            if (hcRes.status === 409) {
                // Something changed — in a real frontend we  reload detail
                lastSeen = new Date().toISOString();
            }
        } else {
            heartbeatFail.add(1);
        }

        // 4b. Fetch presence
        const t0 = Date.now();
        const onlineIds = getPresence(groupId);
        presenceLatency.add(Date.now() - t0);

        if (Array.isArray(onlineIds)) {
            presenceCount.add(onlineIds.length);
            if (__VU === 1) {
                // Only VU 1 logs to avoid console flood
                console.log(`[tick] ${onlineIds.length} users online`);
            }
        }

        sleep(POLL_SEC);
    }
}

// ── Helpers ─────────────────────────────────────────────────────────

function parseDuration(str) {
    const match = str.match(/^(\d+)(s|m|h)$/);
    if (!match) return 180_000; // default 3m
    const val = parseInt(match[1], 10);
    switch (match[2]) {
        case "s": return val * 1_000;
        case "m": return val * 60_000;
        case "h": return val * 3_600_000;
        default:  return 180_000;
    }
}
