package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.HTTP;
import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.NETWORK;
import static com.appdynamics.extensions.f5.util.F5Util.convertValue;
import static com.appdynamics.extensions.f5.util.F5Util.createPattern;
import static com.appdynamics.extensions.f5.util.F5Util.isMetricToMonitor;

import com.appdynamics.extensions.f5.F5Monitor;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;
import iControl.CommonStatistic;
import iControl.Interfaces;
import iControl.SystemStatisticsBindingStub;
import org.apache.log4j.Logger;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Florencio Sarmiento
 */
public class HttpMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(HttpMetricsCollector.class);

    private Interfaces iControlInterfaces;
    private String f5DisplayName;
    private Set<String> metricExcludes;

    public HttpMetricsCollector(Interfaces iControlInterfaces,
                                F5 f5, MetricsFilter metricsFilter, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.iControlInterfaces = iControlInterfaces;
        this.f5DisplayName = f5.getDisplayName();
        this.metricExcludes = metricsFilter.getHttpMetricExcludes();
    }

    /*
     * (non-Javadoc)
     * Compatible with F5 v9.0
     * @see https://devcentral.f5.com/wiki/iControl.System__Statistics__get_http_statistics.ashx
     *
     */
    public Void call() {
        LOGGER.info("HTTP metrics collector started...");

        try {
            SystemStatisticsBindingStub systemStats = iControlInterfaces.getSystemStatistics();

            if (systemStats != null) {
                String httpMetricPrefix = getHttpMetricPrefix();
                Pattern excludePatterns = createPattern(metricExcludes);

                for (CommonStatistic stat : systemStats.get_http_statistics().getStatistics()) {
                    if (isMetricToMonitor(stat.getType().getValue(), excludePatterns)) {
                        String metricName = String.format("%s%s", httpMetricPrefix, stat.getType().getValue());
                        BigInteger value = convertValue(stat.getValue());
                        printCollectiveObservedCurrent(metricName, value);
                    }
                }
            }

        } catch (RemoteException e) {
            LOGGER.error("An issue occurred while fetching http statistics", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching http statistics", e);
        }

        return null;
    }

    private String getHttpMetricPrefix() {
        return String.format("%s%s%s%s%s%s", f5DisplayName,
                METRIC_PATH_SEPARATOR, NETWORK, METRIC_PATH_SEPARATOR,
                HTTP, METRIC_PATH_SEPARATOR);
    }

}
