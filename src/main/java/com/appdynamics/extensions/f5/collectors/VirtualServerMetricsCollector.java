package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.VIRTUAL_SERVERS;
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
import iControl.LocalLBVirtualServerVirtualServerStatisticEntry;
import iControl.LocalLBVirtualServerVirtualServerStatistics;
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
public class VirtualServerMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(VirtualServerMetricsCollector.class);

    private Interfaces iControlInterfaces;
    private String f5DisplayName;
    private Set<String> virtualServerIncludes;
    private Set<String> metricExcludes;

    public VirtualServerMetricsCollector(Interfaces iControlInterfaces,
                                         F5 f5, MetricsFilter metricsFilter, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.iControlInterfaces = iControlInterfaces;
        this.f5DisplayName = f5.getDisplayName();
        this.virtualServerIncludes = f5.getVirtualServerIncludes();
        this.metricExcludes = metricsFilter.getVirtualServerMetricExcludes();
    }

    /*
     * (non-Javadoc)
     * Compatible with F5 v9.0
     * @see https://devcentral.f5.com/wiki/iControl.LocalLB__VirtualServer__get_list.ashx
     *
     */
    public Void call() {
        LOGGER.info("Virtual Server metrics collector started...");

        if (virtualServerIncludes == null || virtualServerIncludes.isEmpty()) {
            LOGGER.info("No virtual servers were included for monitoring.");
            return null;
        }

        try {
            ManagementPartitionAuthZPartition[] partition_list = iControlInterfaces.getManagementPartition().get_partition_list();

            List<String> serverList = new ArrayList<String>();
            for (ManagementPartitionAuthZPartition partition : partition_list) {
                iControlInterfaces.getManagementPartition().set_active_partition(partition.getPartition_name());
                String[] list = iControlInterfaces.getLocalLBVirtualServer().get_list();
                serverList.addAll(Arrays.asList(list));
            }

            String[] virtualServers = serverList.toArray(new String[]{});

            if (ArrayUtils.isNotEmpty(virtualServers)) {
                Pattern virtualServerIncludesPattern = createPattern(virtualServerIncludes);
                virtualServers = filterIncludes(virtualServers, virtualServerIncludesPattern);

                if (ArrayUtils.isNotEmpty(virtualServers)) {
                    collectVirtualServerMetrics(virtualServers);

                } else {
                    LOGGER.info("No Virtual Server matched.");
                }
            } else {
                LOGGER.info("No Virtual Server found.");
            }

        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while fetching virtual server list", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching virtual server list", e);
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * Compatible with F5 v9.0
     * @see https://devcentral.f5.com/wiki/iControl.LocalLB__VirtualServer__get_statistics.ashx
     *
     */
    private void collectVirtualServerMetrics(String[] virtualServers) {
        try {
            LocalLBVirtualServerVirtualServerStatistics vsStats = iControlInterfaces.getLocalLBVirtualServer()
                    .get_statistics(virtualServers);

            if (vsStats != null) {
                String virtualServerMetricPrefix = getVirtualServerMetricPrefix();
                Pattern excludePatterns = createPattern(metricExcludes);

                for (LocalLBVirtualServerVirtualServerStatisticEntry vsStat : vsStats.getStatistics()) {
                    String vsName = changePathSeparator(vsStat.getVirtual_server().getName(), PATH_SEPARATOR,
                            METRIC_PATH_SEPARATOR, true);

                    for (CommonStatistic stat : vsStat.getStatistics()) {
                        if (isMetricToMonitor(stat.getType().getValue(), excludePatterns)) {
                            String metricName = String.format("%s%s%s%s", virtualServerMetricPrefix,
                                    vsName, METRIC_PATH_SEPARATOR, stat.getType().getValue());

                            BigInteger value = convertValue(stat.getValue());
                            printCollectiveObservedCurrent(metricName, value);
                        }
                    }
                }
            }

        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while fetching virtual server statistics", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching virtual server statistics", e);
        }
    }

    private String getVirtualServerMetricPrefix() {
        return String.format("%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR, VIRTUAL_SERVERS);
    }

}
