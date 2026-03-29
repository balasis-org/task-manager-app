// stress test: simulates many users polling has-changed endpoints concurrently.
// tracks 429 rate-limit responses vs successful polls. threshold: <50% rate-limited.
import http from "k6/http";
import { sleep } from "k6";
import { Counter, Rate } from "k6/metrics";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.1.0/index.js";
import { BASE_URL, ALL_USERS, TIER_LEADERS, dynamicUser } from "../config.js";
import {
    loginWithFakeCredentials,
    loginAsTierLeader,
    findTierGroupId,
    preJoinDynamicUsers,
    parseDurationToMilliseconds,
    enableWafBypass,
} from "../http-helpers.js";

// WAF bypass — arena-stress uses header-based access instead of IP whitelisting.
// Pass --env WAF_PASSPHRASE=<value> --env WAF_NONCE=<value> when running.
const WAF_PASSPHRASE = __ENV.WAF_PASSPHRASE || "";
const WAF_NONCE      = __ENV.WAF_NONCE      || "";
if (WAF_PASSPHRASE && WAF_NONCE) enableWafBypass(WAF_PASSPHRASE, WAF_NONCE);

const rateLimitedCounter = new Counter("rate_limited_429");
const pollSuccessCounter = new Counter("poll_success");
const rateLimitPercentage = new Rate("rate_limit_pct");

const VIRTUAL_USERS = parseInt(__ENV.VUS || "30", 10);
const DURATION = __ENV.DURATION || "2m";

export const options = {
    scenarios: {
        aggressive_poll: {
            executor:    "per-vu-iterations",
            vus:         VIRTUAL_USERS,
            iterations:  1,
            maxDuration: DURATION,
        },
    },
    thresholds: {
        rate_limit_pct:    ["rate<0.50"],
        http_req_duration: ["p(95)<3000"],
    },
};

export function setup() {
    const leaderCookies = loginAsTierLeader("TEAM");
    const groupId = findTierGroupId(leaderCookies, "TEAM");
    if (!groupId) {
        console.error("SETUP FAILED: Team Tier Group not found");
        return null;
    }
    preJoinDynamicUsers(groupId, leaderCookies, VIRTUAL_USERS);
    return { groupId };
}

export default function (data) {
    if (!data) return;
    const user = dynamicUser(__VU - 1);

    const cookies = loginWithFakeCredentials(user.email, user.name, user.plan);
    const groupId = data.groupId;

    let lastSeen = new Date().toISOString();
    const deadline = calculateDeadlineFromDuration(DURATION);

    while (Date.now() < deadline) {
        const wasRateLimited = pollHasChangedAndTrackRateLimit(groupId, lastSeen, cookies);
        if (wasRateLimited) continue;

        lastSeen = updateLastSeenIfContentChanged(lastSeen);
        pollPresenceAndTrackRateLimit(groupId, cookies);
        sleep(2);
    }

    logCompletionForFirstVirtualUser();
}

function calculateDeadlineFromDuration(duration) {
    return Date.now() + parseDurationToMilliseconds(duration);
}

function pollHasChangedAndTrackRateLimit(groupId, lastSeen, cookies) {
    const queryString = lastSeen ? `?lastSeen=${encodeURIComponent(lastSeen)}` : "";
    const response = http.get(`${BASE_URL}/groups/${groupId}/has-changed${queryString}`, {
        headers: { Cookie: cookies },
    });

    if (response.status === 429) {
        rateLimitedCounter.add(1);
        rateLimitPercentage.add(1);
        const retryAfter = response.headers["Retry-After"];
        sleep(retryAfter ? parseInt(retryAfter, 10) : 5);
        return true;
    }

    rateLimitPercentage.add(0);
    pollSuccessCounter.add(1);
    return false;
}

function updateLastSeenIfContentChanged(currentLastSeen) {
    return new Date().toISOString();
}

function pollPresenceAndTrackRateLimit(groupId, cookies) {
    const response = http.get(`${BASE_URL}/groups/${groupId}/presence`, {
        headers: { Cookie: cookies },
    });
    if (response.status === 429) {
        rateLimitedCounter.add(1);
    }
}

function logCompletionForFirstVirtualUser() {
    if (__VU === 1) {
        console.log("VU 1 finished aggressive polling.");
    }
}

export function handleSummary(data) {
    const ts = new Date().toISOString().replace(/[:.]/g, "-");
    const name = __ENV.TEST_NAME || "polling-load";
    return {
        [`../results/${name}-${ts}.json`]: JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: " ", enableColors: true }),
    };
}
