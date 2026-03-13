import http from "k6/http";
import { check } from "k6";
import { Counter, Rate } from "k6/metrics";
import { BASE_URL, STRESS_USERS } from "../config.js";
import { loginWithFakeCredentials, sendAuthenticatedGet } from "../http-helpers.js";
import {
    printAttackBanner,
    printTestHeader,
    logRequest,
    logProgressTick,
    assertTestCondition,
    printTestSummary,
} from "../test-logger.js";

const TOTAL_REQUESTS = parseInt(__ENV.REQUESTS || "60", 10);

const blockedRequestCounter = new Counter("rate_limited_429");
const retryAfterHeaderRate = new Rate("retry_after_header_present");

export const options = {
    vus: 1,
    iterations: 1,
    thresholds: {
        rate_limited_429:           ["count>0"],
        retry_after_header_present: ["rate==1.00"],
    },
};

export default function () {
    printAttackBanner("Rate-Limit Hammer", 2);

    const cookies = authenticateAsFirstCoreUser();
    const stats = fireRapidRequestsAndCollectStatistics(cookies, TOTAL_REQUESTS);

    verifyRateLimiterWasTriggered(stats);
    verifyEveryBlockedResponseHasRetryAfterHeader(stats);

    printTestSummary();
}

function authenticateAsFirstCoreUser() {
    const user = STRESS_USERS[0];
    printTestHeader("Authenticate as " + user.email);
    const cookies = loginWithFakeCredentials(user.email, user.name, user.plan);
    logProgressTick("[PASS] Logged in\n");
    return cookies;
}

function fireRapidRequestsAndCollectStatistics(cookies, totalRequests) {
    printTestHeader("Rapid-fire " + totalRequests + " requests (limit: 40/min)");
    logRequest("GET", "/groups  (x" + totalRequests + ", no sleep)");
    console.log("");

    let okCount = 0;
    let blockedCount = 0;
    let firstBlockAtRequest = -1;
    let allBlockedHaveRetryAfter = true;

    for (let i = 1; i <= totalRequests; i++) {
        const response = sendAuthenticatedGet("/groups", cookies);

        if (response.status === 429) {
            blockedCount++;
            blockedRequestCounter.add(1);
            const retryAfterValue = response.headers["Retry-After"];
            retryAfterHeaderRate.add(retryAfterValue ? 1 : 0);
            if (!retryAfterValue) allBlockedHaveRetryAfter = false;
            if (firstBlockAtRequest < 0) {
                firstBlockAtRequest = i;
                logProgressTick(">>> RATE LIMIT at request #" + i +
                    "  Retry-After: " + (retryAfterValue || "MISSING") + "s");
            }
        } else {
            okCount++;
        }

        if (i % 10 === 0) {
            logProgressTick("... " + i + "/" + totalRequests + ": ok=" + okCount + " blocked=" + blockedCount);
        }
    }

    logProgressTick("\n   Tally: " + okCount + " ok | " + blockedCount + " blocked\n");

    return { okCount, blockedCount, firstBlockAtRequest, allBlockedHaveRetryAfter };
}

function verifyRateLimiterWasTriggered(stats) {
    const passed = check(null, {
        "rate limiter triggered": () => stats.blockedCount > 0,
    });
    assertTestCondition(passed,
        "Blocked " + stats.blockedCount + "/" + TOTAL_REQUESTS + " (first at #" + stats.firstBlockAtRequest + ")",
        "No 429s, rate limiter may be off");
}

function verifyEveryBlockedResponseHasRetryAfterHeader(stats) {
    const passed = check(null, {
        "every 429 has Retry-After": () => stats.allBlockedHaveRetryAfter,
    });
    assertTestCondition(passed, "All 429s include Retry-After", "Some 429s missing Retry-After");
}
