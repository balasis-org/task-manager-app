// k6 test configuration — shared by both attack-simulations and stress tests.
// all users map to DataLoader-seeded dev accounts (fake-login only, no Azure AD).
// Override via: k6 run --env BASE_URL=https://<frontdoor>.azurefd.net script.js
// Defaults to localhost for local development against Docker Compose stack
export const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

// Stress header auth — WAF AllowStressHeaders rule requires both headers.
// The deployer generates the secrets and provides them to the tester.
// Usage: k6 run --env STRESS_KEY=<value> --env STRESS_NONCE=<value> ...
export const STRESS_KEY   = __ENV.STRESS_KEY   || "";
export const STRESS_NONCE = __ENV.STRESS_NONCE || "";

// One group leader per subscription tier
export const TIER_LEADERS = {
    FREE:      { email: "lena.dev@example.com",   name: "Lena Dev",   plan: "FREE" },
    STUDENT:   { email: "marco.dev@example.com",  name: "Marco Dev",  plan: "STUDENT" },
    ORGANIZER: { email: "nina.dev@example.com",   name: "Nina Dev",   plan: "ORGANIZER" },
    TEAM:      { email: "tomas.dev@example.com",  name: "Tomas Dev",  plan: "TEAM" },
};

// Group name per tier (seeded by DataLoader on startup)
export const TIER_GROUP_NAMES = {
    FREE:      "Free Tier Group",
    STUDENT:   "Student Tier Group",
    ORGANIZER: "Organizer Tier Group",
    TEAM:      "Team Tier Group",
};

// Regular members distributed across tier groups
export const MEMBER_USERS = [
    { email: "sofia.dev@example.com",  name: "Sofia Dev"  },
    { email: "peter.dev@example.com",  name: "Peter Dev"  },
    { email: "hanna.dev@example.com",  name: "Hanna Dev"  },
    { email: "erik.dev@example.com",   name: "Erik Dev"   },
    { email: "julia.dev@example.com",  name: "Julia Dev"  },
    { email: "ravi.dev@example.com",   name: "Ravi Dev"   },
    { email: "katya.dev@example.com",  name: "Katya Dev"  },
    { email: "leon.dev@example.com",   name: "Leon Dev"   },
];

// Flat list: leaders [0-3], then members [4-11]
export const CORE_USERS = [
    TIER_LEADERS.FREE,
    TIER_LEADERS.STUDENT,
    TIER_LEADERS.ORGANIZER,
    TIER_LEADERS.TEAM,
    ...MEMBER_USERS,
];

export const STRESS_USERS = generateStressUsers(38);

export const ALL_USERS = [...CORE_USERS, ...STRESS_USERS];

// dynamic user generation — creates a unique identity per VU index.
// DevAuthController auto-creates users on fake-login if they don't exist,
// so stress tests are no longer bounded by a predetermined user list.
export function dynamicUser(vuIndex) {
    if (vuIndex < ALL_USERS.length) return ALL_USERS[vuIndex];
    const num = String(vuIndex + 1).padStart(4, "0");
    return {
        email: `dynamic${num}.dev@example.com`,
        name:  `Dynamic${num} Dev`,
    };
}

function generateStressUsers(count) {
    const users = [];
    for (let i = 1; i <= count; i++) {
        const num = String(i).padStart(2, "0");
        users.push({
            email: `stress${num}.dev@example.com`,
            name:  `Stress${num} Dev`,
        });
    }
    return users;
}
