/**
 * Shared configuration for all k6 stress-test scripts.
 *
 * BASE_URL — Backend base URL.  Override with:
 *   k6 run -e BASE_URL=https://myteamtasks-backend.azurewebsites.net scripts/presence-storm.js
 *
 * The default points at a local dev backend (Spring Boot).
 */

export const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

/**
 * Every fake-login call returns Set-Cookie headers for "jwt" and "refreshToken".
 * k6's http.cookieJar() captures them automatically per-VU, so subsequent requests
 * in the same VU carry the cookies without any manual header work.
 */

// Stress users: stress01 through stress38, all members of "Stress Test Group C"
// Alice (leader) + 11 core users + 38 stress users = 50 members total
export const CORE_USERS = [
    { email: "alice.dev@example.com",   name: "Alice Dev"   },
    { email: "bob.dev@example.com",     name: "Bob Dev"     },
    { email: "carol.dev@example.com",   name: "Carol Dev"   },
    { email: "dave.dev@example.com",    name: "Dave Dev"    },
    { email: "erin.dev@example.com",    name: "Erin Dev"    },
    { email: "frank.dev@example.com",   name: "Frank Dev"   },
    { email: "grace.dev@example.com",   name: "Grace Dev"   },
    { email: "heidi.dev@example.com",   name: "Heidi Dev"   },
    { email: "ivan.dev@example.com",    name: "Ivan Dev"    },
    { email: "judy.dev@example.com",    name: "Judy Dev"    },
    { email: "mallory.dev@example.com", name: "Mallory Dev" },
    { email: "oscar.dev@example.com",   name: "Oscar Dev"   },
];

export const STRESS_USERS = [];
for (let i = 1; i <= 38; i++) {
    const num = String(i).padStart(2, "0");
    STRESS_USERS.push({
        email: `stress${num}.dev@example.com`,
        name:  `Stress${num} Dev`,
    });
}

/** All 50 users that belong to Stress Test Group C */
export const ALL_USERS = [...CORE_USERS, ...STRESS_USERS];
