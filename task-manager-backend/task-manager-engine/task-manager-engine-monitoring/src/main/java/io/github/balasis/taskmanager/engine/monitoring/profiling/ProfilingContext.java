package io.github.balasis.taskmanager.engine.monitoring.profiling;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread-local accumulator that collects timing entries during a single HTTP request.
 * The controller aspect (outer ring) calls begin() at the start and end() when done.
 * Inner-layer aspects call record() to add their individual timing entries.
 *
 * All data lives on the current thread — no synchronization needed.
 *
 * After end() is called, formatTree() produces a human-readable console tree like:
 *
 *   ━━━ GroupController.getGroup (total: 47ms) ━━━
 *     service .............. 45ms  (GroupServiceImpl.getGroup)
 *       db ................. 12ms  (GroupRepository.findById)
 *       db .................  8ms  (GroupMembershipRepository.findByGroupIdAndUserId)
 *       auth ...............  2ms  (AuthorizationService.requireAnyRoleIn)
 *     validator ............  1ms  (ResourceDataValidator.validateResourceData)
 *   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 */
public final class ProfilingContext {

    private static final ThreadLocal<ProfilingContext> CURRENT = new ThreadLocal<>();

    private final String controllerName;
    private final String methodName;
    private final long startTimeMs;

    // Stack tracks nesting: when a service calls a repo, the repo entry
    // becomes a child of the service entry. We track depth via this stack.
    private final List<TimingEntry> rootEntries = new ArrayList<>();
    private final List<TimingEntry> entryStack = new ArrayList<>();

    private ProfilingContext(String controllerName, String methodName) {
        this.controllerName = controllerName;
        this.methodName = methodName;
        this.startTimeMs = System.currentTimeMillis();
    }

    // ── Lifecycle (called by ControllerProfilingAspect) ──────────────────

    /** Start profiling for the current request. Called once per controller method. */
    public static void begin(String controllerName, String methodName) {
        CURRENT.set(new ProfilingContext(controllerName, methodName));
    }

    /** Finish profiling and return the formatted tree. Returns null if not active. */
    public static String end() {
        ProfilingContext ctx = CURRENT.get();
        if (ctx == null) return null;
        CURRENT.remove();

        long totalMs = System.currentTimeMillis() - ctx.startTimeMs;
        return ctx.formatTree(totalMs);
    }

    /** Check if profiling is active on this thread. */
    public static boolean isActive() {
        return CURRENT.get() != null;
    }

    // ── Recording (called by LayerProfilingAspect) ───────────────────────

    /**
     * Called when entering a profiled layer (service, db, blob, etc).
     * Pushes a new entry onto the stack so nested calls become children.
     */
    public static void pushLayer(String layerLabel, String className, String methodName) {
        ProfilingContext ctx = CURRENT.get();
        if (ctx == null) return;

        TimingEntry entry = new TimingEntry(layerLabel, className, methodName);

        // If there's a parent on the stack, add as child; otherwise it's a root entry
        if (!ctx.entryStack.isEmpty()) {
            ctx.entryStack.getLast().children.add(entry);
        } else {
            ctx.rootEntries.add(entry);
        }

        ctx.entryStack.add(entry);
    }

    /** Called when leaving a profiled layer. Records the elapsed time. */
    public static void popLayer() {
        ProfilingContext ctx = CURRENT.get();
        if (ctx == null || ctx.entryStack.isEmpty()) return;

        TimingEntry entry = ctx.entryStack.removeLast();
        entry.durationMs = System.currentTimeMillis() - entry.startTimeMs;
    }

    // ── Tree formatting ──────────────────────────────────────────────────

    private String formatTree(long totalMs) {
        String header = controllerName + "." + methodName + " (total: " + totalMs + "ms)";
        String separator = "━".repeat(header.length() + 6);

        StringBuilder sb = new StringBuilder();
        sb.append("\n━━━ ").append(header).append(" ━━━\n");

        for (TimingEntry entry : rootEntries) {
            appendEntry(sb, entry, 1);
        }

        sb.append(separator).append("\n");
        return sb.toString();
    }

    private void appendEntry(StringBuilder sb, TimingEntry entry, int depth) {
        String indent = "  ".repeat(depth);
        String label = entry.layerLabel;
        String description = entry.className + "." + entry.methodName;
        String durationStr = entry.durationMs + "ms";

        // Pad the label with dots to align timing values — makes the tree readable
        int dotsNeeded = Math.max(2, 22 - (indent.length() + label.length()));
        String dots = " " + ".".repeat(dotsNeeded) + " ";

        // Right-align the duration (pad to 6 chars so small numbers line up)
        String paddedDuration = String.format("%6s", durationStr);

        sb.append(indent)
          .append(label)
          .append(dots)
          .append(paddedDuration)
          .append("  (").append(description).append(")")
          .append("\n");

        for (TimingEntry child : entry.children) {
            appendEntry(sb, child, depth + 1);
        }
    }

    // ── Inner timing entry ───────────────────────────────────────────────

    private static class TimingEntry {
        final String layerLabel;    // e.g. "service", "db", "blob", "redis", "auth", "validator", "email", "safety"
        final String className;     // e.g. "GroupServiceImpl"
        final String methodName;    // e.g. "getGroup"
        final long startTimeMs;
        long durationMs;
        final List<TimingEntry> children = new ArrayList<>();

        TimingEntry(String layerLabel, String className, String methodName) {
            this.layerLabel = layerLabel;
            this.className = className;
            this.methodName = methodName;
            this.startTimeMs = System.currentTimeMillis();
        }
    }
}
