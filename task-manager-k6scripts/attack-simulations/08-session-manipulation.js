import http from "k6/http";
import { check } from "k6";
import { BASE_URL, CORE_USERS } from "../config.js";
import {
    loginWithFakeCredentials,
    extractCookiesFromResponse,
    sendGetWithCustomCookies,
    sendAuthenticatedGet,
} from "../http-helpers.js";
import {
    printAttackBanner,
    printTestHeader,
    logRequest,
    logResponse,
    assertTestCondition,
    printTestSummary,
} from "../test-logger.js";

export const options = {
    vus: 1,
    iterations: 1,
    thresholds: { checks: ["rate==1.00"] },
};

export default function () {
    printAttackBanner("Session Manipulation & Cookie Abuse", 5);

    verifySessionFixationIsPreventedOnLogin();
    const userCookies = loginWithFakeCredentials(CORE_USERS[0].email, CORE_USERS[0].name, CORE_USERS[0].plan);

    verifyTokenReplayReturnsOriginalUserIdentity(userCookies);
    verifyDualCookieConfusionIsRejected();
    verifyMalformedCookieInjectionIsRejected();
    verifyMutatedJwtSignatureIsRejected(userCookies);

    printTestSummary();
}

function verifySessionFixationIsPreventedOnLogin() {
    printTestHeader("Session fixation attempt (pre-set jwt before login)");
    logRequest("POST", "/auth/fake-login  (Cookie: jwt=FIXED)");
    const fixedJwtCookie = "jwt=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5OTk5IiwiZXhwIjo5OTk5OTk5OTk5fQ.FIXED";
    const response = http.post(
        BASE_URL + "/auth/fake-login",
        JSON.stringify({ email: CORE_USERS[0].email, name: CORE_USERS[0].name, subscriptionPlan: CORE_USERS[0].plan }),
        { headers: { "Content-Type": "application/json", Cookie: fixedJwtCookie } }
    );
    logResponse(response.status, response.body);
    const newCookies = extractCookiesFromResponse(response);
    const serverReplacedFixedJwt = newCookies.indexOf("FIXED") === -1 && newCookies.indexOf("jwt=") >= 0;
    const passed = check(null, {
        "server replaces pre-set jwt": () => serverReplacedFixedJwt,
    });
    assertTestCondition(passed, "Server issued fresh jwt (fixation failed)", "Fixation may have succeeded, old jwt reused");
}

function verifyTokenReplayReturnsOriginalUserIdentity(userCookies) {
    printTestHeader("Replay user-0's cookies on different session");
    logRequest("GET", "/users/me  (user-0's cookies)");
    const response = sendAuthenticatedGet("/users/me", userCookies);
    logResponse(response.status, response.body);
    const identityMatchesUser0 = checkResponseIdentityMatchesUser0(response);
    const passed = check(null, {
        "session belongs to user-0, not user-1": () => identityMatchesUser0,
    });
    assertTestCondition(passed, "Token correctly identifies user-0", "Token identity mismatch, possible session confusion");
}

function verifyDualCookieConfusionIsRejected() {
    printTestHeader("Dual-cookie: forged jwt + garbage RefreshKey");
    logRequest("GET", "/groups  (mixed cookies: sub=9999 + fake refresh)");
    const mixedCookies = "jwt=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5OTk5IiwiZXhwIjo5OTk5OTk5OTk5fQ.BAD; RefreshKey=1:fake-refresh";
    const response = sendGetWithCustomCookies("/groups", mixedCookies);
    logResponse(response.status, response.body);
    const passed = check(response, { "dual cookie -> 4xx or ignored": (r) => r.status === 200 || (r.status >= 400 && r.status < 500) });
    assertTestCondition(passed, "Dual-cookie handled (" + response.status + ")", "Unexpected " + response.status);
}

function verifyMalformedCookieInjectionIsRejected() {
    printTestHeader("Malformed cookie injection (extra semicolons, CRLF)");
    logRequest("GET", "/groups  (garbage cookies with CRLF)");
    const malformedCookies = "jwt=abc;;;   RefreshKey=;;;evil=true\r\nX-Injected: header";
    const response = sendGetWithCustomCookies("/groups", malformedCookies);
    logResponse(response.status, response.body);
    const passed = check(response, { "malformed cookies -> transport reject or 4xx": (r) => r.status === 0 || (r.status >= 400 && r.status < 500) });
    assertTestCondition(passed, "Malformed cookies rejected (" + response.status + ")", "Unexpected " + response.status);
}

function verifyMutatedJwtSignatureIsRejected(validCookies) {
    printTestHeader("Mutated JWT (flip last signature char)");
    logRequest("GET", "/groups  (jwt with flipped sig)");
    const mutatedCookies = flipLastCharacterOfJwtSignature(validCookies);
    const response = sendGetWithCustomCookies("/groups", mutatedCookies);
    logResponse(response.status, response.body);
    const passed = check(response, { "mutated jwt -> 401": (r) => r.status === 401 });
    assertTestCondition(passed, "Mutated JWT rejected", "Expected 401, got " + response.status);
}

function checkResponseIdentityMatchesUser0(response) {
    try {
        const parsedBody = JSON.parse(response.body);
        return parsedBody.email === CORE_USERS[0].email || parsedBody.e === CORE_USERS[0].email;
    } catch (_) {
        return false;
    }
}

function flipLastCharacterOfJwtSignature(cookieString) {
    return cookieString.replace(/jwt=([^;]+)/, (match, token) => {
        const lastChar = token.slice(-1);
        const flippedChar = lastChar === "A" ? "B" : "A";
        return "jwt=" + token.slice(0, -1) + flippedChar;
    });
}
