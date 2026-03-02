/**
 * helpers.js — Shared utility functions for k6 scripts.
 */

import http from "k6/http";
import { check } from "k6";
import { BASE_URL } from "../config.js";

/**
 * Authenticate a dev user via the fake-login endpoint.
 * k6 cookie jar captures jwt + refreshToken cookies automatically.
 *
 * @param {string} email
 * @param {string} name
 * @returns {{ userId: number }} parsed response body
 */
export function fakeLogin(email, name) {
    const res = http.post(
        `${BASE_URL}/api/auth/fake-login`,
        JSON.stringify({ email, name }),
        { headers: { "Content-Type": "application/json" } }
    );

    check(res, {
        "login status 200": (r) => r.status === 200,
    });

    return res.json();
}

/**
 * Fetch the list of groups the currently-authenticated user belongs to.
 * Returns the parsed JSON array.
 */
export function getGroups() {
    const res = http.get(`${BASE_URL}/api/groups`);
    check(res, {
        "GET /groups 200": (r) => r.status === 200,
    });
    return res.json();
}

/**
 * Find a group by name from the groups list.
 * Group DTO uses short key "n" for name.
 */
export function findGroupByName(groups, name) {
    return groups.find((g) => g.n === name || g.name === name);
}

/**
 * Hit the has-changed endpoint (which piggybacks a heartbeat).
 * Returns the HTTP response.
 */
export function hasChanged(groupId, lastSeen) {
    const qs = lastSeen
        ? `?lastSeen=${encodeURIComponent(lastSeen)}`
        : "";
    return http.get(`${BASE_URL}/api/groups/${groupId}/has-changed${qs}`);
}

/**
 * Fetch the presence list (array of user IDs currently online).
 */
export function getPresence(groupId) {
    const res = http.get(`${BASE_URL}/api/groups/${groupId}/presence`);
    check(res, {
        "presence 200": (r) => r.status === 200,
    });
    return res.json();
}

/**
 * Fetch full group detail (used once to obtain a valid lastSeen timestamp).
 * Group detail DTO uses short key "sn" for serverNow in the refresh endpoint,
 * but the full detail endpoint returns tasks under "tp".
 */
export function getGroupDetail(groupId) {
    const res = http.get(`${BASE_URL}/api/groups/${groupId}`);
    check(res, {
        "group detail 200": (r) => r.status === 200,
    });
    return res.json();
}
