import { check } from "k6";
import { CORE_USERS } from "../config.js";
import {
    loginWithFakeCredentials,
    findFirstAvailableGroupId,
    sendAuthenticatedGet,
    postJsonPayload,
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
    printAttackBanner("Unauthorized Access (Broken Access Control)", 3);

    const targetGroupId = discoverTargetGroupAsLegitimateUser();
    if (!targetGroupId) return;

    const attackerCookies = loginAsOutsiderAttacker();

    verifyOutsiderGroupListIsEmpty(attackerCookies);
    verifyOutsiderCannotViewTargetGroup(targetGroupId, attackerCookies);
    verifyOutsiderCannotCreateTaskInTargetGroup(targetGroupId, attackerCookies);

    printTestSummary();
}

function discoverTargetGroupAsLegitimateUser() {
    const user = CORE_USERS[0];
    logSetupStep("Logging in as " + user.email + " to find target group");
    const legitCookies = loginWithFakeCredentials(user.email, user.name, user.plan);
    const targetGroupId = findFirstAvailableGroupId(legitCookies);
    if (!targetGroupId) {
        console.log("   [SKIP] No groups found for legitimate user\n");
        return null;
    }
    logSetupStep("Target group: " + targetGroupId);
    return targetGroupId;
}

function loginAsOutsiderAttacker() {
    const attackerEmail = "outsider_attacker@evil.com";
    logSetupStep("Switching to attacker: " + attackerEmail + "\n");
    return loginWithFakeCredentials(attackerEmail, "Outsider Attacker");
}

function verifyOutsiderGroupListIsEmpty(attackerCookies) {
    printTestHeader("Outsider /groups must be empty (no data leak)");
    logRequest("GET", "/groups");
    const response = sendAuthenticatedGet("/groups", attackerCookies);
    logResponse(response.status, response.body);
    let groups = [];
    try { groups = response.json(); } catch (_) {}
    const passed = check(response, {
        "outsider sees 0 groups": () => Array.isArray(groups) && groups.length === 0,
    });
    assertTestCondition(passed, "Outsider sees 0 groups", "Outsider saw " + groups.length + " groups, data leak!");
}

function verifyOutsiderCannotViewTargetGroup(targetGroupId, attackerCookies) {
    printTestHeader("Outsider GET /groups/" + targetGroupId);
    logRequest("GET", "/groups/" + targetGroupId);
    const response = sendAuthenticatedGet("/groups/" + targetGroupId, attackerCookies);
    logResponse(response.status, response.body);
    const passed = check(response, {
        "view blocked": (r) => r.status === 403 || r.status === 404 || r.status === 401,
    });
    assertTestCondition(passed, "View blocked (" + response.status + ")", "Expected 403/404, got " + response.status);
}

function verifyOutsiderCannotCreateTaskInTargetGroup(targetGroupId, attackerCookies) {
    printTestHeader("Outsider POST /groups/" + targetGroupId + "/tasks");
    logRequest("POST", "/groups/" + targetGroupId + "/tasks");
    const response = postJsonPayload("/groups/" + targetGroupId + "/tasks", {
        title: "Hacked Task", description: "Injected", priority: 5,
    }, attackerCookies);
    logResponse(response.status, response.body);
    const passed = check(response, {
        "create blocked": (r) => r.status === 403 || r.status === 404 || r.status === 401,
    });
    assertTestCondition(passed, "Task creation blocked (" + response.status + ")", "Expected 403/404, got " + response.status);
}
