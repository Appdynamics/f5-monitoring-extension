package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.SNAT_POOLS;
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
import iControl.LocalLBSNATPoolSNATPoolStatisticEntry;
import iControl.LocalLBSNATPoolSNATPoolStatistics;
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
public class SnatPoolMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(SnatPoolMetricsCollector.class);

    private Interfaces iControlInterfaces;
    private String f5DisplayName;
    private Set<String> snatPoolIncludes;
    private Set<String> snatPoolMemberIncludes;
    private Set<String> metricExcludes;
    private boolean preVersion11;

    public SnatPoolMetricsCollector(Interfaces iControlInterfaces,
                                    F5 f5, MetricsFilter metricsFilter, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.iControlInterfaces = iControlInterfaces;
        this.f5DisplayName = f5.getDisplayName();
        this.snatPoolIncludes = f5.getSnatPoolIncludes();
        this.snatPoolMemberIncludes = f5.getSnatPoolMemberIncludes();
        this.metricExcludes = metricsFilter.getSnatPoolMetricExcludes();
        this.preVersion11 = f5.isPreVersion11();
    }

    /*
     * (non-Javadoc)
     * Compatible with F5 v9.0
     * @see https://devcentral.f5.com/wiki/iControl.LocalLB__SNATPool__get_list.ashx
     *
     */
    public Void call() {
        LOGGER.info("SNAT Pool metrics collector started...");

        if (snatPoolIncludes == null || snatPoolIncludes.isEmpty()) {
            LOGGER.info("No SNAT pools were included for monitoring.");
            return null;
        }

        try {
            ManagementPartitionAuthZPartition[] partition_list = iControlInterfaces.getManagementPartition().get_partition_list();

            List<String> poolsList = new ArrayList<String>();
            for (ManagementPartitionAuthZPartition partition : partition_list) {
                iControlInterfaces.getManagementPartition().set_active_partition(partition.getPartition_name());
                String[] pools = iControlInterfaces.getLocalLBProfileServerSSL().get_list();
                poolsList.addAll(Arrays.asList(pools));
            }
            String[] pools = poolsList.toArray(new String[]{});

            if (ArrayUtils.isNotEmpty(pools)) {
                Pattern poolIncludesPattern = createPattern(snatPoolIncludes);
                pools = filterIncludes(pools, poolIncludesPattern);

                if (ArrayUtils.isNotEmpty(pools)) {
                    collectSnatPoolMetrics(pools);
                    collectMemberMetrics(pools);

                } else {
                    LOGGER.info("No SNAT Pool matched.");
                }
            } else {
                LOGGER.info("No SNAT Pool found.");
            }

        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while fetching snat pool list", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching snat pool list", e);
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * Compatible with F5 v9.0
     * @see https://devcentral.f5.com/wiki/iControl.LocalLB__SNATPool__get_statistics.ashx
     *
     */
    private void collectSnatPoolMetrics(String[] pools) {
        try {
            LocalLBSNATPoolSNATPoolStatistics stats = iControlInterfaces.getLocalLBSNATPool().get_statistics(pools);

            if (stats != null) {
                String poolMetricPrefix = getPoolMetricPrefix();
                Pattern excludePatterns = createPattern(metricExcludes);

                for (LocalLBSNATPoolSNATPoolStatisticEntry pool : stats.getStatistics()) {
                    String poolName = changePathSeparator(pool.getSnat_pool(),
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
            }

        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while fetching snat pool statistics", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching snat pool statistics", e);
        }
    }

    /*
     * (non-Javadoc)
     * Compatible with F5 v11.0
     * @see https://devcentral.f5.com/wiki/iControl.LocalLB__SNATPool__get_all_member_statistics.ashx
     *
     */
    private void collectMemberMetrics(String[] pools) {
        if (snatPoolMemberIncludes == null || snatPoolMemberIncludes.isEmpty()) {
            LOGGER.info("No SNAT pool members were included for monitoring.");
            return;
        }

        SnatPoolMemberMetricsCollector memberMetricsCollector;

        if (preVersion11) {
            memberMetricsCollector = new PreVersion11SnatPoolMemberMetricsCollector(
                    snatPoolMemberIncludes, metricExcludes, iControlInterfaces);
        } else {
            memberMetricsCollector = new SnatPoolMemberMetricsCollector(
                    snatPoolMemberIncludes, metricExcludes, iControlInterfaces);
        }

        memberMetricsCollector.collectMemberMetrics(getPoolMetricPrefix(), pools);
    }

    private String getPoolMetricPrefix() {
        return String.format("%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR, SNAT_POOLS);
    }

}
