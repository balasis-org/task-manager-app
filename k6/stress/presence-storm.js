import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Counter, Gauge } from "k6/metrics";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.1.0/index.js";
import { BASE_URL, ALL_USERS, TIER_LEADERS, dynamicUser } from "../config.js";
import {
    loginWithFakeCredentials,
    loginAsTierLeader,
    findTierGroupId,
    sendAuthenticatedGet,
    preJoinDynamicUsers,
    parseDurationToMilliseconds,
} from "../http-helpers.js";

const presenceOnlineCount = new Gauge("presence_online_count");
const heartbeatOkCounter = new Counter("heartbeat_ok");
const heartbeatFailCounter = new Counter("heartbeat_fail");
const presenceLatencyTrend = new Trend("presence_latency_ms");

const VIRTUAL_USERS = parseInt(__ENV.VUS || "50", 10);
const DURATION = __ENV.DURATION || "3m";
const POLL_INTERVAL_SECONDS = parseInt(__ENV.POLL_SEC || "30", 10);

export const options = {
    scenarios: {
        presence: {
            executor:    "per-vu-iterations",
            vus:         VIRTUAL_USERS,
            iterations:  1,
            maxDuration: DURATION,
        },
    },
    thresholds: {
        http_req_failed:       ["rate<0.05"],
        http_req_duration:     ["p(95)<2000"],
        presence_latency_ms:   ["p(95)<1500"],
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
    logLoginDetailsForFirstVirtualUser(cookies);

    const groupId = data.groupId;

    loadInitialGroupDetail(groupId, cookies);
    let lastSeen = new Date().toISOString();
    const deadline = calculateDeadlineFromDuration(DURATION);

    while (Date.now() < deadline) {
        lastSeen = pollHasChangedAndTrackHeartbeat(groupId, lastSeen, cookies);
        pollPresenceAndTrackLatency(groupId, cookies);
        sleep(POLL_INTERVAL_SECONDS);
    }
}

function logLoginDetailsForFirstVirtualUser(cookies) {
    if (__VU === 1) {
        console.log("[VU 1] Cookies: " + cookies.substring(0, 80) + "...");
    }
}

function loadInitialGroupDetail(groupId, cookies) {
    const response = http.get(`${BASE_URL}/groups/${groupId}`, {
        headers: { Cookie: cookies },
    });
    check(response, { "group detail 200": (r) => r.status === 200 });
}

function calculateDeadlineFromDuration(duration) {
    return Date.now() + parseDurationToMilliseconds(duration);
}

function pollHasChangedAndTrackHeartbeat(groupId, lastSeen, cookies) {
    const queryString = lastSeen ? `?lastSeen=${encodeURIComponent(lastSeen)}` : "";
    const response = http.get(`${BASE_URL}/groups/${groupId}/has-changed${queryString}`, {
        headers: { Cookie: cookies },
    });

    if (response.status === 204 || response.status === 409) {
        heartbeatOkCounter.add(1);
        if (response.status === 409) return new Date().toISOString();
    } else {
        heartbeatFailCounter.add(1);
        logUnexpectedHeartbeatForFirstVirtualUser(response);
    }

    return lastSeen;
}

function logUnexpectedHeartbeatForFirstVirtualUser(response) {
    if (__VU === 1) {
        console.log("[VU 1] has-changed unexpected: " + response.status + " " + response.body);
    }
}

function pollPresenceAndTrackLatency(groupId, cookies) {
    const startTime = Date.now();
    const response = http.get(`${BASE_URL}/groups/${groupId}/presence`, {
        headers: { Cookie: cookies },
    });
    presenceLatencyTrend.add(Date.now() - startTime);

    if (response.status === 200) {
        parseAndTrackOnlineUserCount(response);
    }
}

function parseAndTrackOnlineUserCount(response) {
    try {
        const onlineIds = response.json();
        if (Array.isArray(onlineIds)) {
            presenceOnlineCount.add(onlineIds.length);
            if (__VU === 1) console.log(`[tick] ${onlineIds.length} users online`);
        }
    } catch (_) {}
}

export function handleSummary(data) {
    const ts = new Date().toISOString().replace(/[:.]/g, "-");
    const name = __ENV.TEST_NAME || "presence-storm";
    return {
        [`../results/${name}-${ts}.json`]: JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: " ", enableColors: true }),
    };
}
