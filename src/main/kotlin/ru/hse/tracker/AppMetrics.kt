package ru.hse.tracker

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

@Component
class AppMetrics(
    registry: MeterRegistry,
    tasks: TaskRepository,
) {
    private val created = Counter.builder("tasks.created")
        .description("Successfully created tasks")
        .register(registry)

    private val saveDuration = Timer.builder("task.save.duration")
        .description("Time to create or update a task")
        .publishPercentileHistogram()
        .register(registry)

    private val completionDuration = Timer.builder("task.completion.duration")
        .description("Time from task creation until it is marked done")
        .publishPercentileHistogram()
        .register(registry)

    private val openTasks = AtomicLong(tasks.countOpen())

    init {
        Gauge.builder("tasks.open", openTasks) { it.get().toDouble() }
            .description("Tasks that are not done")
            .register(registry)
    }

    fun taskCreated() = created.increment()

    fun taskOpened() {
        openTasks.incrementAndGet()
    }

    fun taskClosed() {
        openTasks.decrementAndGet()
    }

    fun <T> recordSave(block: () -> T): T = saveDuration.recordCallable(block)!!

    fun recordCompletion(createdAt: Instant) {
        val duration = Duration.between(createdAt, Instant.now())
        if (!duration.isNegative) completionDuration.record(duration)
    }
}
