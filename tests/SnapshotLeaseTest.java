import io.github.techwithrakibofficial.hermesbridge.SnapshotLease;

/** Pure JVM guard fixtures; real Accessibility execution is not simulated. */
public final class SnapshotLeaseTest {
    private static int checks;
    private static void check(boolean pass, String label) {
        if (!pass) throw new AssertionError(label);
        checks++;
    }
    private static SnapshotLease fresh() {
        return new SnapshotLease("snapshot-a", 1000, 42, "com.example.chat", "recipient-a-and-controls");
    }
    public static void main(String[] args) {
        SnapshotLease lease = fresh();
        check(lease.checkId("snapshot-a", 1001) == null, "matching snapshot allowed");
        for (int n = 0; n < 100; n++) lease.changed();
        check(lease.eventCount() == 100, "event storm recorded");
        check(lease.checkId("snapshot-a", 1002) == null, "noise does not invalidate ID");
        check(lease.checkContext("snapshot-a", 1003, 42, "com.example.chat", "recipient-a-and-controls") == null,
              "actual unchanged context passes after event storm");
        check(lease.checkContext("snapshot-a", 1003, 42, "com.example.chat", "recipient-b-and-controls")
              .equals("screen_context_changed_observe_again"), "recipient changes block despite same send control");
        check(fresh().checkContext("snapshot-a", 1003, 42, "com.example.chat", "different")
              .equals("screen_context_changed_observe_again"), "context changes blocked without events");
        check(fresh().checkContext("snapshot-a", 1003, 43, "com.example.chat", "recipient-a-and-controls")
              .equals("screen_window_changed_observe_again"), "new window blocked");
        check(fresh().checkContext("snapshot-a", 1003, 42, "com.other.app", "recipient-a-and-controls")
              .equals("screen_window_changed_observe_again"), "other package blocked");
        check(fresh().checkId("wrong-id", 1003).equals("snapshot_id_mismatch_observe_again"), "wrong ID specific error");
        check(fresh().checkId("snapshot-a", 60_999) == null, "valid before expiry");
        check(fresh().checkId("snapshot-a", 61_000).equals("snapshot_expired_observe_again"), "exact expiry blocked");
        check(fresh().checkId("snapshot-a", 999).equals("snapshot_expired_observe_again"), "backward time blocked");
        lease.consume();
        check(lease.checkId("snapshot-a", 1004).equals("snapshot_consumed_observe_again"), "single use blocks replay");
        check(lease.checkContext("snapshot-a", 1004, 42, "com.example.chat", "recipient-a-and-controls")
              .equals("snapshot_consumed_observe_again"), "context match cannot resurrect consumed snapshot");
        SnapshotLease replacement = new SnapshotLease("snapshot-b", 2000, 42, "com.example.chat", "context");
        check(replacement.checkId("snapshot-a", 2001).equals("snapshot_id_mismatch_observe_again"), "new capture rejects old ID");
        check(replacement.checkId("snapshot-b", 2001) == null, "new ID works");
        check(SnapshotLease.checkTarget(true, "button", "button", true, true, false) == null, "exact target allowed");
        check(SnapshotLease.checkTarget(false, "button", "button", true, true, false)
              .equals("target_changed_observe_again"), "identical label cannot substitute different node");
        check(SnapshotLease.checkTarget(true, "old", "new", true, true, false)
              .equals("target_changed_observe_again"), "reused node changed content blocked");
        check(SnapshotLease.checkTarget(true, "button", "button", false, true, false)
              .equals("target_changed_observe_again"), "hidden node blocked");
        check(SnapshotLease.checkTarget(true, "button", "button", true, false, false)
              .equals("target_changed_observe_again"), "disabled node blocked");
        check(SnapshotLease.checkTarget(true, "field", "field", true, true, true)
              .equals("password_target_forbidden"), "password field blocked");
        System.out.println("PASS: " + checks + " snapshot-guard checks. Android Accessibility execution NOT tested.");
    }
}
