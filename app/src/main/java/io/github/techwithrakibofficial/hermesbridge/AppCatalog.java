package io.github.techwithrakibofficial.hermesbridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/** One background package query at a time. Never wait on a lock held by the loader. */
public final class AppCatalog implements AutoCloseable {
    public interface Loader { Snapshot load() throws Exception; }
    public static final class Entry {
        public final String packageName;
        public final String label;
        public Entry(String packageName, String label) {
            this.packageName = packageName;
            this.label = label.length() > 100 ? label.substring(0, 100) : label;
        }
    }
    public static final class Snapshot {
        public final List<Entry> apps;
        public final boolean truncated;
        public final int labelFallbacks;
        public Snapshot(List<Entry> apps, boolean truncated, int labelFallbacks) {
            this.apps = Collections.unmodifiableList(new ArrayList<>(apps.subList(0, Math.min(150, apps.size()))));
            this.truncated = truncated || apps.size() > 150;
            this.labelFallbacks = labelFallbacks;
        }
    }
    public static final class Result {
        public final Snapshot snapshot;
        public final String error;
        private Result(Snapshot snapshot, String error) { this.snapshot = snapshot; this.error = error; }
    }
    private final Loader loader;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "hermes-app-catalog");
        thread.setDaemon(true);
        return thread;
    });
    private final long cacheNanos;
    private Future<Snapshot> pending;
    private long submittedAt;
    private boolean closed;

    public AppCatalog(Loader loader) { this(loader, TimeUnit.SECONDS.toNanos(30)); }
    public AppCatalog(Loader loader, long cacheNanos) { this.loader = loader; this.cacheNanos = cacheNanos; }

    private synchronized Future<Snapshot> query() {
        if (closed) return null;
        long now = System.nanoTime();
        if (pending == null || (pending.isDone() && now - submittedAt >= cacheNanos)) {
            submittedAt = now;
            pending = executor.submit(loader::load);
        }
        return pending;
    }
    public void warm() { query(); }
    public Result read(long timeoutMillis) {
        Future<Snapshot> task = query();
        if (task == null) return new Result(null, "bridge_stopped");
        try {
            Snapshot value = task.get(timeoutMillis, TimeUnit.MILLISECONDS);
            synchronized (this) {
                return closed ? new Result(null, "bridge_stopped") : new Result(value, null);
            }
        } catch (TimeoutException e) {
            // Keep the single read-only query alive; a later read joins it, not a second loader.
            return new Result(null, "apps_loading");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(null, "apps_query_interrupted");
        } catch (ExecutionException e) {
            return new Result(null, "apps_query_failed"); // Never expose raw Android exceptions.
        } catch (CancellationException e) {
            return new Result(null, "bridge_stopped");
        }
    }
    public synchronized String state() {
        if (closed) return "stopped";
        if (pending == null) return "idle";
        if (!pending.isDone()) return "loading";
        try { pending.get(); return "ready"; }
        catch (Exception e) { return "failed"; }
    }
    @Override public synchronized void close() {
        closed = true;
        if (pending != null) pending.cancel(true);
        executor.shutdownNow();
    }
}
