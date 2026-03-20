package io.github.balasis.taskmanager.engine.core.test.integration;

import io.github.balasis.taskmanager.context.base.enumeration.SubscriptionPlan;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// verifies every atomic budget SQL operation in UserRepository against a real
// H2 database. these @Modifying @Query methods use a WHERE-clause guard
// (e.g. WHERE usedX + :cost <= :max) so the update silently does nothing
// when the budget is exhausted — no read-then-write race, no optimistic lock.
//
// em.clear() is called before every findById that follows a @Modifying query
// because those queries bypass JPA's first-level cache — without the clear,
// findById would return the stale in-memory entity instead of re-reading the row.
@DataJpaTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = AutowireMode.ALL)
class BudgetAtomicityIntegrationTest {

    private final UserRepository userRepository;
    private final EntityManager em;

    BudgetAtomicityIntegrationTest(UserRepository userRepository, EntityManager em) {
        this.userRepository = userRepository;
        this.em = em;
    }

    private User user;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        user = userRepository.save(User.builder()
                .azureKey("azure-budget-key")
                .email("budget@test.com")
                .name("Budget User")
                .subscriptionPlan(SubscriptionPlan.FREE)
                .usedStorageBytes(0)
                .usedDownloadBytesMonth(0)
                .usedEmailsMonth(0)
                .usedImageScansMonth(0)
                .usedTaskAnalysisCreditsMonth(0)
                .build());
    }

    // storage budget

    @Test
    void addStorageUsage_withinBudget_returns1() {
        int rows = userRepository.addStorageUsage(user.getId(), 500L, 1000L);
        assertEquals(1, rows);

        em.clear();
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(500L, updated.getUsedStorageBytes());
    }

    @Test
    void addStorageUsage_exactlyAtBudget_returns1() {
        int rows = userRepository.addStorageUsage(user.getId(), 1000L, 1000L);
        assertEquals(1, rows);

        em.clear();
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(1000L, updated.getUsedStorageBytes());
    }

    @Test
    void addStorageUsage_exceedsBudget_returns0_noChange() {
        userRepository.addStorageUsage(user.getId(), 900L, 1000L);

        int rows = userRepository.addStorageUsage(user.getId(), 200L, 1000L);
        assertEquals(0, rows);

        em.clear();
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(900L, updated.getUsedStorageBytes());
    }

    @Test
    void subtractStorageUsage_decrementsCorrectly() {
        userRepository.addStorageUsage(user.getId(), 500L, 1000L);

        userRepository.subtractStorageUsage(user.getId(), 200L);

        em.clear();
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(300L, updated.getUsedStorageBytes());
    }

    @Test
    void subtractStorageUsage_clampsToZero() {
        userRepository.addStorageUsage(user.getId(), 100L, 1000L);

        userRepository.subtractStorageUsage(user.getId(), 500L);

        em.clear();
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(0L, updated.getUsedStorageBytes());
    }

    // download budget

    @Test
    void addDownloadUsage_withinBudget_returns1() {
        int rows = userRepository.addDownloadUsage(user.getId(), 1024L, 5000L);
        assertEquals(1, rows);

        em.clear();
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(1024L, updated.getUsedDownloadBytesMonth());
    }

    @Test
    void addDownloadUsage_exceedsBudget_returns0() {
        userRepository.addDownloadUsage(user.getId(), 4500L, 5000L);

        int rows = userRepository.addDownloadUsage(user.getId(), 600L, 5000L);
        assertEquals(0, rows);

        em.clear();
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(4500L, updated.getUsedDownloadBytesMonth());
    }

    // email quota

    @Test
    void incrementEmailUsage_withinQuota_returns1() {
        int rows = userRepository.incrementEmailUsage(user.getId(), 10);
        assertEquals(1, rows);

        em.clear();
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(1, updated.getUsedEmailsMonth());
    }

    @Test
    void incrementEmailUsage_atQuota_returns0() {
        for (int i = 0; i < 3; i++) {
            userRepository.incrementEmailUsage(user.getId(), 3);
        }

        int rows = userRepository.incrementEmailUsage(user.getId(), 3);
        assertEquals(0, rows);

        em.clear();
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(3, updated.getUsedEmailsMonth());
    }

    // image scan quota

    @Test
    void incrementImageScanUsage_withinLimit_returns1() {
        int rows = userRepository.incrementImageScanUsage(user.getId(), 5);
        assertEquals(1, rows);

        em.clear();
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(1, updated.getUsedImageScansMonth());
    }

    @Test
    void incrementImageScanUsage_atLimit_returns0() {
        for (int i = 0; i < 2; i++) {
            userRepository.incrementImageScanUsage(user.getId(), 2);
        }

        int rows = userRepository.incrementImageScanUsage(user.getId(), 2);
        assertEquals(0, rows);
    }

    @Test
    void decrementImageScanUsage_decrementsWhenPositive() {
        userRepository.incrementImageScanUsage(user.getId(), 5);

        int rows = userRepository.decrementImageScanUsage(user.getId());
        assertEquals(1, rows);

        em.clear();
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(0, updated.getUsedImageScansMonth());
    }

    @Test
    void decrementImageScanUsage_atZero_returns0() {
        int rows = userRepository.decrementImageScanUsage(user.getId());
        assertEquals(0, rows);
    }

    // task analysis credits

    @Test
    void incrementTaskAnalysisCredits_withinBudget_returns1() {
        int rows = userRepository.incrementTaskAnalysisCredits(user.getId(), 2, 10);
        assertEquals(1, rows);

        em.clear();
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(2, updated.getUsedTaskAnalysisCreditsMonth());
    }

    @Test
    void incrementTaskAnalysisCredits_exceedsBudget_returns0() {
        userRepository.incrementTaskAnalysisCredits(user.getId(), 8, 10);

        int rows = userRepository.incrementTaskAnalysisCredits(user.getId(), 5, 10);
        assertEquals(0, rows);

        em.clear();
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(8, updated.getUsedTaskAnalysisCreditsMonth());
    }

    @Test
    void decrementTaskAnalysisCredits_withinBalance_returns1() {
        userRepository.incrementTaskAnalysisCredits(user.getId(), 5, 10);

        int rows = userRepository.decrementTaskAnalysisCredits(user.getId(), 3);
        assertEquals(1, rows);

        em.clear();
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(2, updated.getUsedTaskAnalysisCreditsMonth());
    }

    @Test
    void decrementTaskAnalysisCredits_insufficientBalance_returns0() {
        userRepository.incrementTaskAnalysisCredits(user.getId(), 2, 10);

        int rows = userRepository.decrementTaskAnalysisCredits(user.getId(), 5);
        assertEquals(0, rows);

        em.clear();
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(2, updated.getUsedTaskAnalysisCreditsMonth());
    }

    // finder queries

    @Test
    void findByAzureKey_returnsMatchingUser() {
        Optional<User> found = userRepository.findByAzureKey("azure-budget-key");
        assertTrue(found.isPresent());
        assertEquals("budget@test.com", found.get().getEmail());
    }

    @Test
    void findByEmail_returnsMatchingUser() {
        Optional<User> found = userRepository.findByEmail("budget@test.com");
        assertTrue(found.isPresent());
        assertEquals("azure-budget-key", found.get().getAzureKey());
    }

    @Test
    void findByInviteCode_returnsUser() {
        User saved = userRepository.findById(user.getId()).orElseThrow();
        String code = saved.getInviteCode();
        assertNotNull(code);

        Optional<User> found = userRepository.findByInviteCode(code);
        assertTrue(found.isPresent());
        assertEquals(user.getId(), found.get().getId());
    }

    @Test
    void existsByAzureKey_returnsTrueForExisting() {
        assertTrue(userRepository.existsByAzureKey("azure-budget-key"));
        assertFalse(userRepository.existsByAzureKey("nonexistent"));
    }
}
