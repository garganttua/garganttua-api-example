package com.garganttua.api.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.garganttua.core.observability.EndEvent;
import com.garganttua.core.observability.ErrorEvent;
import com.garganttua.core.observability.LogEvent;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.observability.StartEvent;
import com.garganttua.core.observability.annotations.Observer;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Logs every {@link ObservableEvent} the example attaches to. With
 * api-side {@code IApiObserver} now folded into core's
 * {@link ObservableEvent} stream, a single observer receives both the
 * {@code api:operation:*} boundary events and every nested engine event
 * (stage:*, script:*, mapper:*, runtime:*, scriptcontext:*, …) sharing
 * the same {@code executionId} via {@code ObservableContextHolder}.
 *
 * <p>Wired manually per-domain in {@code ExampleApplication.main} —
 * see {@link CoreStatsObserver} for the same caveat about the
 * {@code @Observer} auto-discovery path.
 */
@Observer
@Reflected(queryAllDeclaredConstructors = true)
public final class CoreLoggingObserver implements IObserver<ObservableEvent> {

    private static final Logger log = LoggerFactory.getLogger(CoreLoggingObserver.class);

    @Override
    public void onEvent(ObservableEvent event) {
        switch (event) {
            case StartEvent s -> {
                if (log.isTraceEnabled()) {
                    log.trace("core-start exec={} src={}", s.executionId(), s.source());
                }
            }
            case EndEvent e -> {
                long micros = e.duration() == null ? -1L : e.duration().toNanos() / 1_000L;
                log.debug("core-end   exec={} src={} took={}µs code={}",
                        e.executionId(), e.source(), micros, e.code());
            }
            case ErrorEvent e -> {
                long micros = e.duration() == null ? -1L : e.duration().toNanos() / 1_000L;
                log.warn("core-error exec={} src={} took={}µs failure={}: {}",
                        e.executionId(), e.source(), micros,
                        e.failure() == null ? "<null>" : e.failure().getClass().getSimpleName(),
                        e.failure() == null ? "" : e.failure().getMessage());
            }
            case LogEvent l -> {
                String msg = "core-log   exec={} src={} {}";
                switch (l.level()) {
                    case TRACE -> log.trace(msg, l.executionId(), l.source(), l.message());
                    case DEBUG -> log.debug(msg, l.executionId(), l.source(), l.message());
                    case INFO  -> log.info(msg, l.executionId(), l.source(), l.message());
                    case WARN  -> log.warn(msg, l.executionId(), l.source(), l.message());
                    case ERROR -> log.error(msg, l.executionId(), l.source(), l.message());
                }
            }
        }
    }
}
