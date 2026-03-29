// shared HTTP helpers for k6 tests â€” login, cookie extraction, group lookups,
// and typed request wrappers. all requests go through BASE_URL.
import http from "k6/http";
import { check } from "k6";
import { BASE_URL, DEV_AUTH_KEY, TIER_LEADERS, TIER_GROUP_NAMES, ALL_USERS, dynamicUser } from "./config.js";

// Module-level WAF bypass state — stress scripts opt in via enableWafBypass().
// Security tests never call it, so these stay empty and no WAF headers are sent.
let _wafPassphrase = "";
let _wafNonce = "";

// Call from stress script init to inject X-Stress-Key / X-Stress-Nonce on
// every request.  Security tests (arena-security) use IP whitelisting instead
// and never need this.
export function enableWafBypass(passphrase, nonce) {
    _wafPassphrase = passphrase;
    _wafNonce = nonce;
}

// Arena auth headers — injected into every outgoing request.
// Always includes X-Dev-Auth-Key (app-level gate for fake-login).
// Includes WAF bypass passphrases only when a stress script has opted in.
function arenaHeaders() {
    const hdrs = {};
    if (_wafPassphrase && _wafNonce) {
        hdrs["X-Stress-Key"] = _wafPassphrase;
        hdrs["X-Stress-Nonce"] = _wafNonce;
    }
    if (DEV_AUTH_KEY) {
        hdrs["X-Dev-Auth-Key"] = DEV_AUTH_KEY;
    }
    return hdrs;
}

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
        { headers: { "Content-Type": "application/json", ...arenaHeaders() } }
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
        headers: { Cookie: cookies, ...arenaHeaders() },
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
        headers: { Cookie: cookies, ...arenaHeaders() },
    });
    check(response, { "GET /groups 200": (r) => r.status === 200 });
    const groups = response.json();
    return (Array.isArray(groups) && groups.length > 0) ? groups[0].id : null;
}

export function postJsonPayload(path, body, cookies) {
    return http.post(`${BASE_URL}${path}`, JSON.stringify(body), {
        headers: { "Content-Type": "application/json", Cookie: cookies, ...arenaHeaders() },
        redirects: 0,
    });
}

export function postTaskPayload(path, taskData, cookies) {
    const payload = {
        data: http.file(JSON.stringify(taskData), "data.json", "application/json"),
    };
    return http.post(`${BASE_URL}${path}`, payload, {
        headers: { Cookie: cookies, ...arenaHeaders() },
        redirects: 0,
    });
}

export function postRawPayload(path, rawBody, cookies) {
    return http.post(`${BASE_URL}${path}`, rawBody, {
        headers: { "Content-Type": "application/json", Cookie: cookies, ...arenaHeaders() },
        redirects: 0,
    });
}

export function sendAuthenticatedGet(path, cookies) {
    return http.get(`${BASE_URL}${path}`, {
        headers: { Cookie: cookies, ...arenaHeaders() },
        redirects: 0,
    });
}

export function sendAuthenticatedDelete(path, cookies) {
    return http.request("DELETE", `${BASE_URL}${path}`, null, {
        headers: { Cookie: cookies, ...arenaHeaders() },
        redirects: 0,
    });
}

export function sendAuthenticatedPatch(path, cookies) {
    return http.request("PATCH", `${BASE_URL}${path}`, null, {
        headers: { Cookie: cookies, ...arenaHeaders() },
        redirects: 0,
    });
}

export function sendUnauthenticatedGet(url) {
    return http.get(url, { redirects: 0, headers: { ...arenaHeaders() } });
}

export function sendGetWithCustomCookies(path, cookieString) {
    return http.get(`${BASE_URL}${path}`, {
        redirects: 0,
        headers: { Cookie: cookieString, ...arenaHeaders() },
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

// pre-registers dynamic users (beyond the seeded 50) and joins them to the
// target group via the invitation flow. runs once in setup() before VUs start.
// DevAuthController auto-creates users on fake-login; this function then
// uses the invite+accept handshake so the new users become group members.
export function preJoinDynamicUsers(groupId, leaderCookies, vuCount) {
    const seeded = ALL_USERS.length;
    if (vuCount <= seeded) return;

    console.log(`Pre-joining ${vuCount - seeded} dynamic users to group ${groupId}...`);

    for (let i = seeded; i < vuCount; i++) {
        const user = dynamicUser(i);
        const userCookies = loginWithFakeCredentials(user.email, user.name);

        const meRes = http.get(`${BASE_URL}/users/me`, {
            headers: { Cookie: userCookies, ...arenaHeaders() },
        });
        if (meRes.status !== 200) {
            console.warn(`dynamic user ${i}: /users/me returned ${meRes.status}`);
            continue;
        }
        const inviteCode = meRes.json().inviteCode;
        if (!inviteCode) continue;

        http.post(
            `${BASE_URL}/groups/${groupId}/invite`,
            JSON.stringify({
                inviteCode: inviteCode,
                userToBeInvitedRole: "MEMBER",
                sendEmail: false,
            }),
            { headers: { "Content-Type": "application/json", Cookie: leaderCookies, ...arenaHeaders() } }
        );

        const invRes = http.get(`${BASE_URL}/group-invitations/me`, {
            headers: { Cookie: userCookies, ...arenaHeaders() },
        });
        if (invRes.status !== 200) continue;
        const invitations = invRes.json();
        const pending = Array.isArray(invitations)
            ? invitations.find(inv => inv.invitationStatus === "PENDING")
            : null;
        if (pending) {
            http.request(
                "PATCH",
                `${BASE_URL}/group-invitations/${pending.id}/status?status=ACCEPTED`,
                null,
                { headers: { Cookie: userCookies, ...arenaHeaders() } }
            );
        }
    }

    console.log(`Dynamic user pre-join complete.`);
}
