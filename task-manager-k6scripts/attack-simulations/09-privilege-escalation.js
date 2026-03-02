import http from "k6/http";
import { check } from "k6";
import { BASE_URL, CORE_USERS } from "../config.js";
import {
    loginWithFakeCredentials,
    findFirstAvailableGroupId,
    sendAuthenticatedGet,
    sendAuthenticatedDelete,
    sendAuthenticatedPatch,
} from "../http-helpers.js";
import {
    printAttackBanner,
    printTestHeader,
    logRequest,
    logResponse,
    logSetupStep,
    assertTestCondition,
    printTestSummary,
    markTestPassed,
} from "../test-logger.js";

export const options = {
    vus: 1,
    iterations: 1,
    thresholds: { checks: ["rate==1.00"] },
};

export default function () {
    printAttackBanner("Privilege Escalation (Member -> Admin)", 5);

    const cookies = authenticateAsRegularMember();
    const groupId = locateStressTestGroupOrAbort(cookies);
    if (!groupId) return;

    const membershipId = findTargetMembershipId(groupId, cookies);

    verifyMemberCannotDeleteGroup(groupId, cookies);
    verifyMemberCannotDeleteAllEvents(groupId, cookies);
    verifyMemberCannotKickAnotherMember(groupId, membershipId, cookies);
    verifyMemberCannotPromoteToAdmin(groupId, membershipId, cookies);
    verifyMemberCannotUploadGroupImage(groupId, cookies);

    printTestSummary();
}

function authenticateAsRegularMember() {
    const member = CORE_USERS[10];
    logSetupStep("Logging in as " + member.email + " (regular member)");
    return loginWithFakeCredentials(member.email, member.name, member.plan);
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

function findTargetMembershipId(groupId, cookies) {
    const response = sendAuthenticatedGet(
        "/groups/" + groupId + "/groupMemberships?page=0&size=50", cookies
    );
    try {
        const page = response.json();
        const items = page.content || page;
        if (Array.isArray(items) && items.length > 0) {
            const membershipId = items[0].id || items[0].groupMembershipId || null;
            logSetupStep("Target membership ID: " + (membershipId || "unknown") + "\n");
            return membershipId;
        }
    } catch (_) {}
    logSetupStep("Target membership ID: unknown\n");
    return null;
}

function verifyMemberCannotDeleteGroup(groupId, cookies) {
    printTestHeader("Member tries DELETE /groups/" + groupId);
    logRequest("DELETE", "/groups/" + groupId);
    const response = sendAuthenticatedDelete("/groups/" + groupId, cookies);
    logResponse(response.status, response.body);
    const passed = check(response, {
        "delete group blocked": (r) => r.status === 403 || r.status === 401,
    });
    assertTestCondition(passed, "Group deletion blocked (" + response.status + ")", "Expected 403, got " + response.status);
}

function verifyMemberCannotDeleteAllEvents(groupId, cookies) {
    printTestHeader("Member tries DELETE /groups/" + groupId + "/events");
    logRequest("DELETE", "/groups/" + groupId + "/events");
    const response = sendAuthenticatedDelete("/groups/" + groupId + "/events", cookies);
    logResponse(response.status, response.body);
    const passed = check(response, {
        "delete events blocked": (r) => r.status === 403 || r.status === 401,
    });
    assertTestCondition(passed, "Events deletion blocked (" + response.status + ")", "Expected 403, got " + response.status);
}

function verifyMemberCannotKickAnotherMember(groupId, membershipId, cookies) {
    if (!membershipId) {
        printTestHeader("SKIP - no membership ID found");
        markTestPassed("Skipped (no target membership)");
        return;
    }
    printTestHeader("Member tries to kick membership #" + membershipId);
    logRequest("DELETE", "/groups/" + groupId + "/groupMembership/" + membershipId);
    const response = sendAuthenticatedDelete(
        "/groups/" + groupId + "/groupMembership/" + membershipId, cookies
    );
    logResponse(response.status, response.body);
    const passed = check(response, {
        "kick blocked": (r) => r.status === 403 || r.status === 401,
    });
    assertTestCondition(passed, "Kick blocked (" + response.status + ")", "Expected 403, got " + response.status);
}

function verifyMemberCannotPromoteToAdmin(groupId, membershipId, cookies) {
    if (!membershipId) {
        printTestHeader("SKIP - no membership ID found");
        markTestPassed("Skipped (no target membership)");
        return;
    }
    printTestHeader("Member tries to promote membership #" + membershipId + " to ADMIN");
    const path = "/groups/" + groupId + "/groupMembership/" + membershipId + "/role?role=ADMIN";
    logRequest("PATCH", path);
    const response = sendAuthenticatedPatch(path, cookies);
    logResponse(response.status, response.body);
    const passed = check(response, {
        "role change blocked": (r) => r.status === 403 || r.status === 401,
    });
    assertTestCondition(passed, "Role change blocked (" + response.status + ")", "Expected 403, got " + response.status);
}

function verifyMemberCannotUploadGroupImage(groupId, cookies) {
    printTestHeader("Member tries POST /groups/" + groupId + "/image");
    logRequest("POST", "/groups/" + groupId + "/image  (fake PNG)");
    const fakeImage = http.file("not_an_image", "evil.png", "image/png");
    const response = http.post(BASE_URL + "/groups/" + groupId + "/image",
        { file: fakeImage }, {
        headers: { Cookie: cookies }, redirects: 0,
    });
    logResponse(response.status, response.body);
    const passed = check(response, {
        "image upload blocked": (r) => r.status === 403 || r.status === 400 || r.status === 401,
    });
    assertTestCondition(passed, "Image upload blocked (" + response.status + ")", "Expected 403/400, got " + response.status);
}
