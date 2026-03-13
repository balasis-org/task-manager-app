import { check } from "k6";
import { CORE_USERS } from "../config.js";
import {
    loginWithFakeCredentials,
    findFirstAvailableGroupId,
    sendAuthenticatedGet,
    sendAuthenticatedDelete,
} from "../http-helpers.js";
import {
    printAttackBanner,
    printTestHeader,
    logRequest,
    logResponse,
    logSetupStep,
    assertTestCondition,
    printTestSummary,
} from "../test-logger.js";

export const options = {
    vus: 1,
    iterations: 1,
    thresholds: { checks: ["rate==1.00"] },
};

export default function () {
    printAttackBanner("IDOR / Resource Tampering", 5);

    const cookies = authenticateAsRegularMember();
    const groupId = locateStressTestGroupOrAbort(cookies);
    if (!groupId) return;

    const outsiderCookies = loginAsOutsiderProbe();

    verifyFabricatedFileIdDownloadIsBlocked(groupId, cookies);
    verifyDeletingOthersTaskIsBlocked(groupId, cookies);
    verifyAccessingTaskInPhantomGroupIsBlocked(cookies);
    verifyOutsiderCannotReadForeignGroupComments(groupId, outsiderCookies);
    verifyGuessedAssigneeFileDownloadIsBlocked(groupId, cookies);

    printTestSummary();
}

function authenticateAsRegularMember() {
    const user = CORE_USERS[4];
    logSetupStep("Logging in as " + user.email);
    return loginWithFakeCredentials(user.email, user.name, user.plan);
}

function locateStressTestGroupOrAbort(cookies) {
    const groupId = findFirstAvailableGroupId(cookies);
    if (!groupId) {
        console.log("   [SKIP] No groups found\n");
        return null;
    }
    logSetupStep("Target group ID: " + groupId);
    return groupId;
}

function loginAsOutsiderProbe() {
    const cookies = loginWithFakeCredentials("idor_probe@evil.com", "IDOR Probe");
    logSetupStep("Outsider session ready\n");
    return cookies;
}

function verifyFabricatedFileIdDownloadIsBlocked(groupId, cookies) {
    printTestHeader("Download with fabricated file ID (999999)");
    logRequest("GET", "/groups/" + groupId + "/task/1/files/999999/download");
    const response = sendAuthenticatedGet("/groups/" + groupId + "/task/1/files/999999/download", cookies);
    logResponse(response.status, response.body);
    const passed = check(response, { "fake file -> 4xx": (r) => r.status >= 400 && r.status < 500 });
    assertTestCondition(passed, "Fabricated file ID blocked (" + response.status + ")", "Expected 4xx, got " + response.status);
}

function verifyDeletingOthersTaskIsBlocked(groupId, cookies) {
    printTestHeader("Delete task #1 as non-owner non-admin");
    logRequest("DELETE", "/groups/" + groupId + "/task/1");
    const response = sendAuthenticatedDelete("/groups/" + groupId + "/task/1", cookies);
    logResponse(response.status, response.body);
    const passed = check(response, {
        "delete blocked": (r) => r.status === 403 || r.status === 404 || r.status === 401,
    });
    assertTestCondition(passed, "Delete blocked (" + response.status + ")", "Expected 403/404, got " + response.status);
}

function verifyAccessingTaskInPhantomGroupIsBlocked(cookies) {
    printTestHeader("Access task in phantom group (ID 999999)");
    logRequest("GET", "/groups/999999/task/1");
    const response = sendAuthenticatedGet("/groups/999999/task/1", cookies);
    logResponse(response.status, response.body);
    const passed = check(response, {
        "phantom group -> 4xx": (r) => r.status >= 400 && r.status < 500,
    });
    assertTestCondition(passed, "Phantom group blocked (" + response.status + ")", "Expected 4xx, got " + response.status);
}

function verifyOutsiderCannotReadForeignGroupComments(groupId, outsiderCookies) {
    printTestHeader("Outsider reads comments on foreign group");
    logRequest("GET", "/groups/" + groupId + "/task/1/comments");
    const response = sendAuthenticatedGet("/groups/" + groupId + "/task/1/comments?page=0&size=5", outsiderCookies);
    logResponse(response.status, response.body);
    const passed = check(response, {
        "foreign comments -> 4xx": (r) => r.status === 403 || r.status === 404 || r.status === 401,
    });
    assertTestCondition(passed, "Foreign comment access blocked (" + response.status + ")", "Expected 403/404, got " + response.status);
}

function verifyGuessedAssigneeFileDownloadIsBlocked(groupId, cookies) {
    printTestHeader("Download with guessed assignee-file ID (999999)");
    logRequest("GET", "/groups/" + groupId + "/task/1/assignee-files/999999/download");
    const response = sendAuthenticatedGet("/groups/" + groupId + "/task/1/assignee-files/999999/download", cookies);
    logResponse(response.status, response.body);
    const passed = check(response, { "guessed file -> 4xx": (r) => r.status >= 400 && r.status < 500 });
    assertTestCondition(passed, "Guessed assignee file blocked (" + response.status + ")", "Expected 4xx, got " + response.status);
}
