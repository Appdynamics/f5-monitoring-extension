package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.DASH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.HOST;
import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.util.F5Util.convertValue;

import com.appdynamics.extensions.f5.F5Monitor;
import com.appdynamics.extensions.f5.config.F5;
import iControl.CommonStatistic;
import iControl.Interfaces;
import iControl.SystemStatisticsBindingStub;
import iControl.SystemStatisticsHostStatisticEntry;
import iControl.SystemStatisticsHostStatistics;
import org.apache.log4j.Logger;

import java.math.BigInteger;
import java.rmi.RemoteException;

/**
 * @author Florencio Sarmiento
 */
public class MemoryMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(MemoryMetricsCollector.class);

    private Interfaces iControlInterfaces;
    private String f5DisplayName;

    public MemoryMetricsCollector(Interfaces iControlInterfaces, F5 f5, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.iControlInterfaces = iControlInterfaces;
        this.f5DisplayName = f5.getDisplayName();
    }

    /*
     * (non-Javadoc)
     * Compatible with F5 v9.4
     * @see https://devcentral.f5.com/wiki/iControl.System__Statistics__get_all_host_statistics.ashx
     *
     */
    public Void call() {
        LOGGER.info("Memory metrics collector started...");

        try {
            SystemStatisticsBindingStub systemStats = iControlInterfaces.getSystemStatistics();
            SystemStatisticsHostStatistics hostStats = systemStats.get_all_host_statistics();

            if (hostStats != null) {
                String memoryMetricPrefix = getMemoryMetricPrefix();

                for (SystemStatisticsHostStatisticEntry entry : hostStats.getStatistics()) {
                    for (CommonStatistic stat : entry.getStatistics()) {
                        String metricName = stat.getType().getValue();

                        if (metricName.contains("STATISTIC_MEMORY_USED_BYTES") ||
                                metricName.contains("STATISTIC_MEMORY_TOTAL_BYTES")) {
                            metricName = String.format("%s%s%s%s%s%s",
                                    memoryMetricPrefix, HOST, DASH_SEPARATOR, entry.getHost_id(),
                                    METRIC_PATH_SEPARATOR, metricName);
                            BigInteger value = convertValue(stat.getValue());
                            printCollectiveObservedCurrent(metricName, value);
                        }
                    }
                }

            }

        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while fetching memory statistics", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching memory statistics", e);
        }

        return null;
    }

    private String getMemoryMetricPrefix() {
        return String.format("%s%s%s%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR,
                "System", METRIC_PATH_SEPARATOR, "Memory", METRIC_PATH_SEPARATOR);
    }

}
