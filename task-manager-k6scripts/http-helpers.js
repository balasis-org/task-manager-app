import http from "k6/http";
import { check } from "k6";
import { BASE_URL, TIER_LEADERS, TIER_GROUP_NAMES } from "./config.js";

export function extractCookiesFromResponse(response) {
    const setCookieHeader = response.headers["Set-Cookie"];
    if (!setCookieHeader) return "";
    return setCookieHeader
        .split("\n")
        .map(line => line.split(";")[0].trim())
        .join("; ");
}

export function loginWithFakeCredentials(email, name, plan) {
    const body = { email, name };
    if (plan) body.subscriptionPlan = plan;
    const response = http.post(
        `${BASE_URL}/auth/fake-login`,
        JSON.stringify(body),
        { headers: { "Content-Type": "application/json" } }
    );
    check(response, { "login 200": (r) => r.status === 200 });
    return extractCookiesFromResponse(response);
}

export function loginAsTierLeader(tier) {
    const leader = TIER_LEADERS[tier];
    return loginWithFakeCredentials(leader.email, leader.name, leader.plan);
}

export function findGroupByName(cookies, groupName) {
    const response = http.get(`${BASE_URL}/groups`, {
        headers: { Cookie: cookies },
    });
    check(response, { "GET /groups 200": (r) => r.status === 200 });
    const groups = response.json();
    const match = groups.find(
        g => g.n === groupName || g.name === groupName
    );
    return match ? match.id : null;
}

export function findTierGroupId(cookies, tier) {
    return findGroupByName(cookies, TIER_GROUP_NAMES[tier]);
}

export function findFirstAvailableGroupId(cookies) {
    const response = http.get(`${BASE_URL}/groups`, {
        headers: { Cookie: cookies },
    });
    check(response, { "GET /groups 200": (r) => r.status === 200 });
    const groups = response.json();
    return (Array.isArray(groups) && groups.length > 0) ? groups[0].id : null;
}

export function postJsonPayload(path, body, cookies) {
    return http.post(`${BASE_URL}${path}`, JSON.stringify(body), {
        headers: { "Content-Type": "application/json", Cookie: cookies },
        redirects: 0,
    });
}

export function postRawPayload(path, rawBody, cookies) {
    return http.post(`${BASE_URL}${path}`, rawBody, {
        headers: { "Content-Type": "application/json", Cookie: cookies },
        redirects: 0,
    });
}

export function sendAuthenticatedGet(path, cookies) {
    return http.get(`${BASE_URL}${path}`, {
        headers: { Cookie: cookies },
        redirects: 0,
    });
}

export function sendAuthenticatedDelete(path, cookies) {
    return http.request("DELETE", `${BASE_URL}${path}`, null, {
        headers: { Cookie: cookies },
        redirects: 0,
    });
}

export function sendAuthenticatedPatch(path, cookies) {
    return http.request("PATCH", `${BASE_URL}${path}`, null, {
        headers: { Cookie: cookies },
        redirects: 0,
    });
}

export function sendUnauthenticatedGet(url) {
    return http.get(url, { redirects: 0 });
}

export function sendGetWithCustomCookies(path, cookieString) {
    return http.get(`${BASE_URL}${path}`, {
        redirects: 0,
        headers: { Cookie: cookieString },
    });
}

export function parseDurationToMilliseconds(durationString) {
    const match = durationString.match(/^(\d+)(s|m|h)$/);
    if (!match) return 120_000;
    const value = parseInt(match[1], 10);
    switch (match[2]) {
        case "s": return value * 1_000;
        case "m": return value * 60_000;
        case "h": return value * 3_600_000;
        default:  return 120_000;
    }
}

export function buildRepeatString(character, count) {
    let result = "";
    for (let i = 0; i < count; i++) result += character;
    return result;
}
