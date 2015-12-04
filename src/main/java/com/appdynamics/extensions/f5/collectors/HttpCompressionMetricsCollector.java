package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.COMPRESSION;
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
public class HttpCompressionMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(HttpCompressionMetricsCollector.class);

    private Interfaces iControlInterfaces;
    private String f5DisplayName;
    private Set<String> metricExcludes;
    private boolean preVersion11;

    public HttpCompressionMetricsCollector(Interfaces iControlInterfaces,
                                           F5 f5, MetricsFilter metricsFilter, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.iControlInterfaces = iControlInterfaces;
        this.f5DisplayName = f5.getDisplayName();
        this.metricExcludes = metricsFilter.getHttpCompressionMetricExcludes();
        this.preVersion11 = f5.isPreVersion11();
    }

    /*
     * (non-Javadoc)
     * Compatible with F5 v11.0
     * @see https://devcentral.f5.com/wiki/iControl.System__Statistics__get_httpcompression_statistics.ashx
     */
    public Void call() {
        if (preVersion11) {
            LOGGER.info("HTTP Compression metrics collector not supported in this version.");
            return null;
        }

        LOGGER.info("HTTP Compression metrics collector started...");

        try {
            SystemStatisticsBindingStub systemStats = iControlInterfaces.getSystemStatistics();

            if (systemStats != null) {
                String httpCompressionMetricPrefix = getHttpCompressionMetricPrefix();
                Pattern excludePatterns = createPattern(metricExcludes);

                for (CommonStatistic stat : systemStats.get_httpcompression_statistics().getStatistics()) {
                    if (isMetricToMonitor(stat.getType().getValue(), excludePatterns)) {
                        String metricName = String.format("%s%s", httpCompressionMetricPrefix, stat.getType().getValue());
                        BigInteger value = convertValue(stat.getValue());
                        printCollectiveObservedCurrent(metricName, value);
                    }
                }
            }

        } catch (RemoteException e) {
            LOGGER.error("An issue occurred while fetching http compression statistics", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching http compression statistics", e);
        }

        return null;
    }

    private String getHttpCompressionMetricPrefix() {
        return String.format("%s%s%s%s%s%s%s%s",
                f5DisplayName, METRIC_PATH_SEPARATOR,
                NETWORK, METRIC_PATH_SEPARATOR,
                HTTP, METRIC_PATH_SEPARATOR,
                COMPRESSION, METRIC_PATH_SEPARATOR);
    }

}
