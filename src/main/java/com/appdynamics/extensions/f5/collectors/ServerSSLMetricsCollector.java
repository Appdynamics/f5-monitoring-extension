package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.SERVERS;
import static com.appdynamics.extensions.f5.F5Constants.SSL;
import static com.appdynamics.extensions.f5.util.F5Util.changePathSeparator;
import static com.appdynamics.extensions.f5.util.F5Util.convertValue;
import static com.appdynamics.extensions.f5.util.F5Util.createPattern;
import static com.appdynamics.extensions.f5.util.F5Util.filterIncludes;
import static com.appdynamics.extensions.f5.util.F5Util.isMetricToMonitor;

import com.appdynamics.extensions.f5.F5Monitor;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;
import iControl.CommonStatistic;
import iControl.Interfaces;
import iControl.LocalLBProfileServerSSLProfileServerSSLStatisticEntry;
import iControl.LocalLBProfileServerSSLProfileServerSSLStatistics;
import iControl.ManagementPartitionAuthZPartition;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Florencio Sarmiento
 */
public class ServerSSLMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(ServerSSLMetricsCollector.class);

    private Interfaces iControlInterfaces;
    private String f5DisplayName;
    private Set<String> serverSSLIncludes;
    private Set<String> metricExcludes;

    public ServerSSLMetricsCollector(Interfaces iControlInterfaces,
                                     F5 f5, MetricsFilter metricsFilter, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.iControlInterfaces = iControlInterfaces;
        this.f5DisplayName = f5.getDisplayName();
        this.serverSSLIncludes = f5.getServerSSLProfileIncludes();
        this.metricExcludes = metricsFilter.getServerSSLProfileMetricExcludes();
    }

    /*
     * (non-Javadoc)
     * Compatible with F5 v9.0
     * @see https://devcentral.f5.com/wiki/iControl.LocalLB__ProfileServerSSL__get_list.ashx
     *
     */
    public Void call() {
        LOGGER.info("Profile Server SSL metrics collector started...");

        if (serverSSLIncludes == null || serverSSLIncludes.isEmpty()) {
            LOGGER.info("No server ssl names were included for monitoring.");
            return null;
        }

        try {
            ManagementPartitionAuthZPartition[] partition_list = iControlInterfaces.getManagementPartition().get_partition_list();

            List<String> serverSSLList = new ArrayList<String>();
            for (ManagementPartitionAuthZPartition partition : partition_list) {
                iControlInterfaces.getManagementPartition().set_active_partition(partition.getPartition_name());
                String[] serverSSLs = iControlInterfaces.getLocalLBProfileServerSSL().get_list();
                serverSSLList.addAll(Arrays.asList(serverSSLs));
            }

            String[] serverSSLs = serverSSLList.toArray(new String[]{});

            if (ArrayUtils.isNotEmpty(serverSSLs)) {
                Pattern serverSSLIncludesPattern = createPattern(serverSSLIncludes);
                serverSSLs = filterIncludes(serverSSLs, serverSSLIncludesPattern);

                if (ArrayUtils.isNotEmpty(serverSSLs)) {
                    collectServerSSLMetrics(serverSSLs);

                } else {
                    LOGGER.info("No Profile Server SSL matched.");
                }
            } else {
                LOGGER.info("No Profile Server SSL found.");
            }


        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while fetching server ssl profile list", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching server ssl profile list", e);
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * Compatible with F5 v9.0
     * @see https://devcentral.f5.com/wiki/iControl.LocalLB__ProfileServerSSL__get_statistics.ashx
     *
     */
    private void collectServerSSLMetrics(String[] serverSSLs) {
        try {
            LocalLBProfileServerSSLProfileServerSSLStatistics sslStats =
                    iControlInterfaces.getLocalLBProfileServerSSL().get_statistics(serverSSLs);

            if (sslStats != null) {
                String serverSSLMetricPrefix = getServerSSLMetricPrefix();
                Pattern excludePatterns = createPattern(metricExcludes);

                for (LocalLBProfileServerSSLProfileServerSSLStatisticEntry entry : sslStats.getStatistics()) {
                    String profileName = changePathSeparator(entry.getProfile_name(), PATH_SEPARATOR,
                            METRIC_PATH_SEPARATOR, true);

                    for (CommonStatistic stat : entry.getStatistics()) {
                        if (isMetricToMonitor(stat.getType().getValue(), excludePatterns)) {
                            String metricName = String.format("%s%s%s%s", serverSSLMetricPrefix,
                                    profileName, METRIC_PATH_SEPARATOR, stat.getType().getValue());

                            BigInteger value = convertValue(stat.getValue());
                            printCollectiveObservedCurrent(metricName, value);
                        }
                    }
                }
            }

        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while server ssl statistics", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching server ssl statistics", e);
        }
    }

    private String getServerSSLMetricPrefix() {
        return String.format("%s%s%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR, SSL,
                METRIC_PATH_SEPARATOR, SERVERS);
    }

}
