package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.POOLS;
import static com.appdynamics.extensions.f5.F5Constants.STATUS;
import static com.appdynamics.extensions.f5.util.F5Util.changePathSeparator;
import static com.appdynamics.extensions.f5.util.F5Util.convertToStatus;
import static com.appdynamics.extensions.f5.util.F5Util.convertValue;
import static com.appdynamics.extensions.f5.util.F5Util.createPattern;
import static com.appdynamics.extensions.f5.util.F5Util.filterIncludes;
import static com.appdynamics.extensions.f5.util.F5Util.isMetricToMonitor;

import com.appdynamics.extensions.f5.F5Monitor;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;
import iControl.CommonStatistic;
import iControl.Interfaces;
import iControl.LocalLBObjectStatus;
import iControl.LocalLBPoolPoolStatisticEntry;
import iControl.LocalLBPoolPoolStatistics;
import iControl.ManagementPartitionAuthZPartition;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Florencio Sarmiento
 */
public class PoolMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(PoolMetricsCollector.class);

    private Interfaces iControlInterfaces;
    private String f5DisplayName;
    private Set<String> poolIncludes;
    private Set<String> poolMemberIncludes;
    private Set<String> metricExcludes;
    private boolean preVersion11;

    public PoolMetricsCollector(Interfaces iControlInterfaces, F5 f5, MetricsFilter metricsFilter, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.iControlInterfaces = iControlInterfaces;
        this.f5DisplayName = f5.getDisplayName();
        this.poolIncludes = f5.getPoolIncludes();
        this.poolMemberIncludes = f5.getPoolMemberIncludes();
        this.metricExcludes = metricsFilter.getPoolMetricExcludes();
        this.preVersion11 = f5.isPreVersion11();
    }

    /*
     * (non-Javadoc)
     * Compatible with F5 v9.0
     * @see https://devcentral.f5.com/wiki/iControl.LocalLB__Pool__get_list.ashx
     *
     */
    public Void call() {
        LOGGER.info("Pool metrics collector started...");

        if (poolIncludes == null || poolIncludes.isEmpty()) {
            LOGGER.info("No pools were included for monitoring.");
            return null;
        }

        try {
            ManagementPartitionAuthZPartition[] partition_list = iControlInterfaces.getManagementPartition().get_partition_list();

            List<String> poolList = new ArrayList<String>();
            for (ManagementPartitionAuthZPartition partition : partition_list) {
                iControlInterfaces.getManagementPartition().set_active_partition(partition.getPartition_name());
                String[] pools = iControlInterfaces.getLocalLBPool().get_list();
                poolList.addAll(Arrays.asList(pools));
            }

            String[] pools = poolList.toArray(new String[]{});

            if (ArrayUtils.isNotEmpty(pools)) {
                Pattern poolIncludesPattern = createPattern(poolIncludes);
                pools = filterIncludes(pools, poolIncludesPattern);

                if (ArrayUtils.isNotEmpty(pools)) {
                    collectPoolMetrics(pools);
                    collectMemberMetrics(pools);

                } else {
                    LOGGER.info("No Pool matched.");
                }
            } else {
                LOGGER.info("No Pool found.");
            }

        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while fetching pool list", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching pool list", e);
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * Compatible with F5 v9.0
     * @see https://devcentral.f5.com/wiki/iControl.LocalLB__Pool__get_statistics.ashx
     *
     */
    private void collectPoolMetrics(String[] pools) {
        try {
            LocalLBPoolPoolStatistics stats = iControlInterfaces.getLocalLBPool().get_statistics(pools);

            if (stats != null) {
                String poolMetricPrefix = getPoolMetricPrefix();
                Pattern excludePatterns = createPattern(metricExcludes);

                for (LocalLBPoolPoolStatisticEntry pool : stats.getStatistics()) {
                    String poolName = changePathSeparator(pool.getPool_name(),
                            PATH_SEPARATOR, METRIC_PATH_SEPARATOR, true);

                    for (CommonStatistic stat : pool.getStatistics()) {
                        if (isMetricToMonitor(stat.getType().getValue(), excludePatterns)) {
                            String metricName = String.format("%s%s%s%s", poolMetricPrefix,
                                    poolName, METRIC_PATH_SEPARATOR, stat.getType().getValue());

                            BigInteger value = convertValue(stat.getValue());
                            printCollectiveObservedCurrent(metricName, value);
                        }
                    }
                }

                if (isMetricToMonitor(STATUS, excludePatterns)) {
                    collectPoolStatus(pools);
                }
            }

        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while fetching pool statistics", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching pool statistics", e);
        }
    }

    /*
     * (non-Javadoc)
     * Compatible with F5 v9.0
     * @see https://devcentral.f5.com/wiki/iControl.LocalLB__Pool__get_object_status.ashx
     *
     */
    private void collectPoolStatus(String[] pools) {
        try {
            LocalLBObjectStatus[] statuses = iControlInterfaces.getLocalLBPool().get_object_status(pools);

            if (ArrayUtils.isNotEmpty(statuses)) {
                String poolMetricPrefix = getPoolMetricPrefix();
                int index = 0;

                for (LocalLBObjectStatus status : statuses) {
                    String poolName = changePathSeparator(pools[index++],
                            PATH_SEPARATOR, METRIC_PATH_SEPARATOR, true);

                    String metricName = String.format("%s%s%s%s", poolMetricPrefix,
                            poolName, METRIC_PATH_SEPARATOR, STATUS);

                    BigInteger value = BigInteger.valueOf(convertToStatus(status.getAvailability_status(),
                            status.getEnabled_status()).getValue());
                    printCollectiveObservedCurrent(metricName, value);
                }
            }

        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while fetching pool status", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching pool status", e);
        }
    }

    private void collectMemberMetrics(String[] pools) {
        if (poolMemberIncludes == null || poolMemberIncludes.isEmpty()) {
            LOGGER.info("No pool members were included for monitoring.");
//			return;
        }

        PoolMemberMetricsCollector memberMetricsCollector;

        if (preVersion11) {
            memberMetricsCollector = new PreVersion11PoolMemberMetricsCollector(
                    poolMemberIncludes, metricExcludes, iControlInterfaces);
        } else {
            memberMetricsCollector = new PoolMemberMetricsCollector(
                    poolMemberIncludes, metricExcludes, iControlInterfaces);
        }

        Map<String, BigInteger> poolMemberMetrics = memberMetricsCollector.collectMemberMetrics(getPoolMetricPrefix(), pools);
        printMetrics(poolMemberMetrics);
    }

    private String getPoolMetricPrefix() {
        return String.format("%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR, POOLS);
    }

}
