import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.1.0/index.js";
import { BASE_URL, ALL_USERS, TIER_LEADERS, dynamicUser } from "../config.js";
import {
    loginWithFakeCredentials,
    loginAsTierLeader,
    findTierGroupId,
    preJoinDynamicUsers,
    parseDurationToMilliseconds,
} from "../http-helpers.js";

// ────────────────────────────────────────────────────────────
// VU CONFIGURATION
//
// Default (30 VUs): regression baseline — ~50% of S1 DTU capacity.
//   Stable, repeatable. Code changes show as measurable delta.
//
// Full stress (50+ VUs): saturates Bucket4j-limited throughput.
//   Use for capacity ceiling discovery: k6 run -e VUS=100 download-storm.js
//   VUs beyond the 50 seeded users auto-create via DevAuthController.
// ────────────────────────────────────────────────────────────
const VIRTUAL_USERS = parseInt(__ENV.VUS || "30", 10);
const DURATION      = __ENV.DURATION || "3m";
const THINK_TIME    = parseFloat(__ENV.THINK_SEC || "3");
const FILE_SIZE_MB  = parseInt(__ENV.FILE_SIZE_MB || "10", 10);

const downloadOk       = new Counter("download_ok");
const downloadFail     = new Counter("download_fail");
const downloadLatency  = new Trend("download_latency_ms");
const rateLimited      = new Counter("rate_limited_429");
const rateLimitPct     = new Rate("rate_limit_pct");

export const options = {
    scenarios: {
        download_storm: {
            executor:    "per-vu-iterations",
            vus:         VIRTUAL_USERS,
            iterations:  1,
            maxDuration: DURATION,
        },
    },
    thresholds: {
        http_req_failed:      ["rate<0.10"],
        http_req_duration:    ["p(95)<5000"],
        download_latency_ms:  ["p(95)<4000"],
        rate_limit_pct:       ["rate<0.40"],
    },
};

/**
 * Generates a binary payload of the configured size.
 * Lazy-initialised: only the setup() VU allocates the buffer.
 * Avoids 10 MB × VU-count of wasted RAM in the download VUs.
 */
let _testFileBytes;
function getTestFileBytes() {
    if (!_testFileBytes) {
        const size = FILE_SIZE_MB * 1024 * 1024;
        const buf = new Uint8Array(size);
        for (let i = 0; i < size; i++) {
            buf[i] = i % 256;
        }
        _testFileBytes = buf.buffer;
    }
    return _testFileBytes;
}

/**
 * setup() runs ONCE before VUs start. Creates a task with an attached
 * file in the Team Tier Group so every VU has something to download.
 * Also pre-joins dynamic users (beyond the seeded 50) to the group.
 */
export function setup() {
    const leader = TIER_LEADERS.TEAM;
    const cookies = loginWithFakeCredentials(leader.email, leader.name, leader.plan);
    const groupId = findTierGroupId(cookies, "TEAM");

    if (!groupId) {
        console.error("SETUP FAILED: Team Tier Group not found");
        return null;
    }

    preJoinDynamicUsers(groupId, cookies, VIRTUAL_USERS);

    const taskData = {
        title: "Stress Download Task",
        description: "Auto-created by download-storm.js for stress testing",
    };

    const payload = {
        data: http.file(
            JSON.stringify(taskData),
            "data.json",
            "application/json"
        ),
        files: http.file(
            getTestFileBytes(),
            "stress-file.bin",
            "application/octet-stream"
        ),
    };

    const createRes = http.post(
        `${BASE_URL}/groups/${groupId}/tasks`,
        payload,
        { headers: { Cookie: cookies } }
    );

    const ok = check(createRes, {
        "task created 200": (r) => r.status === 200,
    });
    if (!ok) {
        console.error(`SETUP FAILED: task creation returned ${createRes.status}: ${createRes.body}`);
        return null;
    }

    const task = createRes.json();
    const fileId = task.files && task.files.length > 0
        ? task.files[0].id
        : null;

    if (!fileId) {
        console.error("SETUP FAILED: no file ID in task creation response");
        return null;
    }

    console.log(`Setup complete: groupId=${groupId}, taskId=${task.id}, fileId=${fileId}`);
    return { groupId, taskId: task.id, fileId };
}

/**
 * Each VU loops: authenticate → download → sleep → repeat.
 */
export default function (data) {
    if (!data) return;

    const user = dynamicUser(__VU - 1);
    const cookies = loginWithFakeCredentials(user.email, user.name, user.plan);
    const deadline = Date.now() + parseDurationToMilliseconds(DURATION);

    const downloadUrl =
        `${BASE_URL}/groups/${data.groupId}/task/${data.taskId}/files/${data.fileId}/download`;

    while (Date.now() < deadline) {
        const res = http.get(downloadUrl, {
            headers: { Cookie: cookies },
            responseType: "none",
        });

        if (res.status === 429) {
            rateLimited.add(1);
            rateLimitPct.add(1);
            const retryAfter = res.headers["Retry-After"];
            sleep(retryAfter ? parseInt(retryAfter, 10) : 5);
            continue;
        }

        rateLimitPct.add(0);

        if (res.status === 200) {
            downloadOk.add(1);
            downloadLatency.add(res.timings.duration);
        } else {
            downloadFail.add(1);
            if (__VU === 1) {
                console.warn(`VU 1: download returned ${res.status}`);
            }
        }

        sleep(THINK_TIME);
    }
}

export function handleSummary(data) {
    const ts = new Date().toISOString().replace(/[:.]/g, "-");
    const name = __ENV.TEST_NAME || "download-storm";
    return {
        [`../results/${name}-${ts}.json`]: JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: " ", enableColors: true }),
    };
}
