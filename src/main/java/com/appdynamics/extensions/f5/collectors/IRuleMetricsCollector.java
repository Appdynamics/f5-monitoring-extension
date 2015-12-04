package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.IRULES;
import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.PATH_SEPARATOR;
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
import iControl.LocalLBRuleRuleStatisticEntry;
import iControl.LocalLBRuleRuleStatistics;
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

public class IRuleMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(IRuleMetricsCollector.class);

    private Interfaces iControlInterfaces;
    private String f5DisplayName;
    private Set<String> iRuleIncludes;
    private Set<String> metricExcludes;

    public IRuleMetricsCollector(Interfaces iControlInterfaces,
                                 F5 f5, MetricsFilter metricsFilter, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.iControlInterfaces = iControlInterfaces;
        this.f5DisplayName = f5.getDisplayName();
        this.iRuleIncludes = f5.getiRuleIncludes();
        this.metricExcludes = metricsFilter.getiRuleMetricExcludes();
    }

    /*
     * (non-Javadoc)
     * Compatible with F5 v9.0
     * @see https://devcentral.f5.com/wiki/iControl.LocalLB__Rule__get_list.ashx
     *
     */
    public Void call() {
        LOGGER.info("iRule metrics collector started...");

        if (iRuleIncludes == null || iRuleIncludes.isEmpty()) {
            LOGGER.info("No rule names were included for monitoring.");
            return null;
        }

        try {

            ManagementPartitionAuthZPartition[] partition_list = iControlInterfaces.getManagementPartition().get_partition_list();

            List<String> iRulesList = new ArrayList<String>();
            for (ManagementPartitionAuthZPartition partition : partition_list) {
                iControlInterfaces.getManagementPartition().set_active_partition(partition.getPartition_name());
                String[] iRules = iControlInterfaces.getLocalLBRule().get_list();
                iRulesList.addAll(Arrays.asList(iRules));
            }

            String[] iRules = iRulesList.toArray(new String[]{});

            if (ArrayUtils.isNotEmpty(iRules)) {
                Pattern iRuleIncludesPattern = createPattern(iRuleIncludes);
                iRules = filterIncludes(iRules, iRuleIncludesPattern);

                if (ArrayUtils.isNotEmpty(iRules)) {
                    collectIRuleMetrics(iRules);

                } else {
                    LOGGER.info("No rule name matched.");
                }
            } else {
                LOGGER.info("No rule name found.");
            }

        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while fetching rule name list", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching rule name list", e);
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * Compatible with F5 v9.0
     * @see https://devcentral.f5.com/wiki/iControl.LocalLB__Rule__get_statistics.ashx
     *
     */
    private void collectIRuleMetrics(String[] iRules) {
        try {
            LocalLBRuleRuleStatistics ruleStats = iControlInterfaces.getLocalLBRule().get_statistics(iRules);

            if (ruleStats != null) {
                String iRuleMetricPrefix = getIRuleMetricPrefix();
                Pattern excludePatterns = createPattern(metricExcludes);

                for (LocalLBRuleRuleStatisticEntry entry : ruleStats.getStatistics()) {
                    String ruleName = changePathSeparator(entry.getRule_name(), PATH_SEPARATOR,
                            METRIC_PATH_SEPARATOR, true);

                    for (CommonStatistic stat : entry.getStatistics()) {
                        if (isMetricToMonitor(stat.getType().getValue(), excludePatterns)) {
                            String metricName = String.format("%s%s%s%s%s%s", iRuleMetricPrefix,
                                    ruleName, METRIC_PATH_SEPARATOR, entry.getEvent_name(),
                                    METRIC_PATH_SEPARATOR, stat.getType().getValue());

                            BigInteger value = convertValue(stat.getValue());
                            printCollectiveObservedCurrent(metricName, value);
                        }
                    }
                }
            }

        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while fetching rule statistics", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching virtual rule statistics", e);
        }
    }

    private String getIRuleMetricPrefix() {
        return String.format("%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR, IRULES);
    }

}
