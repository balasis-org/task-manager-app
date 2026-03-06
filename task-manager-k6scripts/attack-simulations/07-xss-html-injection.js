import http from "k6/http";
import { check } from "k6";
import { BASE_URL, TIER_LEADERS } from "../config.js";
import {
    loginWithFakeCredentials,
    findFirstAvailableGroupId,
    postJsonPayload,
    postTaskPayload,
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

const XSS_PAYLOADS = {
    scriptTag:     "<script>alert('xss')</script>",
    imgOnerror:    "<img src=x onerror=alert(1)>",
    svgOnload:     "<svg/onload=alert('xss')>",
    eventHandler:  "' onfocus='alert(1)' autofocus='",
    unicodeEscape: "\u003cscript\u003ealert(1)\u003c/script\u003e",
};

export const options = {
    vus: 1,
    iterations: 1,
    thresholds: { checks: ["rate==1.00"] },
};

export default function () {
    printAttackBanner("XSS / HTML Injection", 6);

    const cookies = authenticateAsFirstCoreUser();
    const groupId = locateStressTestGroup(cookies);

    verifyScriptTagInGroupNameIsHandled(cookies);
    verifyImgOnerrorInTaskTitleIsHandled(groupId, cookies);
    verifySvgOnloadInTaskDescriptionIsHandled(groupId, cookies);
    verifyEventHandlerInjectionInGroupNameIsHandled(cookies);
    verifyUnicodeEscapedScriptInTaskTitleIsHandled(groupId, cookies);
    verifyInvisibleUnicodeCharsAreStrippedFromGroupName(cookies);

    printTestSummary();
}

function authenticateAsFirstCoreUser() {
    const user = TIER_LEADERS.ORGANIZER;
    logSetupStep("Logging in as " + user.email);
    return loginWithFakeCredentials(user.email, user.name, user.plan);
}

function locateStressTestGroup(cookies) {
    const groupId = findFirstAvailableGroupId(cookies);
    logSetupStep("Group ID: " + (groupId || "none") + "\n");
    return groupId || 1;
}

function verifyScriptTagInGroupNameIsHandled(cookies) {
    printTestHeader("<script> tag in group name");
    logRequest("POST", "/groups  { name: " + XSS_PAYLOADS.scriptTag + " }");
    const response = postJsonPayload("/groups", { name: XSS_PAYLOADS.scriptTag, description: "xss test" }, cookies);
    logResponse(response.status, response.body);
    const passed = check(response, {
        "script tag handled": (r) => r.status === 400 || r.status === 200,
    });
    assertTestCondition(passed, "Script tag handled (" + response.status + ")", "Unexpected response " + response.status);
}

function verifyImgOnerrorInTaskTitleIsHandled(groupId, cookies) {
    printTestHeader("<img onerror> in task title");
    logRequest("POST", "/groups/" + groupId + "/tasks");
    const response = postTaskPayload("/groups/" + groupId + "/tasks", {
        title: XSS_PAYLOADS.imgOnerror, description: "clean", priority: 3, taskState: "TODO",
    }, cookies);
    logResponse(response.status, response.body);
    const passed = check(response, {
        "img onerror handled": (r) => r.status === 400 || r.status === 200,
    });
    assertTestCondition(passed, "IMG onerror handled (" + response.status + ")", "Unexpected " + response.status);
}

function verifySvgOnloadInTaskDescriptionIsHandled(groupId, cookies) {
    printTestHeader("<svg onload> in task description");
    logRequest("POST", "/groups/" + groupId + "/tasks  { desc: svg payload }");
    const response = postTaskPayload("/groups/" + groupId + "/tasks", {
        title: "SVG test", description: XSS_PAYLOADS.svgOnload, priority: 1, taskState: "TODO",
    }, cookies);
    logResponse(response.status, response.body);
    const passed = check(response, {
        "svg onload handled": (r) => r.status === 400 || r.status === 200,
    });
    assertTestCondition(passed, "SVG onload handled (" + response.status + ")", "Unexpected " + response.status);
}

function verifyEventHandlerInjectionInGroupNameIsHandled(cookies) {
    printTestHeader("Event handler injection in group name");
    logRequest("POST", "/groups  { name: onfocus payload }");
    const response = postJsonPayload("/groups", { name: XSS_PAYLOADS.eventHandler, description: "xss test" }, cookies);
    logResponse(response.status, response.body);
    const passed = check(response, {
        "event handler handled": (r) => r.status === 400 || r.status === 200,
    });
    assertTestCondition(passed, "Event handler handled (" + response.status + ")", "Unexpected " + response.status);
}

function verifyUnicodeEscapedScriptInTaskTitleIsHandled(groupId, cookies) {
    printTestHeader("Unicode-escaped <script> in task title");
    logRequest("POST", "/groups/" + groupId + "/tasks  { title: unicode script }");
    const response = postTaskPayload("/groups/" + groupId + "/tasks", {
        title: XSS_PAYLOADS.unicodeEscape, description: "clean", priority: 3, taskState: "TODO",
    }, cookies);
    logResponse(response.status, response.body);
    const passed = check(response, {
        "unicode xss handled": (r) => r.status === 400 || r.status === 200,
    });
    assertTestCondition(passed, "Unicode XSS handled (" + response.status + ")", "Unexpected " + response.status);
}

function verifyInvisibleUnicodeCharsAreStrippedFromGroupName(cookies) {
    printTestHeader("Invisible unicode chars in group name");
    logRequest("POST", "/groups  { name: zero-width + separators }");
    const sneakyName = "Clean\u200BName\u2028With\uFEFFInvisibles";
    const response = postJsonPayload("/groups", { name: sneakyName, description: "invisible test" }, cookies);
    logResponse(response.status, response.body);
    let passed = check(response, {
        "invisible chars handled": (r) => r.status === 400 || r.status === 200,
    });
    if (response.status === 200) {
        const bodyText = response.body || "";
        const containsZeroWidthChars = bodyText.indexOf("\u200B") !== -1 || bodyText.indexOf("\uFEFF") !== -1;
        passed = check(null, { "invisible chars stripped": () => !containsZeroWidthChars });
    }
    assertTestCondition(passed, "Invisible chars stripped/handled (" + response.status + ")", "Invisible chars survived!");
}
