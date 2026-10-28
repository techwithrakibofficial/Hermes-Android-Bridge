import io.github.techwithrakibofficial.hermesbridge.AppCatalog;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** JVM concurrency tests with a fake slow package provider, not real Android APIs. */
public final class AppCatalogTest {
    private static int checks;
    private static void check(boolean pass, String label) {
        if (!pass) throw new AssertionError(label);
        checks++;
    }
    private static AppCatalog.Snapshot sample() {
        return new AppCatalog.Snapshot(Collections.singletonList(
            new AppCatalog.Entry("com.example.app", "বাংলা app")), false, 0);
    }
    public static void main(String[] args) throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<Thread> loaderThread = new AtomicReference<>();
        Thread caller = Thread.currentThread();
        try (AppCatalog catalog = new AppCatalog(() -> {
            calls.incrementAndGet(); loaderThread.set(Thread.currentThread());
            entered.countDown(); release.await(); return sample();
        })) {
            catalog.warm();
            check(entered.await(2, TimeUnit.SECONDS), "background provider started");
            check(loaderThread.get() != caller, "package provider never executes on the requesting thread");
            long before = System.nanoTime();
            check(catalog.read(30).error.equals("apps_loading"), "cold slow query returns loading instead of failed action");
            check(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before) < 1000, "read wait is bounded");
            check(catalog.state().equals("loading"), "slow-query diagnostic");
            check(catalog.read(30).error.equals("apps_loading"), "another read joins pending work");
            check(calls.get() == 1, "repeated reads do not multiply background package work");
            release.countDown();
            AppCatalog.Result result = catalog.read(2000);
            check(result.error == null && result.snapshot.apps.get(0).label.equals("বাংলা app"), "slow query can finish successfully");
            check(catalog.read(30).snapshot == result.snapshot, "warm cache reuses resolved labels");
            check(calls.get() == 1 && catalog.state().equals("ready"), "warm read performs no new package/label loads");
        } finally { release.countDown(); }

        AtomicInteger failedCalls = new AtomicInteger();
        try (AppCatalog failed = new AppCatalog(() -> {
            failedCalls.incrementAndGet(); throw new IllegalStateException("private exception must not leak");
        })) {
            AppCatalog.Result error = failed.read(2000);
            check(error.snapshot == null && error.error.equals("apps_query_failed"), "provider failures have sanitized specific code");
            check(failed.state().equals("failed"), "provider failure diagnostic");
            failed.read(30);
            check(failedCalls.get() == 1, "failed queries do not form an immediate retry loop");
        }

        AppCatalog closed = new AppCatalog(AppCatalogTest::sample);
        closed.close();
        check(closed.read(30).error.equals("bridge_stopped"), "closed session never starts a query");
        check(closed.state().equals("stopped"), "closed-session diagnostic");

        CountDownLatch started = new CountDownLatch(1), blocked = new CountDownLatch(1);
        AppCatalog stopping = new AppCatalog(() -> { started.countDown(); blocked.await(); return sample(); });
        ExecutorService reader = Executors.newSingleThreadExecutor();
        try {
            Future<AppCatalog.Result> waiting = reader.submit(() -> stopping.read(5000));
            check(started.await(2, TimeUnit.SECONDS), "in-flight read started");
            long before = System.nanoTime();
            stopping.close();
            check(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before) < 1000, "Stop does not wait for the loader lock");
            check(waiting.get(2, TimeUnit.SECONDS).error.equals("bridge_stopped"), "in-flight result revoked on Stop");
        } finally { blocked.countDown(); stopping.close(); reader.shutdownNow(); }

        AtomicInteger refreshCalls = new AtomicInteger();
        try (AppCatalog refresh = new AppCatalog(() -> { refreshCalls.incrementAndGet(); return sample(); }, 0)) {
            refresh.read(2000); refresh.read(2000);
            check(refreshCalls.get() == 2, "expired completed cache refreshes");
        }
        List<AppCatalog.Entry> many = new ArrayList<>();
        for (int i = 0; i < 151; i++) many.add(new AppCatalog.Entry("com.example.app" + i, "x".repeat(101)));
        AppCatalog.Snapshot limited = new AppCatalog.Snapshot(many, false, 0);
        many.clear();
        check(limited.apps.size() == 150 && limited.truncated, "snapshot cap enforced and copied");
        check(limited.apps.get(0).label.length() == 100, "label text bounded");
        try { limited.apps.clear(); throw new AssertionError("mutable snapshot"); }
        catch (UnsupportedOperationException expected) { checks++; }
        System.out.println("PASS: " + checks + " app-catalog checks. Android PackageManager NOT tested.");
    }
}
