export const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

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
