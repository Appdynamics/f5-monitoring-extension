package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.CLIENTS;
import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.PATH_SEPARATOR;
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
import iControl.LocalLBProfileClientSSLProfileClientSSLStatisticEntry;
import iControl.LocalLBProfileClientSSLProfileClientSSLStatistics;
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
public class ClientSSLMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(ClientSSLMetricsCollector.class);

    private Interfaces iControlInterfaces;
    private String f5DisplayName;
    private Set<String> clientSSLIncludes;
    private Set<String> metricExcludes;

    public ClientSSLMetricsCollector(Interfaces iControlInterfaces, F5 f5, MetricsFilter metricsFilter, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.iControlInterfaces = iControlInterfaces;
        this.f5DisplayName = f5.getDisplayName();
        this.clientSSLIncludes = f5.getClientSSLProfileIncludes();
        this.metricExcludes = metricsFilter.getClientSSLProfileMetricExcludes();
    }

    /*
     * (non-Javadoc)
     * Compatible with F5 v9.0
     * @see https://devcentral.f5.com/wiki/iControl.LocalLB__ProfileClientSSL__get_list.ashx
     *
     */
    public Void call() {
        LOGGER.info("Profile Client SSL metrics collector started...");

        if (clientSSLIncludes == null || clientSSLIncludes.isEmpty()) {
            LOGGER.info("No client ssl names were included for monitoring.");
            return null;
        }

        try {

            ManagementPartitionAuthZPartition[] partition_list = iControlInterfaces.getManagementPartition().get_partition_list();

            List<String> clientSSLList = new ArrayList<String>();
            for (ManagementPartitionAuthZPartition partition : partition_list) {
                iControlInterfaces.getManagementPartition().set_active_partition(partition.getPartition_name());
                String[] clientSSLs = iControlInterfaces.getLocalLBProfileClientSSL().get_list();
                clientSSLList.addAll(Arrays.asList(clientSSLs));
            }

            String[] clientSSLs = clientSSLList.toArray(new String[]{});


            if (ArrayUtils.isNotEmpty(clientSSLs)) {
                Pattern clientSSLIncludesPattern = createPattern(clientSSLIncludes);
                clientSSLs = filterIncludes(clientSSLs, clientSSLIncludesPattern);

                if (ArrayUtils.isNotEmpty(clientSSLs)) {
                    collectClientSSLMetrics(clientSSLs);

                } else {
                    LOGGER.info("No Profile Client SSL matched.");
                }

            } else {
                LOGGER.info("No Profile Client SSL found.");
            }

        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while fetching client ssl profile list", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching client ssl profile list", e);
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * Compatible with F5 v9.0
     * @see https://devcentral.f5.com/wiki/iControl.LocalLB__ProfileClientSSL__get_statistics.ashx
     *
     */
    private void collectClientSSLMetrics(String[] clientSSLs) {
        try {
            LocalLBProfileClientSSLProfileClientSSLStatistics sslStats =
                    iControlInterfaces.getLocalLBProfileClientSSL().get_statistics(clientSSLs);

            if (sslStats != null) {
                String clientSSLMetricPrefix = getClientSSLMetricPrefix();
                Pattern excludePatterns = createPattern(metricExcludes);

                for (LocalLBProfileClientSSLProfileClientSSLStatisticEntry entry : sslStats.getStatistics()) {
                    String profileName = changePathSeparator(entry.getProfile_name(),
                            PATH_SEPARATOR, METRIC_PATH_SEPARATOR, true);

                    for (CommonStatistic stat : entry.getStatistics()) {
                        if (isMetricToMonitor(stat.getType().getValue(), excludePatterns)) {
                            String metricName = String.format("%s%s%s%s", clientSSLMetricPrefix,
                                    profileName, METRIC_PATH_SEPARATOR, stat.getType().getValue());

                            BigInteger value = convertValue(stat.getValue());
                            printCollectiveObservedCurrent(metricName, value);
                        }
                    }
                }
            }

        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while client ssl statistics", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching client ssl statistics", e);
        }
    }

    private String getClientSSLMetricPrefix() {
        return String.format("%s%s%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR, SSL,
                METRIC_PATH_SEPARATOR, CLIENTS);
    }

}
