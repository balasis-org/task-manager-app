import http from "k6/http";
import { check } from "k6";
import { BASE_URL } from "../config.js";
import { sendUnauthenticatedGet, sendGetWithCustomCookies } from "../http-helpers.js";
import {printAttackBanner, printTestHeader, logRequest,  logResponse,  assertTestCondition,  printTestSummary}
    from "../test-logger.js";

export const options = {   vus: 1,   iterations: 1,   thresholds: { checks: ["rate==1.00"] } };

export default function () {
    printAttackBanner("Authentication Bypass", 4);
    const targetUrl = BASE_URL + "/groups";

    verifyAnonymousRequestIsRejected(targetUrl);
    verifyForgedJwtIsRejected(targetUrl);
    verifyExpiredJwtIsRejected(targetUrl);
    verifyGarbageRefreshTokenIsRejected(targetUrl);

    printTestSummary();
}

function verifyAnonymousRequestIsRejected(targetUrl) {
    printTestHeader("Anonymous-No cookies request");
    logRequest("GET","/groups");
    const response = sendUnauthenticatedGet(targetUrl);
    logResponse(response.status,response.body);
    const passed = check(response,{ "anonymous -> 401 :": (r) => r.status === 401 });
    assertTestCondition(passed, "Rejected anonymous request", "Expected 401, got " + response.status);
}

function verifyForgedJwtIsRejected(targetUrl) {
    printTestHeader("Forged JWT with invalid HMAC signature");
    logRequest("GET", "/groups  (jwt=<forged>)");
    const forgedJwtCookie = "jwt=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5OTk5IiwiZXhwIjo5OTk5OTk5OTk5fQ.INVALID_SIG";
    const response = sendGetWithCustomCookies("/groups", forgedJwtCookie);
    logResponse(response.status, response.body);
    const passed = check(response, { "forged JWT -> 401": (r) => r.status === 401 });
    assertTestCondition(passed, "Rejected forged JWT", "Expected 401, got " + response.status);
}

function verifyExpiredJwtIsRejected(targetUrl) {
    printTestHeader("Expired JWT from Jan 2020");
    logRequest("GET", "/groups  (jwt=<expired>)");
    const expiredJwtCookie = "jwt=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZXhwIjoxNTc3ODM2ODAwfQ.BAD_SIG";
    const response = sendGetWithCustomCookies("/groups", expiredJwtCookie);
    logResponse(response.status, response.body);
    const passed = check(response, { "expired JWT -> 401": (r) => r.status === 401 });
    assertTestCondition(passed, "Rejected expired JWT", "Expected 401, got " + response.status);
}

function verifyGarbageRefreshTokenIsRejected(targetUrl) {
    printTestHeader("Garbage refresh-token cookie");
    logRequest("GET", "/groups  (RefreshKey=garbage)");
    const garbageRefreshCookie = "RefreshKey=999:garbage-value-not-a-real-token";
    const response = sendGetWithCustomCookies("/groups", garbageRefreshCookie);
    logResponse(response.status, response.body);
    const passed = check(response, { "bad refresh -> 401": (r) => r.status === 401 });
    assertTestCondition(passed, "Rejected garbage refresh token", "Expected 401, got " + response.status);
}
