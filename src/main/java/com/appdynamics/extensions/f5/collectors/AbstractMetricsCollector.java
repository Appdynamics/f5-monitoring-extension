package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.util.F5Util.convertValueToZeroIfNullOrNegative;

import com.appdynamics.extensions.f5.F5Monitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.apache.log4j.Logger;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author Satish Muddam
 */
public abstract class AbstractMetricsCollector implements Callable<Void> {

    private static final Logger LOGGER = Logger.getLogger(AbstractMetricsCollector.class);

    private final F5Monitor monitor;
    private final String metricPrefix;

    public AbstractMetricsCollector(F5Monitor monitor, String metricPrefix) {
        this.monitor = monitor;
        this.metricPrefix = metricPrefix;
    }

    public F5Monitor getMonitor() {
        return monitor;
    }

    public String getMetricPrefix() {
        return metricPrefix;
    }

    public void printMetrics(Map<String, BigInteger> metrics) {

        for(Map.Entry<String, BigInteger> metricEntry : metrics.entrySet()) {
            printCollectiveObservedCurrent(metricEntry.getKey(), metricEntry.getValue());
        }

    }

    public void printCollectiveObservedCurrent(String metricName, BigInteger metricValue) {
        printMetric(metricPrefix + metricName, metricValue,
                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
        );
    }

    private void printMetric(String metricName, BigInteger metricValue, String aggregation,
                             String timeRollup, String cluster) {

        MetricWriter metricWriter = monitor.getMetricWriter(metricName, aggregation,
                timeRollup, cluster);

        BigInteger valueToReport = convertValueToZeroIfNullOrNegative(metricValue);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Sending [%s/%s/%s] metric = %s = %s => %s",
                    aggregation, timeRollup, cluster,
                    metricName, metricValue, valueToReport));
        }

        metricWriter.printMetric(valueToReport.toString());
    }
}
