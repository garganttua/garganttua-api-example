package com.garganttua.api.example;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.garganttua.core.observability.EndEvent;
import com.garganttua.core.observability.ErrorEvent;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.observability.annotations.Observer;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Core-layer analog of the api {@code StatsObserver} — aggregates timing
 * stats per {@code ObservableEvent.source()} (e.g. {@code stage:create},
 * {@code script:business.CREATE_ONE}, {@code mapper:Tenant->TenantDto}).
 *
 * <p>Only {@link EndEvent} and {@link ErrorEvent} are recorded — they
 * carry a {@code duration}. Start events are skipped: the end event
 * pairs them and carries everything they have plus timing. Lock-free
 * via {@link ConcurrentHashMap} + atomic counters with CAS min/max.
 *
 * <p>Wired manually per-domain in {@code ExampleApplication.main} until
 * the api configures its captured {@code IObservabilityBuilder} with
 * {@code autoDetect(true)} + {@code withPackage(...)} — see
 * {@code ApiBuilder.provide(IObservabilityBuilder)}. Once that wiring
 * lands, this class can switch back to {@code @Observer} discovery
 * (the static {@code BUCKETS} are kept so either path remains valid).
 */
@Observer
@Reflected(queryAllDeclaredConstructors = true)
public final class CoreStatsObserver implements IObserver<ObservableEvent> {

    /**
     * Shared static state — the bootstrap auto-scan instantiates an
     * {@code @Observer} class via {@code IReflection.newInstance(...)},
     * yielding a different reference than a manually-{@code new}-ed one
     * held by the application. Routing all buckets through a static map
     * means both instances see the same aggregate and either can serve
     * as the read endpoint via {@link #snapshot()}.
     */
    private static final ConcurrentHashMap<String, Bucket> BUCKETS = new ConcurrentHashMap<>();

    @Override
    public void onEvent(ObservableEvent event) {
        switch (event) {
            case EndEvent e -> record(e.source(), e.duration(), true);
            case ErrorEvent e -> record(e.source(), e.duration(), false);
            default -> { /* ignore StartEvent — paired by EndEvent / ErrorEvent */ }
        }
    }

    private static void record(String source, Duration duration, boolean success) {
        if (source == null || duration == null) return;
        BUCKETS.computeIfAbsent(source, k -> new Bucket()).record(duration, success);
    }

    /**
     * Point-in-time snapshot keyed by {@code source}. Each bucket is read
     * atomically; the map as a whole is not — concurrent recordings may
     * produce a snapshot where some buckets advanced past others. Fine
     * for dashboards; not for cross-source invariants.
     */
    public Map<String, SourceStats> snapshot() {
        Map<String, SourceStats> result = new HashMap<>();
        BUCKETS.forEach((key, bucket) -> result.put(key, bucket.snapshot(key)));
        return Collections.unmodifiableMap(result);
    }

    /** Clears every bucket — handy in tests or for rolling windows. */
    public void reset() {
        BUCKETS.clear();
    }

    /**
     * Snapshot record for one source string. Mirrors api-layer
     * {@code OperationStats} but keyed by the core event {@code source}
     * (e.g. {@code stage:create}, {@code script:business.CREATE_ONE}).
     */
    public record SourceStats(
            String source,
            long count,
            long successCount,
            long failureCount,
            Duration totalDuration,
            Duration minDuration,
            Duration maxDuration) {

        public Duration averageDuration() {
            if (count == 0 || totalDuration == null) return Duration.ZERO;
            return totalDuration.dividedBy(count);
        }
    }

    private static final class Bucket {
        private final AtomicLong count = new AtomicLong();
        private final AtomicLong successCount = new AtomicLong();
        private final AtomicLong totalNanos = new AtomicLong();
        private final AtomicReference<Duration> min = new AtomicReference<>();
        private final AtomicReference<Duration> max = new AtomicReference<>();

        void record(Duration duration, boolean success) {
            count.incrementAndGet();
            if (success) successCount.incrementAndGet();
            totalNanos.addAndGet(duration.toNanos());
            updateMin(duration);
            updateMax(duration);
        }

        private void updateMin(Duration candidate) {
            min.updateAndGet(current ->
                    current == null || candidate.compareTo(current) < 0 ? candidate : current);
        }

        private void updateMax(Duration candidate) {
            max.updateAndGet(current ->
                    current == null || candidate.compareTo(current) > 0 ? candidate : current);
        }

        SourceStats snapshot(String key) {
            long c = count.get();
            long s = successCount.get();
            return new SourceStats(
                    key,
                    c,
                    s,
                    c - s,
                    Duration.ofNanos(totalNanos.get()),
                    min.get(),
                    max.get());
        }
    }
}
