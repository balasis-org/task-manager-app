// stress test: simulates many users polling has-changed endpoints concurrently.
// tracks 429 rate-limit responses vs successful polls. threshold: <50% rate-limited.
import http from "k6/http";
import { sleep } from "k6";
import { Counter, Rate } from "k6/metrics";
import { BASE_URL, ALL_USERS } from "../config.js";
import {
    loginWithFakeCredentials,
    findTierGroupId,
    parseDurationToMilliseconds,
} from "../http-helpers.js";

const rateLimitedCounter = new Counter("rate_limited_429");
const pollSuccessCounter = new Counter("poll_success");
const rateLimitPercentage = new Rate("rate_limit_pct");

const VIRTUAL_USERS = parseInt(__ENV.VUS || "30", 10);
const DURATION = __ENV.DURATION || "2m";

export const options = {
    scenarios: {
        aggressive_poll: {
            executor:    "per-vu-iterations",
            vus:         Math.min(VIRTUAL_USERS, ALL_USERS.length),
            iterations:  1,
            maxDuration: DURATION,
        },
    },
    thresholds: {
        rate_limit_pct:    ["rate<0.50"],
        http_req_duration: ["p(95)<3000"],
    },
};

export default function () {
    const user = ALL_USERS[__VU - 1];

    const cookies = loginWithFakeCredentials(user.email, user.name, user.plan);
    const groupId = findTeamTierGroupOrAbort(cookies);
    if (!groupId) return;

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

function findTeamTierGroupOrAbort(cookies) {
    const groupId = findTierGroupId(cookies, "TEAM");
    if (!groupId) console.error(`VU ${__VU}: Team Tier Group not found!`);
    return groupId;
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
