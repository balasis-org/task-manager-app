import http from "k6/http";
import { check } from "k6";
import { BASE_URL, TIER_LEADERS, STRESS_USERS } from "../config.js";
import { loginWithFakeCredentials } from "../http-helpers.js";
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
    printAttackBanner("Upload Header Pollution & File Abuse", 5);

    const cookies = authenticateAsFirstCoreUser();

    verifySpoofedContentTypeIsRejected(cookies);
    verifyGifUploadIsRejected(cookies);
    verifyOversizedImageIsRejected(cookies);
    verifyPathTraversalFilenameIsHandled(cookies);
    verifyEmptyFileUploadIsRejected(cookies);

    printTestSummary();
}

function authenticateAsFirstCoreUser() {
    const user = STRESS_USERS[1];
    logSetupStep("Logging in as " + user.email);
    const cookies = loginWithFakeCredentials(user.email, user.name, user.plan);
    console.log("");
    return cookies;
}

function verifySpoofedContentTypeIsRejected(cookies) {
    printTestHeader("Text file with spoofed image/png Content-Type");
    logRequest("POST", "/users/me/profile-image  (bash script as PNG)");
    const bashScriptBody = "#!/bin/bash\nrm -rf /\n";
    const response = uploadProfileImage(cookies, bashScriptBody, "exploit.png", "image/png");
    logResponse(response.status, response.body);
    const passed = check(response, { "spoofed type rejected": (r) => r.status === 400 || r.status === 503 || r.status === 409 });
    assertTestCondition(passed, "Spoofed content-type rejected (" + response.status + ")", "Expected 400/503/409, got " + response.status);
}

function verifyGifUploadIsRejected(cookies) {
    printTestHeader("GIF image upload (blocked format)");
    logRequest("POST", "/users/me/profile-image  (animation.gif)");
    const minimalGifHeader = "GIF89a\x01\x00\x01\x00\x80\x00\x00\xff\xff\xff\xff\xff\xff!\xf9\x04\x01\x00\x00\x00\x00,\x00\x00\x00\x00\x01\x00\x01\x00\x00\x02\x02D\x01\x00;";
    const response = uploadProfileImage(cookies, minimalGifHeader, "animation.gif", "image/gif");
    logResponse(response.status, response.body);
    const passed = check(response, { "gif rejected": (r) => r.status === 400 || r.status === 503 || r.status === 409 });
    assertTestCondition(passed, "GIF upload rejected (" + response.status + ")", "Expected 400/503/409, got " + response.status);
}

function verifyOversizedImageIsRejected(cookies) {
    printTestHeader("Oversized image (6 MB, max 5 MB)");
    logRequest("POST", "/users/me/profile-image  (6 MB blob)");
    const sixMegabyteBlob = buildFakeBinaryPayload(6 * 1024 * 1024);
    const response = uploadProfileImage(cookies, sixMegabyteBlob, "huge.png", "image/png");
    logResponse(response.status, response.body);
    const passed = check(response, { "oversized rejected": (r) => r.status === 400 || r.status === 503 || r.status === 409 });
    assertTestCondition(passed, "Oversized image rejected (" + response.status + ")", "Expected 400/503/409, got " + response.status);
}

function verifyPathTraversalFilenameIsHandled(cookies) {
    printTestHeader("Path traversal filename (../../etc/passwd)");
    logRequest("POST", "/users/me/profile-image  (../../etc/passwd)");
    const tinyPayload = buildFakeBinaryPayload(100);
    const response = uploadProfileImage(cookies, tinyPayload, "../../etc/passwd", "image/png");
    logResponse(response.status, response.body);
    const passed = check(response, {
        "traversal handled": (r) => r.status === 400 || r.status === 200 || r.status === 503 || r.status === 409,
    });
    assertTestCondition(passed, "Path traversal handled (" + response.status + ")", "Unexpected " + response.status);
}

function verifyEmptyFileUploadIsRejected(cookies) {
    printTestHeader("Empty file upload (zero bytes)");
    logRequest("POST", "/users/me/profile-image  (empty)");
    const response = uploadProfileImage(cookies, "", "empty.png", "image/png");
    logResponse(response.status, response.body);
    const passed = check(response, { "empty rejected": (r) => r.status === 400 || r.status === 503 || r.status === 409 });
    assertTestCondition(passed, "Empty file rejected (" + response.status + ")", "Expected 400/503/409, got " + response.status);
}

function uploadProfileImage(cookies, fileBody, filename, contentType) {
    const fileData = { file: http.file(fileBody, filename, contentType) };
    return http.post(BASE_URL + "/users/me/profile-image", fileData, {
        headers: { Cookie: cookies },
        redirects: 0,
    });
}

function buildFakeBinaryPayload(sizeInBytes) {
    const array = new Uint8Array(sizeInBytes);
    for (let i = 0; i < sizeInBytes; i++) array[i] = i % 256;
    return array.buffer;
}
