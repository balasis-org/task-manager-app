package io.github.balasis.taskmanager.engine.monitoring.profiling;

import java.util.ArrayList;
import java.util.List;

// Thread-local timing accumulator for request profiling.
// The controller aspect calls begin()/end(), inner layer aspects call pushLayer()/popLayer().
// end() returns a formatted tree of all layer timings for that request.
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

    // --- lifecycle ---

    /** Starts profiling for this request thread. */
    public static void begin(String controllerName, String methodName) {
        CURRENT.set(new ProfilingContext(controllerName, methodName));
    }

    /** Finishes profiling and returns the formatted timing tree (or null). */
    public static String end() {
        ProfilingContext ctx = CURRENT.get();
        if (ctx == null) return null;
        CURRENT.remove();

        long totalMs = System.currentTimeMillis() - ctx.startTimeMs;
        return ctx.formatTree(totalMs);
    }

    /** True if profiling is active on this thread. */
    public static boolean isActive() {
        return CURRENT.get() != null;
    }

    // --- recording ---

    // enters a profiled layer, nesting under the current parent if any
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

    /** Leaves a profiled layer, recording elapsed time. */
    public static void popLayer() {
        ProfilingContext ctx = CURRENT.get();
        if (ctx == null || ctx.entryStack.isEmpty()) return;

        TimingEntry entry = ctx.entryStack.removeLast();
        entry.durationMs = System.currentTimeMillis() - entry.startTimeMs;
    }

    // --- tree formatting ---

    private String formatTree(long totalMs) {
        String header = controllerName + "." + methodName + " (total: " + totalMs + "ms)";
        String separator = "=".repeat(header.length() + 6);

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== ").append(header).append(" ===\n");

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

        // pad with dots so timing values line up
        int dotsNeeded = Math.max(2, 22 - (indent.length() + label.length()));
        String dots = " " + ".".repeat(dotsNeeded) + " ";

        // right-align duration (pad to 6 chars)
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

    // --- inner timing entry ---

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
