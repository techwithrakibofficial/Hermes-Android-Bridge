package io.github.techwithrakibofficial.hermesbridge;

import java.util.Objects;

/** Single-use snapshot. Events require inspection, not automatic invalidation. */
public final class SnapshotLease {
    public static final long TTL_MS = 60_000;
    private final String id, packageName, context;
    private final long capturedAt;
    private final int windowId;
    private boolean consumed;
    private long events;

    public SnapshotLease(String id, long capturedAt, int windowId, String packageName, String context) {
        this.id = id;
        this.capturedAt = capturedAt;
        this.windowId = windowId;
        this.packageName = packageName;
        this.context = context;
    }
    public void changed() { if (events < Long.MAX_VALUE) events++; }
    public long eventCount() { return events; }
    public String checkId(String requested, long now) {
        if (consumed) return "snapshot_consumed_observe_again";
        if (!Objects.equals(id, requested)) return "snapshot_id_mismatch_observe_again";
        if (now < capturedAt || now - capturedAt >= TTL_MS) return "snapshot_expired_observe_again";
        return null;
    }
    public String checkContext(String requested, long now, int currentWindow, String currentPackage, String currentContext) {
        String error = checkId(requested, now);
        if (error != null) return error;
        if (windowId != currentWindow || !Objects.equals(packageName, currentPackage))
            return "screen_window_changed_observe_again";
        if (!Objects.equals(context, currentContext)) return "screen_context_changed_observe_again";
        return null;
    }
    public void consume() { consumed = true; }
    public static String checkTarget(boolean sameNode, String recorded, String current,
                                     boolean visible, boolean enabled, boolean password) {
        if (password) return "password_target_forbidden";
        if (!sameNode || !Objects.equals(recorded, current) || !visible || !enabled)
            return "target_changed_observe_again";
        return null;
    }
}
