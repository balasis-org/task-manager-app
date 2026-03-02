import { check } from "k6";
import { CORE_USERS } from "../config.js";
import {
    loginWithFakeCredentials,
    findFirstAvailableGroupId,
    postJsonPayload,
    postRawPayload,
    buildRepeatString,
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
    printAttackBanner("Input Abuse (Validation Bypass)", 6);

    const cookies = authenticateAsFirstCoreUser();
    const groupId = locateTestTargetGroup(cookies);

    verifyBlankGroupNameIsRejected(cookies);
    verifyOversizedGroupNameIsRejected(cookies);
    verifyBlankTaskTitleIsRejected(groupId, cookies);
    verifyOutOfRangePriorityIsRejected(groupId, cookies);
    verifyOversizedDescriptionIsRejected(groupId, cookies);
    verifyMalformedJsonBodyIsRejected(cookies);

    printTestSummary();
}

function authenticateAsFirstCoreUser() {
    const user = CORE_USERS[0];
    logSetupStep("Logging in as " + user.email);
    return loginWithFakeCredentials(user.email, user.name, user.plan);
}

function locateTestTargetGroup(cookies) {
    const groupId = findFirstAvailableGroupId(cookies);
    logSetupStep("Group ID: " + (groupId || "none") + "\n");
    return groupId || 1;
}

function verifyBlankGroupNameIsRejected(cookies) {
    printTestHeader("Blank group name");
    logRequest("POST", '/groups  { name: "" }');
    const response = postJsonPayload("/groups", { name: "", description: "test" }, cookies);
    logResponse(response.status, response.body);
    const passed = check(response, { "blank name -> 400": (r) => r.status === 400 });
    assertTestCondition(passed, "Blank name rejected", "Expected 400, got " + response.status);
}

function verifyOversizedGroupNameIsRejected(cookies) {
    printTestHeader("Oversized group name (51 chars, max 50)");
    logRequest("POST", "/groups  { name: 51x'A' }");
    const response = postJsonPayload("/groups", { name: buildRepeatString("A", 51), description: "test" }, cookies);
    logResponse(response.status, response.body);
    const passed = check(response, { "oversized name -> 400": (r) => r.status === 400 });
    assertTestCondition(passed, "Oversized name rejected", "Expected 400, got " + response.status);
}

function verifyBlankTaskTitleIsRejected(groupId, cookies) {
    printTestHeader("Blank task title");
    logRequest("POST", "/groups/" + groupId + '/tasks  { title: "" }');
    const response = postJsonPayload("/groups/" + groupId + "/tasks", {
        title: "", description: "some desc", priority: 5,
    }, cookies);
    logResponse(response.status, response.body);
    const passed = check(response, { "blank title -> 400": (r) => r.status === 400 });
    assertTestCondition(passed, "Blank title rejected", "Expected 400, got " + response.status);
}

function verifyOutOfRangePriorityIsRejected(groupId, cookies) {
    printTestHeader("Priority = 99 (valid: 0-10)");
    logRequest("POST", "/groups/" + groupId + "/tasks  { priority: 99 }");
    const response = postJsonPayload("/groups/" + groupId + "/tasks", {
        title: "Evil Task", description: "valid desc", priority: 99,
    }, cookies);
    logResponse(response.status, response.body);
    const passed = check(response, { "priority 99 -> 400": (r) => r.status === 400 });
    assertTestCondition(passed, "Out-of-range priority rejected", "Expected 400, got " + response.status);
}

function verifyOversizedDescriptionIsRejected(groupId, cookies) {
    printTestHeader("Oversized description (1501 chars, max 1500)");
    logRequest("POST", "/groups/" + groupId + "/tasks  { desc: 1501x'B' }");
    const response = postJsonPayload("/groups/" + groupId + "/tasks", {
        title: "Evil Task", description: buildRepeatString("B", 1501), priority: 5,
    }, cookies);
    logResponse(response.status, response.body);
    const passed = check(response, { "oversized desc -> 400": (r) => r.status === 400 });
    assertTestCondition(passed, "Oversized desc rejected", "Expected 400, got " + response.status);
}

function verifyMalformedJsonBodyIsRejected(cookies) {
    printTestHeader("Non-JSON body to JSON endpoint");
    logRequest("POST", '/groups  body: "{not valid json!!}"');
    const response = postRawPayload("/groups", "{not valid json!!}", cookies);
    logResponse(response.status, response.body);
    const passed = check(response, { "malformed body -> 400": (r) => r.status === 400 });
    assertTestCondition(passed, "Malformed JSON rejected", "Expected 400, got " + response.status);
}
