import { BASE_URL } from "./config.js";

const SEPARATOR = "================================================================";

let passCount = 0;
let failCount = 0;
let currentTestNumber = 0;

export function printAttackBanner(attackTitle, totalTests) {
    passCount = 0;
    failCount = 0;
    currentTestNumber = 0;
    console.log("\n" + SEPARATOR);
    console.log("  ATTACK: " + attackTitle);
    console.log("  Target: " + BASE_URL + "  |  Tests: " + totalTests);
    console.log(SEPARATOR + "\n");
}

export function printTestHeader(description) {
    currentTestNumber++;
    console.log("-- Test " + currentTestNumber + ": " + description + " --");
}

export function logRequest(method, path) {
    console.log("   > " + method + " " + path);
}

export function logResponse(status, body) {
    const truncatedBody = (body || "").substring(0, 120);
    console.log("   > " + status + "  " + truncatedBody);
}

export function logSetupStep(message) {
    console.log("   [setup] " + message);
}

export function logProgressTick(message) {
    console.log("   " + message);
}

export function markTestPassed(message) {
    passCount++;
    console.log("   [PASS] " + message + "\n");
}

export function markTestFailed(message) {
    failCount++;
    console.log("   [FAIL] " + message + "\n");
}

export function assertTestCondition(condition, passMessage, failMessage) {
    condition ? markTestPassed(passMessage) : markTestFailed(failMessage || passMessage);
}

export function printTestSummary() {
    const total = passCount + failCount;
    console.log(SEPARATOR);
    console.log("  RESULTS: " + passCount + "/" + total + " passed  |  " + failCount + " failed");
    console.log(SEPARATOR + "\n");
}
