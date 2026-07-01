package com.erp.platform.observability.metrics;

/**
 * Metrics registry abstraction.
 *
 * <p>Provides a facade for recording application metrics.
 * This is an abstraction that can be implemented by different
 * metrics backends (Micrometer, Prometheus, etc.).
 *
 * <p>Note: Actual Prometheus/OpenTelemetry implementation is deferred
 * to platform services. This provides the abstraction only.
 *
 * @since 1.0.0
 */
public interface MetricsRegistry {

    /**
     * Increments a counter.
     *
     * @param name the counter name
     * @param tags the tags
     */
    void incrementCounter(String name, String... tags);

    /**
     * Records a gauge value.
     *
     * @param name the gauge name
     * @param value the value
     * @param tags the tags
     */
    void recordGauge(String name, double value, String... tags);

    /**
     * Records a timer value.
     *
     * @param name the timer name
     * @param durationMs the duration in milliseconds
     * @param tags the tags
     */
    void recordTimer(String name, long durationMs, String... tags);

    /**
     * Creates a no-op metrics registry.
     *
     * @return a no-op registry
     */
    static MetricsRegistry noop() {
        return new NoopMetricsRegistry();
    }

    /**
     * No-op implementation of MetricsRegistry.
     */
    class NoopMetricsRegistry implements MetricsRegistry {
        @Override
        public void incrementCounter(String name, String... tags) {
            // No-op
        }

        @Override
        public void recordGauge(String name, double value, String... tags) {
            // No-op
        }

        @Override
        public void recordTimer(String name, long durationMs, String... tags) {
            // No-op
        }
    }
}
