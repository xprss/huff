package dev.huff.hexaquot.game;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Every midnight the agent writes the upcoming cases (today and the lookahead days, by default
 * tomorrow), so a case is ready a full day before players need it. A retry job and a startup
 * check fill any missing or failed date. Generation runs on a background thread.
 */
@ApplicationScoped
public class HexatrapCaseScheduler {
    private static final Logger LOG = Logger.getLogger(HexatrapCaseScheduler.class);

    @Inject HexatrapCaseGenerator generator;
    @Inject HexatrapAgentClient client;
    @Inject DailyGameService clock;

    @ConfigProperty(name = "app.hexatrap.generation.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "app.hexatrap.generation.lookahead-days", defaultValue = "1")
    int lookaheadDays;

    private final Set<String> queued = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(new DaemonThreads());

    @Scheduled(cron = "{app.hexatrap.generation.cron}", timeZone = "{app.game.timezone}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void nightly() {
        ensureUpcoming();
    }

    @Scheduled(cron = "{app.hexatrap.generation.retry-cron}", timeZone = "{app.game.timezone}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void retry() {
        ensureUpcoming();
    }

    void onStart(@Observes StartupEvent event) {
        ensureUpcoming();
    }

    public void ensureUpcoming() {
        if (!enabled) return;
        if (!client.configured()) {
            LOG.debug("Hexatrap generation skipped: the agent token is not configured.");
            return;
        }
        LocalDate today = LocalDate.parse(clock.todayDate());
        for (int offset = 0; offset <= Math.max(0, lookaheadDays); offset++) enqueue(today.plusDays(offset).toString(), false);
    }

    /** Queues the generation of a date; false when it is already queued or the agent is not configured. */
    public boolean enqueue(String date, boolean replaceReady) {
        if (!client.configured() || !queued.add(date)) return false;
        try {
            executor.execute(() -> {
                try {
                    generator.generate(date, replaceReady);
                } catch (RuntimeException error) {
                    LOG.errorf(error, "Hexatrap generation crashed for %s.", date);
                } finally {
                    queued.remove(date);
                }
            });
            return true;
        } catch (RejectedExecutionException error) {
            queued.remove(date);
            return false;
        }
    }

    public boolean queued(String date) {
        return queued.contains(date);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private static final class DaemonThreads implements java.util.concurrent.ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "hexatrap-generation-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
