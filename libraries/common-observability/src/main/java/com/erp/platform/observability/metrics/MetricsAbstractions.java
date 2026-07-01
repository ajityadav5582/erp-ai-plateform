package com.erp.platform.observability.metrics;

/**
 * Metrics abstractions for the ERP AI Platform.
 *
 * <p>This interface provides abstractions for metrics collection.
 * Implementations will be provided by platform services.
 *
 * @since 1.0.0
 */
public interface MetricsAbstractions {

    /**
     * Increments a counter.
     *
     * @param name the counter name
     */
    void incrementCounter(String name);

    /**
     * Increments a counter with tags.
     *
     * @param name the counter name
     * @param tags the tags
     */
    void incrementCounter(String name, Tags tags);

    /**
     * Records a gauge value.
     *
     * @param name the gauge name
     * @param value the value
     */
    void recordGauge(String name, double value);

    /**
     * Records a timer value.
     *
     * @param name the timer name
     * @param durationMs the duration in milliseconds
     */
    void recordTimer(String name, long durationMs);

    /**
     * Tags for metrics.
     */
    record Tags(String key, String value) {
    }
}
