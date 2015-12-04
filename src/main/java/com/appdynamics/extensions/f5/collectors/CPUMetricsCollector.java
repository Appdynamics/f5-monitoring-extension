package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.CPU;
import static com.appdynamics.extensions.f5.F5Constants.DASH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.HOST;
import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.SYSTEM;
import static com.appdynamics.extensions.f5.util.F5Util.convertValue;

import com.appdynamics.extensions.f5.F5Monitor;
import com.appdynamics.extensions.f5.config.F5;
import iControl.CommonStatistic;
import iControl.Interfaces;
import iControl.SystemCPUUsageExtended;
import iControl.SystemSystemInfoBindingStub;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import java.math.BigInteger;
import java.rmi.RemoteException;

/**
 * @author Florencio Sarmiento
 */
public class CPUMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(CPUMetricsCollector.class);

    private Interfaces iControlInterfaces;
    private String f5DisplayName;

    public CPUMetricsCollector(Interfaces iControlInterfaces, F5 f5, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.iControlInterfaces = iControlInterfaces;
        this.f5DisplayName = f5.getDisplayName();
    }

    /*
     * (non-Javadoc)
     * Compatible with F5 v9.4
     * @see https://devcentral.f5.com/wiki/iControl.System__SystemInfo__get_all_cpu_usage_extended_information.ashx
     *
     */
    public Void call() {
        LOGGER.info("CPU metrics collector started...");

        try {
            SystemSystemInfoBindingStub systemSub = iControlInterfaces.getSystemSystemInfo();
            SystemCPUUsageExtended[] hosts = systemSub.get_all_cpu_usage_extended_information().getHosts();

            if (ArrayUtils.isNotEmpty(hosts)) {
                collectCPUMetrics(hosts);

            } else {
                LOGGER.info("No host found to collect cpu metrcis from.");
            }

        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while fetching cpu lists", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching cpu lists", e);
        }

        return null;
    }

    private void collectCPUMetrics(SystemCPUUsageExtended[] hosts) {
        try {
            String cpuMetricPrefix = getCPUMetricPrefix();

            for (SystemCPUUsageExtended host : hosts) {
                BigInteger cpuUsage = BigInteger.ZERO;
                int cpuCount = 0;

                for (CommonStatistic[] stat1 : host.getStatistics()) {
                    for (CommonStatistic stat2 : stat1) {
                        if ("STATISTIC_CPU_INFO_ONE_MIN_AVG_IDLE".equalsIgnoreCase(stat2.getType().getValue())) {
                            cpuCount++;
                            cpuUsage = cpuUsage.add(convertValue(stat2.getValue()));
                        }
                    }
                }

                if (cpuCount > 0) {
                    String metricName = String.format("%s%s%s%s%s%s", cpuMetricPrefix,
                            HOST, DASH_SEPARATOR, host.getHost_id(), METRIC_PATH_SEPARATOR, "CPU % BUSY");

                    cpuUsage = BigInteger.valueOf(100).subtract(cpuUsage.divide(BigInteger.valueOf(cpuCount)));
                    printCollectiveObservedCurrent(metricName, cpuUsage);
                }
            }


        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching cpu statistics", e);
        }
    }

    private String getCPUMetricPrefix() {
        return String.format("%s%s%s%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR,
                SYSTEM, METRIC_PATH_SEPARATOR, CPU, METRIC_PATH_SEPARATOR);
    }

}
